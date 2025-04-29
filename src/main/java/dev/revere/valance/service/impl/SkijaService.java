package dev.revere.valance.service.impl;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.core.ClientContext;
import dev.revere.valance.core.annotation.Service;
import dev.revere.valance.core.exception.ServiceException;
import dev.revere.valance.service.ISkijaService;
import dev.revere.valance.util.MinecraftUtil;
import dev.revere.valance.util.render.UIState;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.paragraph.FontCollection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

@Service(provides = ISkijaService.class, priority = 100)
public class SkijaService implements ISkijaService {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":SkijaService] ";

    private DirectContext skijaContext = null;
    private Surface skijaSurface = null;
    private BackendRenderTarget renderTarget = null;
    private FontCollection fontCollection = null;

    private boolean initialized = false;
    private int lastSurfaceWidth = -1;
    private int lastSurfaceHeight = -1;

    public SkijaService() {
        System.out.println(LOG_PREFIX + "Constructed.");
    }

    @Override
    public void setup(ClientContext context) throws ServiceException {
        System.out.println(LOG_PREFIX + "Setup phase (Context/Surface creation deferred).");

        try {
            if (fontCollection == null) {
                fontCollection = new FontCollection();
                fontCollection.setDefaultFontManager(FontMgr.getDefault());
                System.out.println(LOG_PREFIX + "FontCollection Initialized.");
            }
        } catch (Exception e) {
            System.err.println(LOG_PREFIX + "[ERROR] Failed to initialize FontCollection during setup.");
            throw new ServiceException("Failed to setup FontCollection", e);
        }
    }

    @Override
    public void initialize(ClientContext context) throws ServiceException {
        System.out.println(LOG_PREFIX + "Initializing...");
        try {
            ensureSurface();
            if (isInitialized()) {
                System.out.println(LOG_PREFIX + "Initial surface check/creation successful.");
            } else {
                System.out.println(LOG_PREFIX + "[WARN] Initial surface creation failed or deferred.");
            }
        } catch (Exception e) {
            System.err.println(LOG_PREFIX + "[ERROR] Error during initial surface check/creation: " + e.getMessage());
        }
        System.out.println(LOG_PREFIX + "Initialization complete.");
    }

    @Override
    public void shutdown(ClientContext context) throws ServiceException {
        System.out.println(LOG_PREFIX + "Shutting down...");
        try {
            if (fontCollection != null) fontCollection.close();
            if (skijaSurface != null) skijaSurface.close();
            if (renderTarget != null) renderTarget.close();
            if (skijaContext != null) skijaContext.close();
            System.out.println(LOG_PREFIX + "Skija resources closed.");
        } catch (Exception e) {
            System.err.println(LOG_PREFIX + "[ERROR] Error during Skija resource shutdown: " + e.getMessage());
        } finally {
            skijaSurface = null;
            renderTarget = null;
            skijaContext = null;
            fontCollection = null;
            initialized = false;
            lastSurfaceWidth = -1;
            lastSurfaceHeight = -1;
            System.out.println(LOG_PREFIX + "Shutdown complete.");
        }
    }

    @Override
    public DirectContext getContext() {
        if (skijaContext == null && !initialized) {
            try {
                ensureSurface();
            } catch (Exception e) {
                System.err.println(LOG_PREFIX + "[ERROR] Lazy context creation failed: " + e.getMessage());
            }
        }
        return skijaContext;
    }

    @Override
    public Surface getSurface() {
        if (skijaSurface == null || dimensionsChanged()) {
            try {
                ensureSurface();
            } catch (Exception e) {
                System.err.println(LOG_PREFIX + "[ERROR] Lazy surface creation/update failed: " + e.getMessage());
            }
        }
        return skijaSurface;
    }

    @Override
    public Canvas getCanvas() {
        Surface currentSurface = getSurface();
        return (currentSurface != null) ? currentSurface.getCanvas() : null;
    }

    @Override
    public FontCollection getFontCollection() {
        return fontCollection;
    }

