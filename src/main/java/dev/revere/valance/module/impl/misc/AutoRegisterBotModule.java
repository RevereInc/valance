package dev.revere.valance.module.impl.misc;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.settings.Setting;
import dev.revere.valance.settings.type.NumberSetting;
import dev.revere.valance.util.MinecraftUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@ModuleInfo(name = "LegacyAutoRegisterBot", description = "Simplified bot system for Netty 4.0.x", category = Category.MISC)
public class AutoRegisterBotModule extends AbstractModule {

    private static class BotConnection {
        final String username;
        Channel channel;
        String code;
        int state = 0;
        int messageCount = 0;
        long lastAction = 0;
        boolean loggedIn = false;

        BotConnection(String username) {
            this.username = username;
        }
    }

    private final Setting<Integer> botCount = new NumberSetting<>("Bots", 5, 1, 10000);
    private final Setting<Integer> connectTimeout = new NumberSetting<>("ConnectionTimeout", 5000, 1000, 100000);
    private final Setting<Integer> connectionDelay = new NumberSetting<>("ConnectionDelay", 500, 100, 5000);

    private final Map<String, BotConnection> bots = new ConcurrentHashMap<>();
    private EventLoopGroup workerGroup;
    private final Pattern codePattern = Pattern.compile("§a/register password password §c(\\d+)");

