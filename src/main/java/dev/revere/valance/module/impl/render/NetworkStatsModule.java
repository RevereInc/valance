package dev.revere.valance.module.impl.render;

import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.event.type.game.ClientTickEvent;
import dev.revere.valance.event.type.packet.PacketEvent;
import dev.revere.valance.event.type.render.Render2DEvent;
import dev.revere.valance.module.Category;
import dev.revere.valance.module.annotation.ModuleInfo;
import dev.revere.valance.module.api.AbstractModule;
import dev.revere.valance.service.IDraggableService;
import dev.revere.valance.service.IEventBusService;
import dev.revere.valance.service.ISkijaService;
import dev.revere.valance.ui.draggable.Draggable;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static dev.revere.valance.util.MinecraftUtil.isInGame;

/**
 * @author Remi
 * @project valance
 * @date 4/30/2025
 */
@ModuleInfo(name = "NetworkStats", displayName = "Network Stats", description = "Displays network latency and packet info graphically.", category = Category.RENDER)
public class NetworkStatsModule extends AbstractModule {
    private final ISkijaService skijaService;
    private final Draggable draggable;

    private Font contentFont = null;
    private Font smallFont = null;

    // --- Proxy Detection ---
    private enum ProxyType {NONE, BUNGEE, VELOCITY, UNKNOWN}

    private volatile ProxyType detectedProxy = ProxyType.NONE;
    private static final String BUNGEE_CORD_CHANNEL = "BungeeCord";
    private static final String VELOCITY_LEGACY_CHANNEL = "velocity:player_info";
    private static final String VELOCITY_MODERN_CHANNEL = "velocity:forwarding";

    // --- State for Stats ---
    private volatile int latency = -1;
    private volatile int packetsReceivedPerSecond = 0;
    private volatile int packetsSentPerSecond = 0;
    private volatile long lastPacketReceivedTime = 0L;
    private volatile String serverBrand = "Unknown";

    // Counters for PPS calculation
    private transient int currentSentCount = 0;
    private transient int currentReceivedCount = 0;
    private transient long lastPpsUpdateTime = 0L;

