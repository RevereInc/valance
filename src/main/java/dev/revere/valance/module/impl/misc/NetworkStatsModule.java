package dev.revere.valance.module.impl.misc;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.event.type.game.ClientTickEvent;
import dev.revere.valance.event.type.packet.PacketEvent;
import dev.revere.valance.event.type.render.Render2DEvent;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.service.IEventBusService;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.server.MinecraftServer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static dev.revere.valance.util.MinecraftUtil.isInGame;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
@ModuleInfo(name = "NetworkStats", description = "Displays network latency and packet info.", category = Category.MISC, defaultEnabled = true)
public class NetworkStatsModule extends AbstractModule {

    // --- Proxy Detection ---
    private enum ProxyType { NONE, BUNGEE, VELOCITY, UNKNOWN }
    private volatile ProxyType detectedProxy = ProxyType.NONE;

    // Known proxy plugin channels
    private static final String BUNGEE_CORD_CHANNEL = "BungeeCord";
    private static final String VELOCITY_LEGACY_CHANNEL = "velocity:player_info";
    private static final String VELOCITY_MODERN_CHANNEL = "velocity:forwarding";

    // --- State for Stats ---
    private volatile int latency = -1;
    private volatile int packetsSentPerSecond = 0;
    private volatile int packetsReceivedPerSecond = 0;
    private volatile String serverBrand = "Unknown";
    private volatile long lastPacketReceivedTime = 0L;

    // Counters for PPS calculation
    private transient int currentSentCount = 0;
    private transient int currentReceivedCount = 0;
    private transient long lastPpsUpdateTime = 0L;

