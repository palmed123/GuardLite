package cnm.obsoverlay.utils;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

public class StencilUtils implements Wrapper {
   private static boolean stencilInitialized = false;
   private static int lastWidth = 0;
   private static int lastHeight = 0;

   public static void write(boolean renderClipLayer) {
      checkAndSetupFBO();
      GL11.glClear(1024);
      GL11.glEnable(2960);
      GL11.glStencilFunc(519, 1, 65535);
      GL11.glStencilOp(7680, 7680, 7681);
      if (!renderClipLayer) {
         RenderSystem.colorMask(false, false, false, false);
      }
   }

   public static void erase(boolean invert) {
      RenderSystem.colorMask(true, true, true, true);
      GL11.glStencilFunc(invert ? 514 : 517, 1, 65535);
      GL11.glStencilOp(7680, 7680, 7681);
   }

   public static void dispose() {
      GL11.glDisable(2960);
   }

   private static void checkAndSetupFBO() {
      int currentWidth = mc.getWindow().getWidth();
      int currentHeight = mc.getWindow().getHeight();
      
      // 只在窗口大小改变或首次初始化时重建
      if (!stencilInitialized || lastWidth != currentWidth || lastHeight != currentHeight) {
         setupFBO();
         stencilInitialized = true;
         lastWidth = currentWidth;
         lastHeight = currentHeight;
      }
   }

   public static void setupFBO() {
      if (mc.getMainRenderTarget().getDepthTextureId() > -1) {
         setupFBO(mc.getMainRenderTarget());
//         ((RenderTargetAccessor)mc.getMainRenderTarget()).setDepthBufferId(-1);
         ReflectUtil.setFieldValue(RenderTarget.class, "depthBufferId", mc.getMainRenderTarget(), -1);
      }
   }

   public static void setupFBO(RenderTarget fbo) {
      EXTFramebufferObject.glDeleteRenderbuffersEXT(fbo.getDepthTextureId());
      int stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();
      EXTFramebufferObject.glBindRenderbufferEXT(36161, stencilDepthBufferID);
      EXTFramebufferObject.glRenderbufferStorageEXT(36161, 34041, mc.getWindow().getWidth(), mc.getWindow().getHeight());
      EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36128, 36161, stencilDepthBufferID);
      EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36096, 36161, stencilDepthBufferID);
   }
   
   // 在窗口调整大小时重置标志
   public static void invalidate() {
      stencilInitialized = false;
   }
}