    // For Latency calculation
    private final Queue<KeepAliveEntry> pendingKeepAlives = new ConcurrentLinkedQueue<>();
    private static final long KEEP_ALIVE_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);

    // --- Style Settings ---
    private final float blurSigmaX = 7f;
    private final float blurSigmaY = 7f;
    private final int contentOpacity = 235;
    private final boolean blurEnabled = true;
    private final float paddingPixels = 20f;
    private final float contentFontSizePixels = 13f;
    private final float smallFontSizePixels = 10f;

    public NetworkStatsModule(IEventBusService eventBusService, ISkijaService skijaService, IDraggableService draggableService) {
        super(eventBusService);
        this.skijaService = skijaService;

        this.draggable = new Draggable(this, "Network Stats", 10, 400);
        this.draggable.setContentWidth(400f);
        this.draggable.setContentHeight(150f);

        draggableService.register(this.draggable);
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
        packetsReceivedPerSecond = 0;
        packetsSentPerSecond = 0;
        currentSentCount = 0;
        currentReceivedCount = 0;
        pendingKeepAlives.clear();
        serverBrand = "Unknown";
        detectedProxy = ProxyType.NONE;
        lastPacketReceivedTime = 0L;
    }


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

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!isEnabled()) return;
        if (event.getEventState() == PacketEvent.EventState.SENDING) {
            currentSentCount++;
            Packet<?> packet = event.getRawPacket();
            if (packet instanceof C00PacketKeepAlive)
                pendingKeepAlives.offer(new KeepAliveEntry(System.currentTimeMillis()));
        } else if (event.getEventState() == PacketEvent.EventState.RECEIVING) {
            currentReceivedCount++;
            lastPacketReceivedTime = System.currentTimeMillis();
            Packet<?> packet = event.getRawPacket();
            if (packet instanceof S00PacketKeepAlive) {
                KeepAliveEntry oldestSent = pendingKeepAlives.poll();
                if (oldestSent != null) latency = (int) ((System.currentTimeMillis() - oldestSent.timestamp) / 2);
            } else if (packet instanceof S3FPacketCustomPayload) {
                S3FPacketCustomPayload payloadPacket = event.getPacket();
                String channel = payloadPacket.getChannelName();
                if ("MC|Brand".equals(channel)) {
                    try {
                        PacketBuffer data = payloadPacket.getBufferData();
                        if (data.readableBytes() > 0) serverBrand = data.readStringFromBuffer(32767);
                        else serverBrand = "Empty";
                        data.readerIndex(data.writerIndex());
                    } catch (Exception e) {
                        serverBrand = "Error";
                    }
                } else if (detectedProxy == ProxyType.NONE) {
                    if (BUNGEE_CORD_CHANNEL.equals(channel)) detectedProxy = ProxyType.BUNGEE;
                    else if (VELOCITY_LEGACY_CHANNEL.equals(channel) || channel.startsWith(VELOCITY_MODERN_CHANNEL))
                        detectedProxy = ProxyType.VELOCITY;
                }
            }
        }
    }

    @Subscribe
    public void onRender(Render2DEvent event) {
        if (!isEnabled()) {
            return;
        }
        if (!isInGame() || skijaService == null || !skijaService.isInitialized()) return;

        float neededHeight = (contentFontSizePixels + 4f) * 6;
        neededHeight += paddingPixels * 2;
        neededHeight = Math.max(neededHeight, 60f);

        draggable.setContentWidth(400f);
        draggable.setContentHeight(neededHeight);

        skijaService.runFrame(() -> {
            Canvas canvas = skijaService.getCanvas();
            if (canvas == null) return;

            if (contentFont == null) {
                contentFont = new Font(Typeface.makeDefault(), contentFontSizePixels);
            }
            if (smallFont == null) {
                smallFont = new Font(Typeface.makeDefault(), smallFontSizePixels);
            }

            draggable.drawFrame(canvas, blurEnabled, blurSigmaX, blurSigmaY, contentOpacity);

            Rect contentBoundsPixel = Rect.makeXYWH(
                    draggable.getX(), draggable.getY(),
                    draggable.getWidth(), draggable.getHeight()
            );

            if (contentBoundsPixel.getWidth() <= 0 || contentBoundsPixel.getHeight() <= 0) return;

            // --- Layout within Content Bounds ---
            float contentInnerWidth = contentBoundsPixel.getWidth() - paddingPixels * 2;
            float contentInnerHeight = contentBoundsPixel.getHeight() - paddingPixels * 2;

            if (contentInnerWidth <= 0 || contentInnerHeight <= 0) return;

            float textInfoWidthPixels = contentInnerWidth * 0.55f;
            float indicatorAreaWidthPixels = Math.max(0, contentInnerWidth - textInfoWidthPixels - paddingPixels);

            Rect textInfoArea = Rect.makeXYWH(
                    contentBoundsPixel.getLeft() + paddingPixels,
                    contentBoundsPixel.getTop() + paddingPixels,
                    textInfoWidthPixels,
                    contentInnerHeight
            );
            Rect lagCircleArea = Rect.makeXYWH(
                    textInfoArea.getRight() + paddingPixels,
                    contentBoundsPixel.getTop() + paddingPixels,
                    indicatorAreaWidthPixels,
                    contentInnerHeight
            );

            try (Paint textPaint = new Paint().setColor(draggable.getTextColor()).setAntiAlias(true);
                 Paint smallTextPaint = new Paint().setColor(draggable.getTextColor()).setAntiAlias(true)) {
                float textBaselineOffset = contentFont.getMetrics().getAscent() * -1;
                float lineHeight = contentFontSizePixels + 4f;

                float currentTextY = textInfoArea.getTop();

                if (textInfoArea.getHeight() >= 0) {
                    String s = "Ping: " + (latency == -1 ? "N/A" : latency + " ms");
                    canvas.drawString(s, textInfoArea.getLeft(), currentTextY + textBaselineOffset, contentFont, textPaint);
                    currentTextY += lineHeight;
                }
                if (textInfoArea.getHeight() >= lineHeight) {
                    int ec = mc.theWorld != null ? mc.theWorld.loadedEntityList.size() : 0;
                    String s = "Entities: " + ec;
                    canvas.drawString(s, textInfoArea.getLeft(), currentTextY + textBaselineOffset, contentFont, textPaint);
                    currentTextY += lineHeight;
                }
                if (textInfoArea.getHeight() >= lineHeight * 2) {
                    String s = "Backend: " + serverBrand;
                    if (contentFont.measureText(s).getWidth() > textInfoArea.getWidth()) {
                        s = s.substring(0, Math.min(s.length(), 20)) + "...";
                    }
                    canvas.drawString(s, textInfoArea.getLeft(), currentTextY + textBaselineOffset, contentFont, textPaint);
                    currentTextY += lineHeight;
                }
                if (textInfoArea.getHeight() >= lineHeight * 3) {
                    String s;
                    switch (detectedProxy) {
                        case BUNGEE:
                            s = "Proxy: Bungee";
                            break;
                        case VELOCITY:
                            s = "Proxy: Velocity";
                            break;
                        case UNKNOWN:
                            s = "Proxy: Unknown";
                            break;
                        default:
                            s = "Proxy: N/A";
                            break;
                    }
                    canvas.drawString(s, textInfoArea.getLeft(), currentTextY + textBaselineOffset, contentFont, textPaint);
                    currentTextY += lineHeight;
                }
                if (textInfoArea.getHeight() >= lineHeight * 4) {
                    String s = "IP: " + (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager().getRemoteAddress() != null ? mc.getNetHandler().getNetworkManager().getRemoteAddress().toString().replace("/", "") : "N/A");
                    if (contentFont.measureText(s).getWidth() > textInfoArea.getWidth()) {
                        s = s.substring(0, Math.min(s.length(), 25)) + "...";
                    }
                    canvas.drawString(s, textInfoArea.getLeft(), currentTextY + textBaselineOffset, contentFont, textPaint);
                    currentTextY += lineHeight;
                }
                if (textInfoArea.getHeight() >= lineHeight * 5) {
                    String s = String.format("PPS In: %d / Out: %d", packetsReceivedPerSecond, packetsSentPerSecond);
                    canvas.drawString(s, textInfoArea.getLeft(), currentTextY + textBaselineOffset, contentFont, textPaint);
                }

                if (lagCircleArea.getWidth() > 0 && lagCircleArea.getHeight() > 0) {
                    drawLagIndicator(canvas, lagCircleArea, smallFont, smallTextPaint,
                            0x55888888, 0xFF55FF55, 0xFFFFFF55, 0xFFFF5555);
                }

            } catch (Exception e) {
                System.err.println("Error rendering NetworkStats content: " + e.getMessage());
            }
        });
    }

    private void drawLagIndicator(Canvas canvas, Rect area, Font font, Paint textPaint, int baseColor, int goodColor, int warnColor, int badColor) {
        long timeSinceLastPacket = (lastPacketReceivedTime == 0L) ? -1 : (System.currentTimeMillis() - lastPacketReceivedTime);
        float circleCx = area.getLeft() + area.getWidth() / 2f;
        float circleCy = area.getTop() + area.getHeight() / 2f;
        float radius = Math.min(area.getWidth(), area.getHeight()) / 2f - 4f;
        if (radius <= 0) return;
        float strokeWidth = 3f;
        int lagColor;
        if (timeSinceLastPacket < 0) lagColor = baseColor;
        else if (timeSinceLastPacket <= 500) lagColor = goodColor;
        else if (timeSinceLastPacket <= 2000) lagColor = warnColor;
        else lagColor = badColor;
        float maxLagForFullCircle = 2000f;
        float sweepAngle = (timeSinceLastPacket < 0) ? 0f : Math.min(360f, (timeSinceLastPacket / maxLagForFullCircle) * 360f);
        float left = circleCx - radius;
        float top = circleCy - radius;
        float right = circleCx + radius;
        float bottom = circleCy + radius;
        try (Paint basePaint = new Paint().setColor(baseColor).setMode(PaintMode.STROKE).setStrokeWidth(strokeWidth).setAntiAlias(true);
             Paint lagPaint = new Paint().setColor(lagColor).setMode(PaintMode.STROKE).setStrokeWidth(strokeWidth).setStrokeCap(PaintStrokeCap.ROUND).setAntiAlias(true)) {
            canvas.drawArc(left, top, right, bottom, 0, 360, false, basePaint);
            if (sweepAngle > 0) canvas.drawArc(left, top, right, bottom, -90, sweepAngle, false, lagPaint);
        }
        String lagText = (timeSinceLastPacket < 0) ? "N/A" : timeSinceLastPacket + "ms";
        float textWidth = font.measureText(lagText).getWidth();
        float textX = circleCx - textWidth / 2f;
        float textY = circleCy - font.getMetrics().getAscent() / 2f;
        canvas.drawString(lagText, textX, textY, font, textPaint);
    }

    private static class KeepAliveEntry {
        final long timestamp;

        KeepAliveEntry(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}