package dev.revere.valance.service.impl;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.core.ClientContext;
import dev.revere.valance.core.annotation.Inject;
import dev.revere.valance.core.annotation.Service;
import dev.revere.valance.core.exception.ServiceException;
import dev.revere.valance.service.IConfigService;
import dev.revere.valance.service.IDraggableService;
import dev.revere.valance.ui.draggable.Draggable;
import dev.revere.valance.util.Logger;
import dev.revere.valance.util.SkijaRenderUtil;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Remi
 * @project valance
 * @date 4/30/2025
 */
@Service(provides = IDraggableService.class, priority = 40)
public class DraggableManagerService implements IDraggableService {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":DraggableManager]";

    private final List<Draggable> draggables = new CopyOnWriteArrayList<>();
    private Draggable activeDragger = null;

    // TODO: Implement saving and loading positions
    private final IConfigService configService;

    @Inject
    public DraggableManagerService(IConfigService configService) {
        this.configService = configService;
    }

    @Override
    public void initialize(ClientContext context) throws ServiceException {
    }

    @Override
    public void shutdown(ClientContext context) throws ServiceException {
    }

    @Override
    public void register(Draggable draggable) {
        if (draggable != null && !draggables.contains(draggable)) {
            draggables.add(draggable);
            Logger.info(LOG_PREFIX, "Registered: " + draggable.getName());
        }
    }

    @Override
    public void unregister(Draggable draggable) {
        if (draggable != null) {
            draggables.remove(draggable);
        }
    }

    @Override
    public List<Draggable> getDraggables() {
        return Collections.unmodifiableList(draggables);
    }


    @Override
    public void draw(float mouseX, float mouseY, Canvas canvas) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int scaleFactorInt = sr.getScaleFactor();
        float scaleFactor = (float) scaleFactorInt;

        float mouseX_pixels = mouseX * scaleFactor;
        float mouseY_pixels = mouseY * scaleFactor;

        for (Draggable draggable : draggables) {
            draggable.draw(mouseX_pixels, mouseY_pixels);

            if (draggable.getModule().isEnabled() && (draggable == activeDragger || draggable.hovered(mouseX_pixels, mouseY_pixels))) {
                SkijaRenderUtil.drawOutlineRoundRect(draggable.getInternalX(), draggable.getInternalY(), draggable.getTotalWidth(), draggable.getTotalHeight(), 16f, 5, 0xAA2C2C2E);
            }
        }
    }

    @Override
    public void handleMouseClick(float mouseX, float mouseY, int button) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int scaleFactor = sr.getScaleFactor();
        float mouseX_pixels = mouseX * scaleFactor;
        float mouseY_pixels = mouseY * scaleFactor;

        activeDragger = null;
        for (int i = draggables.size() - 1; i >= 0; i--) {
            Draggable draggable = draggables.get(i);
            if (draggable.hovered(mouseX_pixels, mouseY_pixels)) {
                activeDragger = draggable;
                draggable.onClick(mouseX_pixels, mouseY_pixels, button);
                if (i != draggables.size() - 1) {
                    draggables.remove(i);
                    draggables.add(draggable);
                }
                break;
            }
        }
    }

    @Override
    public void handleMouseRelease(int button) {
        if (activeDragger != null) {
            activeDragger.onRelease(button);
            activeDragger = null;
        }
    }
}
