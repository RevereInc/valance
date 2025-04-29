package dev.revere.valance.util.render;

import org.lwjgl.opengl.*;

import java.util.HashMap;
import java.util.Map;

public class UIState {
    private static final Map<Integer, Integer> pixelStores = new HashMap<>();

    private static int lastActiveTexture;
    private static int lastProgram;
    private static int lastSampler;
    private static int lastVertexArray;
    private static int lastArrayBuffer;

    private static int lastBlendSrcRgb;
    private static int lastBlendDstRgb;
    private static int lastBlendSrcAlpha;
    private static int lastBlendDstAlpha;
    private static int lastBlendEquationRgb;
    private static int lastBlendEquationAlpha;

    private static final int[] pixelStoreParameters = {
            GL11.GL_PACK_SWAP_BYTES,
            GL11.GL_PACK_LSB_FIRST,
            GL11.GL_PACK_ROW_LENGTH,
            GL12.GL_PACK_IMAGE_HEIGHT,
            GL11.GL_PACK_SKIP_PIXELS,
            GL11.GL_PACK_SKIP_ROWS,
            GL12.GL_PACK_SKIP_IMAGES,
            GL11.GL_PACK_ALIGNMENT,
            GL11.GL_UNPACK_SWAP_BYTES,
            GL11.GL_UNPACK_LSB_FIRST,
            GL11.GL_UNPACK_ROW_LENGTH,
            GL12.GL_UNPACK_IMAGE_HEIGHT,
            GL11.GL_UNPACK_SKIP_PIXELS,
            GL11.GL_UNPACK_SKIP_ROWS,
            GL12.GL_UNPACK_SKIP_IMAGES,
            GL11.GL_UNPACK_ALIGNMENT
    };

    public static void backup() {
        GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        lastActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        lastProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        lastSampler = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        lastArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        lastVertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

        for (int param : pixelStoreParameters) {
            pixelStores.put(param, GL11.glGetInteger(param));
        }

        lastBlendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        lastBlendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        lastBlendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        lastBlendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        lastBlendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        lastBlendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
    }

    public static void restore() {
        GL11.glPopAttrib();
        GL11.glPopClientAttrib();

        GL20.glUseProgram(lastProgram);
        GL33.glBindSampler(0, lastSampler);
        GL13.glActiveTexture(lastActiveTexture);
        GL30.glBindVertexArray(lastVertexArray);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lastArrayBuffer);
        GL20.glBlendEquationSeparate(lastBlendEquationRgb, lastBlendEquationAlpha);
        GL14.glBlendFuncSeparate(lastBlendSrcRgb, lastBlendDstRgb, lastBlendSrcAlpha, lastBlendDstAlpha);

        for (int param : pixelStoreParameters) {
            Integer value = pixelStores.get(param);
            if (value != null) {
                GL11.glPixelStorei(param, value);
            }
        }
    }
}