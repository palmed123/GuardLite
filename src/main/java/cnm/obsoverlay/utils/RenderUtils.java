package cnm.obsoverlay.utils;

import cnm.obsoverlay.utils.math.MathUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Camera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class RenderUtils implements Wrapper {
    private static final AABB DEFAULT_BOX = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public static int reAlpha(int color, float alpha) {
        int col = MathUtils.clamp((int) (alpha * 255.0F), 0, 255) << 24;
        col |= MathUtils.clamp(color >> 16 & 0xFF, 0, 255) << 16;
        col |= MathUtils.clamp(color >> 8 & 0xFF, 0, 255) << 8;
        return col | MathUtils.clamp(color & 0xFF, 0, 255);
    }

    public static void drawTracer(PoseStack poseStack, float x, float y, float size, float widthDiv, float heightDiv, int color) {
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        GL11.glEnable(2848);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = poseStack.last().pose();
        float a = (float) (color >> 24 & 0xFF) / 255.0F;
        float r = (float) (color >> 16 & 0xFF) / 255.0F;
        float g = (float) (color >> 8 & 0xFF) / 255.0F;
        float b = (float) (color & 0xFF) / 255.0F;
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x - size / widthDiv, y + size, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x, y + size / heightDiv, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x + size / widthDiv, y + size, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(3042);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(2848);
    }

    public static int getRainbowOpaque(int index, float saturation, float brightness, float speed) {
        float hue = (float) ((System.currentTimeMillis() + (long) index) % (long) ((int) speed)) / speed;
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public static BlockPos getCameraBlockPos() {
        Camera camera = mc.getBlockEntityRenderDispatcher().camera;
        return camera.getBlockPosition();
    }

    public static Vec3 getCameraPos() {
        Camera camera = mc.getBlockEntityRenderDispatcher().camera;
        return camera.getPosition();
    }

    public static RegionPos getCameraRegion() {
        return RegionPos.of(getCameraBlockPos());
    }

    public static void applyRegionalRenderOffset(PoseStack matrixStack) {
        applyRegionalRenderOffset(matrixStack, getCameraRegion());
    }

    public static void applyRegionalRenderOffset(PoseStack matrixStack, RegionPos region) {
        Vec3 offset = region.toVec3().subtract(getCameraPos());
        matrixStack.translate(offset.x, offset.y, offset.z);
    }

    public static void fill(PoseStack pPoseStack, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
        innerFill(pPoseStack.last().pose(), pMinX, pMinY, pMaxX, pMaxY, pColor);
    }

    private static void innerFill(Matrix4f pMatrix, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
        if (pMinX < pMaxX) {
            float i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        if (pMinY < pMaxY) {
            float j = pMinY;
            pMinY = pMaxY;
            pMaxY = j;
        }

        float f3 = (float) (pColor >> 24 & 0xFF) / 255.0F;
        float f = (float) (pColor >> 16 & 0xFF) / 255.0F;
        float f1 = (float) (pColor >> 8 & 0xFF) / 255.0F;
        float f2 = (float) (pColor & 0xFF) / 255.0F;
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(pMatrix, pMinX, pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(pMatrix, pMaxX, pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(pMatrix, pMaxX, pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(pMatrix, pMinX, pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawRectBound(PoseStack poseStack, float x, float y, float width, float height, int color) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        float alpha = (float) (color >> 24 & 0xFF) / 255.0F;
        float red = (float) (color >> 16 & 0xFF) / 255.0F;
        float green = (float) (color >> 8 & 0xFF) / 255.0F;
        float blue = (float) (color & 0xFF) / 255.0F;
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
        tesselator.end();
    }

    private static void color(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
        float alpha = (float) (color >> 24 & 0xFF) / 255.0F;
        float red = (float) (color >> 16 & 0xFF) / 255.0F;
        float green = (float) (color >> 8 & 0xFF) / 255.0F;
        float blue = (float) (color & 0xFF) / 255.0F;
        buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    public static void drawRoundedRect(PoseStack poseStack, float x, float y, float width, float height, float edgeRadius, int color) {
        if (color == 16777215) {
            color = ARGB32.color(255, 255, 255, 255);
        }

        if (edgeRadius < 0.0F) {
            edgeRadius = 0.0F;
        }

        if (edgeRadius > width / 2.0F) {
            edgeRadius = width / 2.0F;
        }

        if (edgeRadius > height / 2.0F) {
            edgeRadius = height / 2.0F;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(1.0F);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        
        float alpha = (float) (color >> 24 & 0xFF) / 255.0F;
        float red = (float) (color >> 16 & 0xFF) / 255.0F;
        float green = (float) (color >> 8 & 0xFF) / 255.0F;
        float blue = (float) (color & 0xFF) / 255.0F;
        
        // 一次性绘制所有矩形部分
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        // 中心矩形
        buffer.vertex(matrix, x + edgeRadius, y + edgeRadius + height - edgeRadius * 2.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + edgeRadius + width - edgeRadius * 2.0F, y + edgeRadius + height - edgeRadius * 2.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + edgeRadius + width - edgeRadius * 2.0F, y + edgeRadius, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + edgeRadius, y + edgeRadius, 0.0F).color(red, green, blue, alpha).endVertex();
        
        // 顶部边
        buffer.vertex(matrix, x + edgeRadius, y + edgeRadius, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + edgeRadius + width - edgeRadius * 2.0F, y + edgeRadius, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + edgeRadius + width - edgeRadius * 2.0F, y, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + edgeRadius, y, 0.0F).color(red, green, blue, alpha).endVertex();
        
        // 底部边
        buffer.vertex(matrix, x + edgeRadius, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + edgeRadius + width - edgeRadius * 2.0F, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + edgeRadius + width - edgeRadius * 2.0F, y + height - edgeRadius, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + edgeRadius, y + height - edgeRadius, 0.0F).color(red, green, blue, alpha).endVertex();
        
        // 左边
        buffer.vertex(matrix, x, y + edgeRadius + height - edgeRadius * 2.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + edgeRadius, y + edgeRadius + height - edgeRadius * 2.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + edgeRadius, y + edgeRadius, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x, y + edgeRadius, 0.0F).color(red, green, blue, alpha).endVertex();
        
        // 右边
        buffer.vertex(matrix, x + width - edgeRadius, y + edgeRadius + height - edgeRadius * 2.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y + edgeRadius + height - edgeRadius * 2.0F, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y + edgeRadius, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width - edgeRadius, y + edgeRadius, 0.0F).color(red, green, blue, alpha).endVertex();
        
        tesselator.end();
        
        // 绘制4个圆角
        int vertices = (int) Math.min(Math.max(edgeRadius, 10.0F), 90.0F);
        
        // 左上角
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        float centerX = x + edgeRadius;
        float centerY = y + edgeRadius;
        buffer.vertex(matrix, centerX, centerY, 0.0F).color(red, green, blue, alpha).endVertex();
        for (int i = 0; i <= vertices; i++) {
            double angleRadians = (Math.PI * 2) * (double) (i + 180) / (double) (vertices * 4);
            buffer.vertex(matrix, 
                (float) ((double) centerX + Math.sin(angleRadians) * (double) edgeRadius),
                (float) ((double) centerY + Math.cos(angleRadians) * (double) edgeRadius),
                0.0F).color(red, green, blue, alpha).endVertex();
        }
        tesselator.end();

        // 右上角
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        centerX = x + width - edgeRadius;
        centerY = y + edgeRadius;
        buffer.vertex(matrix, centerX, centerY, 0.0F).color(red, green, blue, alpha).endVertex();
        for (int i = 0; i <= vertices; i++) {
            double angleRadians = (Math.PI * 2) * (double) (i + 90) / (double) (vertices * 4);
            buffer.vertex(matrix,
                (float) ((double) centerX + Math.sin(angleRadians) * (double) edgeRadius),
                (float) ((double) centerY + Math.cos(angleRadians) * (double) edgeRadius),
                0.0F).color(red, green, blue, alpha).endVertex();
        }
        tesselator.end();

        // 左下角
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        centerX = x + edgeRadius;
        centerY = y + height - edgeRadius;
        buffer.vertex(matrix, centerX, centerY, 0.0F).color(red, green, blue, alpha).endVertex();
        for (int i = 0; i <= vertices; i++) {
            double angleRadians = (Math.PI * 2) * (double) (i + 270) / (double) (vertices * 4);
            buffer.vertex(matrix,
                (float) ((double) centerX + Math.sin(angleRadians) * (double) edgeRadius),
                (float) ((double) centerY + Math.cos(angleRadians) * (double) edgeRadius),
                0.0F).color(red, green, blue, alpha).endVertex();
        }
        tesselator.end();

        // 右下角
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        centerX = x + width - edgeRadius;
        centerY = y + height - edgeRadius;
        buffer.vertex(matrix, centerX, centerY, 0.0F).color(red, green, blue, alpha).endVertex();
        for (int i = 0; i <= vertices; i++) {
            double angleRadians = (Math.PI * 2) * (double) i / (double) (vertices * 4);
            buffer.vertex(matrix,
                (float) ((double) centerX + Math.sin(angleRadians) * (double) edgeRadius),
                (float) ((double) centerY + Math.cos(angleRadians) * (double) edgeRadius),
                0.0F).color(red, green, blue, alpha).endVertex();
        }
        tesselator.end();
        
        RenderSystem.disableBlend();
    }

    public static void drawSolidBox(PoseStack matrixStack) {
        drawSolidBox(DEFAULT_BOX, matrixStack);
    }

    public static void drawSolidBox(AABB bb, PoseStack matrixStack) {
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        Matrix4f matrix = matrixStack.last().pose();
        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void drawOutlinedBox(PoseStack matrixStack) {
        drawOutlinedBox(DEFAULT_BOX, matrixStack);
    }

    public static void drawOutlinedBox(AABB bb, PoseStack matrixStack) {
        Matrix4f matrix = matrixStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void drawSolidBox(AABB bb, VertexBuffer vertexBuffer) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
        drawSolidBox(bb, bufferBuilder);
        BufferUploader.reset();
        vertexBuffer.bind();
        RenderedBuffer buffer = bufferBuilder.end();
        vertexBuffer.upload(buffer);
        VertexBuffer.unbind();
    }

    public static void drawSolidBox(AABB bb, BufferBuilder bufferBuilder) {
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
    }

    public static void drawOutlinedBox(AABB bb, VertexBuffer vertexBuffer) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
        drawOutlinedBox(bb, bufferBuilder);
        vertexBuffer.upload(bufferBuilder.end());
    }

    public static void drawOutlinedBox(AABB bb, BufferBuilder bufferBuilder) {
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
    }

    public static boolean isHovering(int mouseX, int mouseY, float xLeft, float yUp, float xRight, float yBottom) {
        return (float) mouseX > xLeft && (float) mouseX < xRight && (float) mouseY > yUp && (float) mouseY < yBottom;
    }

    public static boolean isHoveringBound(int mouseX, int mouseY, float xLeft, float yUp, float width, float height) {
        return (float) mouseX > xLeft && (float) mouseX < xLeft + width && (float) mouseY > yUp && (float) mouseY < yUp + height;
    }

    public static void fillBound(PoseStack stack, float left, float top, float width, float height, int color) {
        float right = left + width;
        float bottom = top + height;
        fill(stack, left, top, right, bottom, color);
    }

    /**
     * 绘制玩家头像
     */
    public static void drawPlayerHead(PoseStack poseStack, float x, float y, float width, float height, AbstractClientPlayer player) {
        if (mc.player == null || mc.level == null) return;

        ResourceLocation skin = mc.player.getSkinTextureLocation();

        try {
            skin = player.getSkinTextureLocation();
        } catch (Exception e) {
            // empty
        }

        int hurtTime = player.hurtTime;
        float redTint = 1.0f;

        if (hurtTime > 0) {
            float progress = (float) hurtTime / 10.0f;
            progress = Math.min(progress, 1.0f);
            redTint = lerp(progress, 0.6f, 1.0f);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, skin);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, redTint, redTint, 1.0f);

        // 使用正确的 Minecraft 1.20.1 blit 方法
        blit(poseStack, x, y, width, height, 8, 8, 8, 8, 64, 64);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    // 添加辅助方法处理 blit
    private static void blit(PoseStack poseStack, float x, float y, float width, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight) {
        float u0 = (float) uOffset / (float) textureWidth;
        float u1 = (float) (uOffset + uWidth) / (float) textureWidth;
        float v0 = (float) vOffset / (float) textureHeight;
        float v1 = (float) (vOffset + vHeight) / (float) textureHeight;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, x, y + height, 0).uv(u0, v1).endVertex();
        bufferBuilder.vertex(matrix, x + width, y + height, 0).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrix, x + width, y, 0).uv(u1, v0).endVertex();
        bufferBuilder.vertex(matrix, x, y, 0).uv(u0, v0).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    /**
     * 线性插值函数：在 start 和 end 之间根据 progress 进行插值。
     * progress = 0 时返回 end，progress = 1 时返回 start，反向渐变。
     */
    public static float lerp(float progress, float start, float end) {
        return start + (end - start) * (1.0f - progress);
    }


    public static void 装女人(BufferBuilder bufferBuilder, Matrix4f matrix, AABB box) {
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.getPosition().x());
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.getPosition().y());
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.getPosition().x());
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.getPosition().y());
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void renderGuiItem(PoseStack poseStack, ItemStack itemStack, float x, float y, BakedModel model) {
        mc.textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.pushPose();

        // 使用固定的 Z 偏移值 (100.0F) 替代 blitOffset
        poseStack.translate(x, y, 100.0F);
        poseStack.translate(8.0, 8.0, 0.0);
        poseStack.scale(1.0F, -1.0F, 1.0F);
        poseStack.scale(16.0F, 16.0F, 16.0F);
        RenderSystem.applyModelViewMatrix();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        boolean flag = !model.usesBlockLight();
        if (flag) {
            Lighting.setupForFlatItems();
        }

        // 在1.20.1中使用 ItemDisplayContext.GUI
        mc.getItemRenderer().render(itemStack, ItemDisplayContext.GUI, false, poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY, model);
        bufferSource.endBatch();
        RenderSystem.enableDepthTest();
        if (flag) {
            Lighting.setupFor3DItems();
        }

        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    public static void renderGuiItem(PoseStack poseStack, ItemStack itemStack, float x, float y) {
        renderGuiItem(poseStack, itemStack, x, y, mc.getItemRenderer().getModel(itemStack, null, null, 0));
    }
}
