package dev.revere.valance.ui.screen.alt;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.alt.Alt;
import dev.revere.valance.service.IAltService;
import dev.revere.valance.service.ISkijaService;
import dev.revere.valance.ui.screen.alt.components.GuiAltButton;
import dev.revere.valance.ui.screen.alt.components.GuiMainButton;
import dev.revere.valance.util.CookieLogin;
import dev.revere.valance.util.LoggerUtil;
import dev.revere.valance.util.ScrollUtil;
import dev.revere.valance.util.render.SkijaRenderUtil;
import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.litarvan.openauth.microsoft.model.response.MinecraftProfile;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Session;
import org.apache.commons.lang3.RandomStringUtils;
import org.lwjgl.input.Mouse;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Remi
 * @project valance
 * @date 5/5/2025
 */
public class AltManager extends GuiScreen {
    private final List<GuiAltButton> altAccountButtons = new ArrayList<>();
    private final List<GuiMainButton> altButtons = new ArrayList<>();
    private final ScrollUtil scrollUtils = new ScrollUtil();

    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":AltManager]";

    private final ISkijaService skijaService;
    private final IAltService altService;

    private Alt selectedAlt;
    private int pressedTime;
    private float scaledWidth;
    private float scaledHeight;

    public AltManager() {
        Optional<ISkijaService> skijaOpt = ClientLoader.getService(ISkijaService.class);
        this.skijaService = skijaOpt.orElseThrow(() -> {
            LoggerUtil.error(LOG_PREFIX, "Failed to get ISkijaService instance.");
            return new IllegalStateException("SkijaService is not available for AltManager.");
        });

        Optional<IAltService> altOpt = ClientLoader.getService(IAltService.class);
        this.altService = altOpt.orElseThrow(() -> {
            LoggerUtil.error(LOG_PREFIX, "Failed to get IAltService instance.");
            return new IllegalStateException("AltService is not available for AltManager.");
        });
        this.altService.setStatus("");

        this.altButtons.add(new GuiMainButton("Add Alt", 1, 100, 20));
        this.altButtons.add(new GuiMainButton("Delete Alt", 2, 100, 20));
        this.altButtons.add(new GuiMainButton("Clear Alts", 3, 100, 20));
        this.altButtons.add(new GuiMainButton("Cookie", 4, 100, 20));
        this.altButtons.add(new GuiMainButton("Edit", 5, 100, 20));
        this.altButtons.add(new GuiMainButton("Use", 6, 100, 20));
        this.altButtons.add(new GuiMainButton("Microsoft", 7, 100, 20));
    }

    /**
     * Initializes GUI dimensions and creates the alt list panel.
     */
    @Override
    public void initGui() {
        super.initGui();
        ScaledResolution sr = new ScaledResolution(mc);
        this.scaledWidth = sr.getScaledWidth();
        this.scaledHeight = sr.getScaledHeight();
        this.createAltPanel();
        this.scrollUtils.setScroll(0);
        this.selectedAlt = null;
        this.pressedTime = 0;
    }

