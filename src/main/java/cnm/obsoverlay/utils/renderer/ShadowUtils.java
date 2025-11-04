package cnm.obsoverlay.utils.renderer;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventRender2D;
import cnm.obsoverlay.events.impl.EventShader;
import cnm.obsoverlay.utils.TimeHelper;
import cnm.obsoverlay.utils.Wrapper;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;

public class ShadowUtils implements Wrapper {
   private static final TimeHelper shadowTimer = new TimeHelper();
   private static Framebuffer fbo1;
   private static Framebuffer fbo2;
   private static Framebuffer render;
   private static Shader shader;
   private static int lastWidth = 0;
   private static int lastHeight = 0;

   public static void onRenderAfterWorld(EventRender2D e, float fps) {
      Window window = mc.getWindow();
      if (shader == null) {
         shader = new Shader("shadow.vert", "shadow.frag");
         fbo1 = new Framebuffer();
         fbo2 = new Framebuffer();
         render = new Framebuffer();
      }

      // 检查窗口大小是否改变，只在改变时重建 FBO
      int currentWidth = window.getWidth();
      int currentHeight = window.getHeight();
      if (lastWidth != currentWidth || lastHeight != currentHeight) {
         render.resize();
         fbo1.resize();
         fbo2.resize();
         lastWidth = currentWidth;
         lastHeight = currentHeight;
      }

      boolean shouldFresh = false;
      if (shadowTimer.delay((double)(1000.0F / fps))) {
         shouldFresh = true;
         shadowTimer.reset();
      }

      if (shouldFresh) {
         render.bind();
         // 清空 FBO 内容为透明，防止阴影累积
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16640, false); // GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         Naven.getInstance().getEventManager().call(new EventShader(e.getStack(), EventType.SHADOW));
         render.unbind();
      }

      GL.enableBlend();
      shader.bind();
      shader.set("u_Size", (double)window.getWidth(), (double)window.getHeight());
      PostProcessRenderer.beginRender(e.getStack());
      if (shouldFresh) {
         fbo1.bind();
         GL.bindTexture(render.texture);
         shader.set("u_Direction", 1.0, 0.0);
         PostProcessRenderer.render(e.getStack());
         fbo2.bind();
         GL.bindTexture(fbo1.texture);
         shader.set("u_Direction", 0.0, 1.0);
         PostProcessRenderer.render(e.getStack());
         fbo1.bind();
         GL.bindTexture(fbo2.texture);
         shader.set("u_Direction", 1.0, 0.0);
         PostProcessRenderer.render(e.getStack());
         fbo2.unbind();
      }

      GL.bindTexture(fbo1.texture);
      shader.set("u_Direction", 0.0, 1.0);
      PostProcessRenderer.render(e.getStack());
      GL.disableBlend();
      PostProcessRenderer.endRender();
   }
}