    // For Latency calculation (KeepAlive RTT)
    private final Queue<KeepAliveEntry> pendingKeepAlives = new ConcurrentLinkedQueue<>();
    private static final long KEEP_ALIVE_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);

    /**
     * Constructor for dependency injection.
     * @param eventBusService The injected Event Bus service.
     */
    public NetworkStatsModule(IEventBusService eventBusService) {
        super(eventBusService);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetStats();
        lastPpsUpdateTime = System.currentTimeMillis();
        lastPacketReceivedTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetStats();
    }

    private void resetStats() {
        latency = -1;
        packetsSentPerSecond = 0;
        packetsReceivedPerSecond = 0;
        currentSentCount = 0;
        currentReceivedCount = 0;
        pendingKeepAlives.clear();
        serverBrand = "Unknown";
        detectedProxy = ProxyType.NONE;
        lastPacketReceivedTime = 0L;
    }


    /**
     * Updates PPS counters and cleans up timed-out KeepAlive packets every tick.
     * @param event The client tick event.
     */
    @Subscribe
    public void onTick(ClientTickEvent event) {
        if (!isEnabled()) return;

        long now = System.currentTimeMillis();

        if (now - lastPpsUpdateTime >= 1000L) {
            packetsSentPerSecond = currentSentCount;
            packetsReceivedPerSecond = currentReceivedCount;
            currentSentCount = 0;
            currentReceivedCount = 0;
            lastPpsUpdateTime = now;
        }

        pendingKeepAlives.removeIf(entry -> (now - entry.timestamp) > KEEP_ALIVE_TIMEOUT_MS);
    }

    /**
     * Unified handler for both sent and received packets.
     * Checks the event state to count PPS and handle KeepAlive packets for latency.
     * @param event The PacketEvent instance.
     */
    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!isEnabled()) return;

        if (event.getEventState() == PacketEvent.EventState.SENDING) {
            currentSentCount++;
            Packet<?> packet = event.getRawPacket();

            if (packet instanceof C00PacketKeepAlive) {
                pendingKeepAlives.offer(new KeepAliveEntry(System.currentTimeMillis()));
            }

        } else if (event.getEventState() == PacketEvent.EventState.RECEIVING) {
            currentReceivedCount++;
            lastPacketReceivedTime = System.currentTimeMillis();
            Packet<?> packet = event.getRawPacket();

            if (packet instanceof S00PacketKeepAlive) {
                KeepAliveEntry oldestSent = pendingKeepAlives.poll();
                if (oldestSent != null) {
                    long roundTripTime = System.currentTimeMillis() - oldestSent.timestamp;
                    latency = (int) (roundTripTime / 2);
                }
            }
            else if (packet instanceof S3FPacketCustomPayload) {
                S3FPacketCustomPayload payloadPacket = event.getPacket();
                String channel = payloadPacket.getChannelName();

                if ("MC|Brand".equals(channel)) {
                    try {
                        PacketBuffer data = payloadPacket.getBufferData();
                        if (data.readableBytes() > 0) {
                            serverBrand = data.readStringFromBuffer(32767);
                            System.out.println("[" + ClientLoader.CLIENT_NAME + ":NetworkStats] Received Server Brand: " + serverBrand);
                        } else {
                            System.out.println("[" + ClientLoader.CLIENT_NAME + ":NetworkStats] Received empty MC|Brand payload.");
                            serverBrand = "Empty";
                        }
                        data.readerIndex(data.writerIndex());
                    } catch (Exception e) {
                        System.err.println("[" + ClientLoader.CLIENT_NAME + ":NetworkStats] Error reading server brand payload:");
                        e.printStackTrace();
                        serverBrand = "Error";
                    }
                }
                else if (detectedProxy == ProxyType.NONE) {
                    if (BUNGEE_CORD_CHANNEL.equals(channel)) {
                        System.out.println("[" + ClientLoader.CLIENT_NAME + ":NetworkStats] BungeeCord plugin channel detected.");
                        detectedProxy = ProxyType.BUNGEE;
                    } else if (VELOCITY_LEGACY_CHANNEL.equals(channel) || channel.startsWith(VELOCITY_MODERN_CHANNEL)) {
                        System.out.println("[" + ClientLoader.CLIENT_NAME + ":NetworkStats] Velocity plugin channel detected (" + channel + ").");
                        detectedProxy = ProxyType.VELOCITY;
                    }
                }
            }
        }
    }

    /**
     * Renders the network statistics on the screen using the Render2DEvent.
     * @param event The render event containing screen dimensions.
     */
    @Subscribe
    public void onRender2D(Render2DEvent event) {
        if (!isEnabled() || !isInGame()) return;

        FontRenderer fr = mc.fontRendererObj;
        int yOffset = 0;
        int xPos = 2;
        int yPos = 2;
        int color = 0xFFFFFFFF;

        // Calculate time since last packet ("Lag")
        long timeSinceLastPacket = (lastPacketReceivedTime == 0L) ? -1 : (System.currentTimeMillis() - lastPacketReceivedTime);
        String lagText = "Lag: " + (timeSinceLastPacket == -1 ? "N/A" : timeSinceLastPacket + " ms");
        int lagColor = color;
        if (timeSinceLastPacket > 1000) lagColor = 0xFFFF55; // Yellow if > 1s
        if (timeSinceLastPacket > 5000) lagColor = 0xFF5555; // Red if > 5s

        int entityCount = mc.theWorld.loadedEntityList.size();

        String pingText = "Ping: " + (latency == -1 ? "N/A" : latency + " ms");
        fr.drawStringWithShadow(pingText, (float)xPos, (float)(yPos + yOffset), color);
        yOffset += fr.FONT_HEIGHT + 1;

        fr.drawStringWithShadow(lagText, (float)xPos, (float)(yPos + yOffset), lagColor);
        yOffset += fr.FONT_HEIGHT + 1;

        String ppsText = String.format("PPS In: %d / Out: %d", packetsReceivedPerSecond, packetsSentPerSecond);
        fr.drawStringWithShadow(ppsText, (float)xPos, (float)(yPos + yOffset), color);
        yOffset += fr.FONT_HEIGHT + 1;

        String entityText = "Entities: " + entityCount;
        fr.drawStringWithShadow(entityText, (float)xPos, (float)(yPos + yOffset), color);
        yOffset += fr.FONT_HEIGHT + 1;

        String backendBrandText = "Backend: " + serverBrand;
        fr.drawStringWithShadow(backendBrandText, (float)xPos, (float)(yPos + yOffset), color);
        yOffset += fr.FONT_HEIGHT + 1;

        // todo: fix this
        String proxyText;
        switch(detectedProxy) {
            case BUNGEE:
                proxyText = "Proxy: Detected (Bungee)";
                break;
            case VELOCITY:
                proxyText = "Proxy: Detected (Velocity)";
                break;
            case UNKNOWN:
                proxyText = "Proxy: Detected (Unknown)";
                break;
            case NONE:
            default:
                proxyText = "Proxy: N/A";
                break;
        }
        fr.drawStringWithShadow(proxyText, (float)xPos, (float)(yPos + yOffset), color);
        yOffset += fr.FONT_HEIGHT + 1;

        fr.drawStringWithShadow("Server IP: " + mc.getNetHandler().getNetworkManager().getRemoteAddress(), (float)xPos, (float)(yPos + yOffset), color);
    }

    private static class KeepAliveEntry {
        final long timestamp;

        KeepAliveEntry(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