    /**
     * Draws the screen content.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        final float scaleFactor = sr.getScaleFactor();

        final float topMargin = 50f;
        final float bottomMargin = 50f;
        final float listViewportHeight = this.scaledHeight - topMargin - bottomMargin;

        float totalContentHeight = altAccountButtons.size() * 35f;
        float maxScroll = Math.max(0, totalContentHeight - listViewportHeight);
        boolean isMouseInListArea = isHovered(mouseY, topMargin, this.scaledHeight - bottomMargin);
        float currentScroll = scrollUtils.getScroll(topMargin, maxScroll, isMouseInListArea);

        skijaService.runFrame(() -> {
            Canvas canvas = skijaService.getCanvas();
            if (canvas == null) return;

            canvas.save();
            float clipX_pixels = 0f;
            float clipY_pixels = topMargin * scaleFactor;
            float clipWidth_pixels = this.scaledWidth * scaleFactor;
            float clipHeight_pixels = listViewportHeight * scaleFactor;
            canvas.clipRect(Rect.makeXYWH(clipX_pixels, clipY_pixels, clipWidth_pixels, clipHeight_pixels));

            for (GuiAltButton button : this.altAccountButtons) {
                button.y = topMargin + (button.getId() * 35f) - currentScroll;
                button.x = (isMouseInListArea && button.isHovered(mouseX, mouseY))
                        ? this.scaledWidth / 2f - 103f
                        : this.scaledWidth / 2f - 100f;
                button.setSelected(selectedAlt == button.alt);

                float scaledButtonBottom = button.y + button.getHeight();
                if (scaledButtonBottom > topMargin && button.y < (this.scaledHeight - bottomMargin)) {
                    button.drawButton(canvas, scaleFactor);
                }
            }
            canvas.restore();

            Typeface typeface = Typeface.makeDefault();
            Font titleFont = new Font(typeface, 16f * scaleFactor);
            Font infoFont = new Font(typeface, 10f * scaleFactor);
            Font statusFont = new Font(typeface, 8f * scaleFactor);

            float screenWidthPixels = this.scaledWidth * scaleFactor;

            // --- Center Title ---
            String titleText = "Alt Manager";
            float titleWidthPixels = titleFont.measureTextWidth(titleText);
            float centeredTitleXPixels = (screenWidthPixels - titleWidthPixels) / 2f;
            float pixelTitleY = 20f * scaleFactor;
            SkijaRenderUtil.drawColoredString(canvas, titleFont, titleText, centeredTitleXPixels, pixelTitleY, Color.WHITE.getRGB(), true, true, 0);

            // --- Center Login Info ---
            String loginText = "Logged in as " + EnumChatFormatting.GREEN + mc.getSession().getUsername();
            float loginTextWidthPixels = SkijaRenderUtil.measureColoredStringWidth(infoFont, loginText);
            float centeredLoginXPixels = (screenWidthPixels - loginTextWidthPixels) / 2f;
            float pixelInfoY = 30f * scaleFactor;
            SkijaRenderUtil.drawColoredString(canvas, infoFont, loginText, centeredLoginXPixels, pixelInfoY, Color.WHITE.getRGB(), false, false, 150);

            // --- Center Status Info ---
            String currentStatus = altService.getStatus();
            if (currentStatus != null && !currentStatus.isEmpty()) {
                float statusWidthPixels = SkijaRenderUtil.measureColoredStringWidth(statusFont, currentStatus);
                float centeredStatusXPixels = (screenWidthPixels - statusWidthPixels) / 2f;
                float pixelStatusY = 40f * scaleFactor;
                int statusColor = (currentStatus.startsWith(EnumChatFormatting.RED.toString())) ? Color.RED.getRGB() : Color.WHITE.getRGB();
                SkijaRenderUtil.drawColoredString(canvas, statusFont, currentStatus, centeredStatusXPixels, pixelStatusY, statusColor, false, false, 0);
            }

            for (GuiMainButton button : altButtons) {
                float scaledButtonX, scaledButtonY;
                if (button.getId() <= 3) {
                    scaledButtonX = this.scaledWidth / 2f - 155f + (button.getId() - 1) * 105f;
                    scaledButtonY = this.scaledHeight - 45f;
                } else {
                    scaledButtonX = this.scaledWidth / 2f - 210f + (button.getId() - 4) * 105f;
                    scaledButtonY = this.scaledHeight - 22f;
                }
                button.setX(scaledButtonX);
                button.setY(scaledButtonY);
                button.setWidth(100);
                button.setHeight(20);

                button.drawButton(mouseX, mouseY, scaleFactor, canvas);
            }
        });

        if (totalContentHeight > listViewportHeight) {
            float scrollBarHeightPixels = Math.max(20f * scaleFactor, listViewportHeight * scaleFactor * (listViewportHeight / totalContentHeight));
            scrollBarHeightPixels = Math.min(scrollBarHeightPixels, listViewportHeight * scaleFactor);

            float scrollBarX_scaled = this.scaledWidth / 2f + 105f;
            float scrollPercentage = (maxScroll == 0) ? 0 : (currentScroll / maxScroll);
            float scrollBarY_scaled = topMargin + (listViewportHeight - (scrollBarHeightPixels / scaleFactor)) * scrollPercentage;

            Gui.drawRect(
                    (int) scrollBarX_scaled,
                    (int) scrollBarY_scaled,
                    (int) (scrollBarX_scaled + 5),
                    (int) (scrollBarY_scaled + (scrollBarHeightPixels / scaleFactor)),
                    0xFFAAAAAA
            );
        }
    }

    /**
     * Handles mouse clicks (uses scaled coordinates).
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (GuiMainButton button : altButtons) {
            if (button.mousePressed(mouseX, mouseY)) {
                handleMainButtonClick(button);
                return;
            }
        }

        float listViewportY = 50f;
        float bottomMargin = 50f;
        if (isHovered(mouseY, listViewportY, scaledHeight - bottomMargin)) {

            for (GuiAltButton button : this.altAccountButtons) {
                if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth() && mouseY >= button.y && mouseY < button.y + button.getHeight()) {
                    handleAltButtonClick(button, mouseButton);
                    return;
                }
            }
        }
    }

    /**
     * Handles mouse wheel scrolling.
     */
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            int mouseY = sr.getScaledHeight() - Mouse.getEventY() * sr.getScaledHeight() / mc.displayHeight - 1;
            float listViewportY = 50f;
            float bottomMargin = 50f;