    @Override
    public synchronized void ensureSurface() throws ServiceException {
        Minecraft mc = MinecraftUtil.mc();
        if (mc.getFramebuffer() == null) {
            return;
        }

        int currentWidth = mc.displayWidth;
        int currentHeight = mc.displayHeight;

        if (skijaContext == null) {
            System.out.println(LOG_PREFIX + "Creating Skija DirectContext...");
            try {
                skijaContext = DirectContext.makeGL();
                System.out.println(LOG_PREFIX + "Skija DirectContext created successfully.");
            } catch (Exception e) {
                initialized = false;
                throw new ServiceException("Failed to create Skija DirectContext", e);
            }
        }

        if (skijaSurface == null || renderTarget == null || currentWidth != lastSurfaceWidth || currentHeight != lastSurfaceHeight) {
            System.out.println(LOG_PREFIX + "Creating/Recreating Skija Surface for dimensions: " + currentWidth + "x" + currentHeight);

            if (skijaSurface != null) skijaSurface.close();
            if (renderTarget != null) renderTarget.close();

            try {
                Framebuffer buffer = mc.getFramebuffer();
                int fboId = buffer.framebufferObject;

                int samples = 0; // no multisampling
                int stencilBits = 8; // stencil buffer size


                renderTarget = BackendRenderTarget.makeGL(
                        currentWidth,
                        currentHeight,
                        samples,
                        stencilBits,
                        0,
                        FramebufferFormat.GR_GL_RGBA8
                );

                skijaSurface = Surface.makeFromBackendRenderTarget(
                        skijaContext,
                        renderTarget,
                        SurfaceOrigin.BOTTOM_LEFT,
                        SurfaceColorFormat.RGBA_8888,
                        ColorSpace.getSRGB()
                );


                if (skijaSurface == null) {
                    if (renderTarget != null) renderTarget.close();
                    renderTarget = null;
                    throw new ServiceException("Skija Surface.makeFromBackendRenderTarget() returned null.");
                }

                lastSurfaceWidth = currentWidth;
                lastSurfaceHeight = currentHeight;
                initialized = true;
                System.out.println(LOG_PREFIX + "Skija Surface created/recreated successfully.");

            } catch (Exception e) {
                System.err.println(LOG_PREFIX + "[ERROR] Failed to create/recreate Skija Surface/RenderTarget.");
                if (skijaSurface != null) {
                    skijaSurface.close();
                    skijaSurface = null;
                }
                if (renderTarget != null) {
                    renderTarget.close();
                    renderTarget = null;
                }
                initialized = false;
                lastSurfaceWidth = -1;
                lastSurfaceHeight = -1;
                throw new ServiceException("Failed to create/recreate Skija Surface", e);
            }
        }
    }

    private boolean dimensionsChanged() {
        Minecraft mc = MinecraftUtil.mc();
        return mc.displayWidth != lastSurfaceWidth || mc.displayHeight != lastSurfaceHeight;
    }

    @Override
    public void beginFrame() {
        if (!isInitialized()) return;
        UIState.backup();
        GlStateManager.clearColor(0f, 0f, 0f, 0f);
        skijaContext.resetGLAll();

        GL11.glDisable(GL11.GL_ALPHA_TEST);
    }

    @Override
    public void endFrame() {
        if (!isInitialized()) return;
        try {
            if (skijaContext != null) {
                skijaContext.flush();
            }
        } catch (Exception e) {
            System.err.println(LOG_PREFIX + "[ERROR] Exception during Skija flush: " + e.getMessage());
        } finally {
            UIState.restore();
        }
    }

    @Override
    public void runFrame(Runnable render) {
        if (!isInitialized()) {
            return;
        }

        beginFrame();
        try {
            render.run();
        } catch (Throwable t) {
            System.err.println(LOG_PREFIX + "[ERROR] Exception within Skija render task:");
            t.printStackTrace();
        } finally {
            endFrame();
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized && skijaContext != null && skijaSurface != null;
    }
}