    public AutoRegisterBotModule(IEventBusService eventBusService) {
        super(eventBusService);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        workerGroup = new NioEventLoopGroup();

        for (int i = 0; i < botCount.getValue(); i++) {
            String username = "Valance_" + RandomStringUtils.randomAlphanumeric(8);
            final int delayMs = i * connectionDelay.getValue();
            new Thread(() -> {
                try {
                    Thread.sleep(delayMs);
                    connectBot(username);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        log("Starting " + botCount.getValue() + " bots with " + connectionDelay.getValue() + "ms delay between connections...");
    }

    private void connectBot(String username) {
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout.getValue())
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();

                            p.addLast("encoder", new MinecraftEncoder());
                            p.addLast("decoder", new MinecraftDecoder());
                            p.addLast("handler", new MinecraftBotHandler(AutoRegisterBotModule.this, username));
                        }
                    });

            ChannelFuture f = b.connect("50.114.4.102", 25565).sync();

            if (f.isSuccess()) {
                BotConnection bot = new BotConnection(username);
                bot.channel = f.channel();
                bots.put(username, bot);

                sendHandshake(bot.channel, username);
                sendLoginStart(bot.channel, username);

                log(username + " connected and sent handshake");
            }
        } catch (Exception e) {
            logError(username + " connection failed: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    private void sendHandshake(Channel channel, String username) {
        ByteBuf buf = Unpooled.buffer();
        writeVarInt(buf, 0x00);
        writeVarInt(buf, 47);
        writeString(buf, "50.114.4.102");
        buf.writeShort(25565);
        writeVarInt(buf, 2);
        channel.writeAndFlush(buf);
    }


    private void sendLoginStart(Channel channel, String username) {
        ByteBuf buf = Unpooled.buffer();
        writeVarInt(buf, 0x00);
        writeString(buf, username);
        channel.writeAndFlush(buf);
    }

    private void sendChatMessage(Channel channel, String message) {
        ByteBuf buf = Unpooled.buffer();
        writeVarInt(buf, 0x01);
        writeString(buf, message);
        channel.writeAndFlush(buf);
    }

    private static void writeVarInt(ByteBuf buf, int value) {
        while ((value & 0xFFFFFF80) != 0) {
            buf.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    private void writeString(ByteBuf buf, String string) {
        byte[] bytes = string.getBytes(CharsetUtil.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    @Override
    public void onDisable() {
        super.onDisable();

        bots.values().forEach(bot -> {
            if (bot.channel != null) {
                bot.channel.close().awaitUninterruptibly();
            }
        });
        bots.clear();

        if (workerGroup != null) {
            workerGroup.shutdownGracefully().awaitUninterruptibly();
        }
        log("Stopped all bots");
    }

    public void handleLoginSuccess(String username) {
        BotConnection bot = bots.get(username);
        if (bot != null) {
            bot.loggedIn = true;
            bot.state = 1;
            log(username + " successfully logged in");
        }
    }

    public void handleChatMessage(String username, String message) {
        BotConnection bot = bots.get(username);
        if (bot == null) return;

        Matcher matcher = codePattern.matcher(message);
        if (matcher.find() && bot.state == 1) {
            bot.code = matcher.group(1);
            bot.state = 2;
            log(username + " received code: " + bot.code);
        }
    }

    private String generateRandomMessage() {
        String[] words = {"abc", "hello", "guys", "cavepvp", "lol", "gg", "bot", "test"};
        StringBuilder sb = new StringBuilder();
        int wordCount = 2 + new Random().nextInt(3);

        for (int i = 0; i < wordCount; i++) {
            sb.append(words[new Random().nextInt(words.length)]);
            if (i < wordCount - 1) sb.append(" ");
        }
        return sb.toString();
    }

    private void log(String message) {
        MinecraftUtil.sendClientMessage("[" + ClientLoader.CLIENT_NAME + ":LegacyBot] " + message);
    }

    private void logError(String message) {
        MinecraftUtil.sendClientMessage("[" + ClientLoader.CLIENT_NAME + ":LegacyBot] §c" + message);
    }

    private static class MinecraftEncoder extends MessageToByteEncoder<ByteBuf> {
        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
            writeVarInt(out, msg.readableBytes());
            out.writeBytes(msg);
        }

        private void writeVarInt(ByteBuf buf, int value) {
            while ((value & 0xFFFFFF80) != 0) {
                buf.writeByte(value & 0x7F | 0x80);
                value >>>= 7;
            }
            buf.writeByte(value);
        }
    }

    private static class MinecraftDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            if (in.readableBytes() < 1) return;

            in.markReaderIndex();
            int length = readVarInt(in);

            if (in.readableBytes() < length) {
                in.resetReaderIndex();
                return;
            }

            out.add(in.readBytes(length));
        }

        private int readVarInt(ByteBuf buf) {
            int value = 0;
            int length = 0;
            byte current;

            while (true) {
                current = buf.readByte();
                value |= (current & 0x7F) << (length * 7);

                if ((current & 0x80) != 0x80) break;
                if (length++ >= 5) throw new RuntimeException("VarInt too big");
            }

            return value;
        }
    }

    private static class MinecraftBotHandler extends ChannelInboundHandlerAdapter {
        private final AutoRegisterBotModule module;
        private final String username;

        public MinecraftBotHandler(AutoRegisterBotModule module, String username) {
            this.module = module;
            this.username = username;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf buf = (ByteBuf) msg;
            try {
                int packetId = readVarInt(buf);

                switch (packetId) {
                    case 0x02:
                        module.handleLoginSuccess(username);
                        break;
                    case 0x01:
                        ctx.close();
                        break;
                    case 0x03:
                        break;
                    case 0x0F:
                        int messageLength = readVarInt(buf);
                        String message = buf.readBytes(messageLength).toString(CharsetUtil.UTF_8);
                        module.handleChatMessage(username, message);
                        break;
                    case 0x40:
                        ByteBuf response = Unpooled.buffer();
                        writeVarInt(response, 0x00);
                        response.writeLong(buf.readLong());
                        ctx.writeAndFlush(response);
                        break;
                }
            } finally {
                buf.release();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            module.logError(username + " error: " + cause.getMessage());
            ctx.close();
        }

        private int readVarInt(ByteBuf buf) {
            int value = 0;
            int length = 0;
            byte current;

            while (true) {
                current = buf.readByte();
                value |= (current & 0x7F) << (length * 7);

                if ((current & 0x80) != 0x80) break;
                if (length++ >= 5) throw new RuntimeException("VarInt too big");
            }

            return value;
        }

        private void writeVarInt(ByteBuf buf, int value) {
            while ((value & 0xFFFFFF80) != 0) {
                buf.writeByte(value & 0x7F | 0x80);
                value >>>= 7;
            }
            buf.writeByte(value);
        }
    }
}