            if (isHovered(mouseY, listViewportY, this.scaledHeight - bottomMargin)) {
                float listViewportHeight = this.scaledHeight - 100f;
                float totalContentHeight = altAccountButtons.size() * 35f;
                float maxScroll = Math.max(0, totalContentHeight - listViewportHeight);

                if (wheel > 0) {
                    scrollUtils.setScroll(scrollUtils.getScroll() - 15);
                } else {
                    scrollUtils.setScroll(scrollUtils.getScroll() + 15);
                }
                scrollUtils.setScroll(Math.max(0, Math.min(scrollUtils.getScroll(), maxScroll)));
            }
        }
    }

    /**
     * Handles clicks on main action buttons.
     */
    private void handleMainButtonClick(GuiMainButton button) {
        try {
            switch (button.getId()) {
                case 1: {
                    String username = "valance_" + RandomStringUtils.randomAlphanumeric(6);
                    altService.addAlt(new Alt(username, username, "", System.currentTimeMillis(), "cracked", null));
                    createAltPanel();
                    break;
                }
                case 2: {
                    if (selectedAlt != null) {
                        altService.getAlts().remove(selectedAlt);
                        selectedAlt = null;
                        createAltPanel();
                    } else {
                        altService.setStatus(EnumChatFormatting.YELLOW + "Select alt to delete.");
                    }
                    break;
                }
                case 3: {
                    int count = altService.getAlts().size();
                    altService.getAlts().clear();
                    altService.setStatus(EnumChatFormatting.GREEN + "Cleared " + count + " alts.");
                    selectedAlt = null;
                    createAltPanel();
                    break;
                }
                case 4:
                    handleCookieLogin();
                    break;
                case 5: {
                    if (selectedAlt != null) {
                        altService.setStatus(EnumChatFormatting.YELLOW + "Edit not implemented.");
                    } else {
                        altService.setStatus(EnumChatFormatting.YELLOW + "Select alt to edit.");
                    }
                    break;
                }
                case 6: {
                    if (selectedAlt != null) {
                        loginWithSelectedAlt();
                    } else {
                        altService.setStatus(EnumChatFormatting.YELLOW + "Select alt to use.");
                    }
                    break;
                }
                case 7:
                    handleMicrosoftLogin();
                    break;
            }
        } catch (Exception e) {
            LoggerUtil.error(LOG_PREFIX, "Error handling button click " + button.getId() + ": " + e.getMessage(), e);
            altService.setStatus(EnumChatFormatting.RED + "An error occurred.");
        }
    }

    /**
     * Handles clicks on buttons within the alt list.
     */
    private void handleAltButtonClick(GuiAltButton button, int mouseButton) {
        if (mouseButton == 0) {
            if (selectedAlt == button.alt) {
                pressedTime++;
                if (pressedTime >= 2) {
                    loginWithSelectedAlt();
                    pressedTime = 0;
                }
            } else {
                selectedAlt = button.alt;
                pressedTime = 1;
            }
        }
    }

    private void loginWithSelectedAlt() {
        if (selectedAlt == null) {
            altService.setStatus(EnumChatFormatting.YELLOW + "No alt selected.");
            return;
        }

        LoggerUtil.info(LOG_PREFIX, "Attempting login for alt: " + selectedAlt.getAlias());

        String type = selectedAlt.getType() != null ? selectedAlt.getType().toLowerCase() : "unknown";

        switch (type) {
            case "cookie":
                if (selectedAlt.getUuid() == null || selectedAlt.getUuid().isEmpty() || selectedAlt.getPassword() == null || selectedAlt.getPassword().isEmpty()) {
                    altService.setStatus(EnumChatFormatting.RED + "Invalid cookie data (missing UUID/Token).");
                    LoggerUtil.error(LOG_PREFIX, "Cookie alt " + selectedAlt.getAlias() + " missing data.");
                    return;
                }
                try {
                    mc.session = new Session(selectedAlt.getUsername(), selectedAlt.getUuid(), selectedAlt.getPassword(), "legacy");
                    LoggerUtil.info(LOG_PREFIX, "Logged in with cookie for " + selectedAlt.getAlias());
                } catch (Exception e) {
                    altService.setStatus(EnumChatFormatting.RED + "Error setting session (Cookie).");
                    LoggerUtil.error(LOG_PREFIX, "Error creating session for cookie alt " + selectedAlt.getAlias(), e);
                }
                break;

            case "cracked":
                if (altService.isValidCrackedAlt(selectedAlt.getUsername())) {
                    try {
                        mc.session = new Session(selectedAlt.getUsername(), "", "", "mojang");
                        LoggerUtil.info(LOG_PREFIX, "Logged in cracked alt " + selectedAlt.getAlias());
                    } catch (Exception e) {
                        altService.setStatus(EnumChatFormatting.RED + "Error setting session (Cracked).");
                        LoggerUtil.error(LOG_PREFIX, "Error creating session for cracked alt " + selectedAlt.getAlias(), e);
                    }
                } else {
                    altService.setStatus(EnumChatFormatting.RED + "Invalid cracked username!");
                    LoggerUtil.error(LOG_PREFIX, "Invalid cracked username: " + selectedAlt.getUsername());
                }
                break;

            case "microsoft":
            default:
                if (selectedAlt.getUsername() == null || selectedAlt.getPassword() == null) {
                    altService.setStatus(EnumChatFormatting.RED + "Missing credentials for premium alt.");
                    LoggerUtil.error(LOG_PREFIX, "Premium alt " + selectedAlt.getAlias() + " missing username or password.");
                    return;
                }
                AltLoginThread loginThread = new AltLoginThread(selectedAlt.getUsername(), selectedAlt.getPassword());
                loginThread.start();
                break;
        }
    }

    private void handleCookieLogin() {
        boolean wasFullscreen = mc.gameSettings.fullScreen;
        if (wasFullscreen) mc.toggleFullscreen();

        new Thread(() -> {
            try {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }
                JOptionPane.showMessageDialog(null, "Select cookies.txt file (Netscape format).", "Cookie Login", JOptionPane.INFORMATION_MESSAGE);
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
                chooser.setDialogTitle("Select cookies.txt");

                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    mc.addScheduledTask(() -> altService.setStatus(EnumChatFormatting.YELLOW + "Processing cookie file..."));
                    try {
                        CookieLogin.LoginData loginData = CookieLogin.loginWithCookie(chooser.getSelectedFile());
                        if (loginData == null) {
                            mc.addScheduledTask(() -> altService.setStatus(EnumChatFormatting.RED + "Cookie login failed. Check file/cookies."));
                            LoggerUtil.error(LOG_PREFIX, "Cookie login returned null data.");
                        } else {
                            mc.addScheduledTask(() -> {
                                mc.session = new Session(loginData.username, loginData.uuid, loginData.mcToken, "legacy");
                                Alt cookieAlt = new Alt(loginData.username, loginData.username, loginData.mcToken, System.currentTimeMillis(), "cookie", loginData.uuid);
                                altService.addAlt(cookieAlt);
                                altService.setStatus(EnumChatFormatting.GREEN + "Cookie login successful!");
                                LoggerUtil.info(LOG_PREFIX, "Cookie login success for " + loginData.username);
                                createAltPanel();
                            });
                        }
                    } catch (Exception e) {
                        mc.addScheduledTask(() -> altService.setStatus(EnumChatFormatting.RED + "Cookie login error. See logs."));
                        LoggerUtil.error(LOG_PREFIX, "Exception during cookie login: " + e.getMessage(), e);
                    }
                } else {
                    mc.addScheduledTask(() -> altService.setStatus("Cookie login cancelled."));
                }
            } finally {
                if (wasFullscreen) mc.addScheduledTask(() -> {
                    if (!mc.gameSettings.fullScreen) mc.toggleFullscreen();
                });
            }
        }, "CookieLoginThread").start();
    }

    private void handleMicrosoftLogin() {
        altService.setStatus(EnumChatFormatting.YELLOW + "Opening Microsoft login...");
        new Thread(() -> {
            try {
                MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
                MicrosoftAuthResult result = authenticator.loginWithWebview();

                if (result == null) {
                    mc.addScheduledTask(() -> altService.setStatus(EnumChatFormatting.RED + "Microsoft login cancelled or failed."));
                    LoggerUtil.warn(LOG_PREFIX, "Microsoft login returned null.");
                    return;
                }

                MinecraftProfile profile = result.getProfile();
                String username = profile.getName();
                String uuid = profile.getId();
                String token = result.getAccessToken();

                mc.addScheduledTask(() -> {
                    LoggerUtil.info(LOG_PREFIX, "Microsoft login success: " + username);
                    mc.session = new Session(username, uuid, token, "microsoft");
                    Alt msAlt = new Alt(username, username, token, System.currentTimeMillis(), "microsoft", uuid);
                    altService.addAlt(msAlt);
                    createAltPanel();
                });

            } catch (MicrosoftAuthenticationException e) {
                mc.addScheduledTask(() -> altService.setStatus(EnumChatFormatting.RED + "MS Auth Error: " + e.getMessage()));
                LoggerUtil.error(LOG_PREFIX, "MS Auth Exception: " + e.getMessage());
            } catch (Exception e) {
                mc.addScheduledTask(() -> altService.setStatus(EnumChatFormatting.RED + "MS login error. See logs."));
                LoggerUtil.error(LOG_PREFIX, "Unexpected error during MS login", e);
            }
        }, "MicrosoftLoginThread").start();
    }


    /**
     * Checks hover state using scaled coordinates.
     */
    private boolean isHovered(int mouseY, float minY, float maxY) {
        return mouseY >= minY && mouseY < maxY;
    }

    /**
     * Recreates the alt button list.
     */
    private void createAltPanel() {
        altAccountButtons.clear();
        if (this.altService == null) {
            LoggerUtil.error(LOG_PREFIX, "AltService is null in createAltPanel.");
            return;
        }
        List<Alt> currentAlts = this.altService.getAlts();
        float currentScaledWidth = (this.scaledWidth > 0) ? this.scaledWidth : new ScaledResolution(mc).getScaledWidth();

        for (int i = 0; i < currentAlts.size(); i++) {
            Alt alt = currentAlts.get(i);
            if (alt == null) {
                LoggerUtil.warn(LOG_PREFIX, "Null alt at index " + i);
                continue;
            }
            this.altAccountButtons.add(new GuiAltButton(i, currentScaledWidth / 2f - 100f, 50f + i * 35f, 200f, 30f, alt));
        }
    }
}