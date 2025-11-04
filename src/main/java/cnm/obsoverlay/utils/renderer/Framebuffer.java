package cnm.obsoverlay.utils.renderer;

import cnm.obsoverlay.utils.Wrapper;

public class Framebuffer implements Wrapper {
   private int id;
   public int texture;
   public double sizeMulti = 1.0;
   public int width;
   public int height;

   public Framebuffer(double sizeMulti) {
      this.sizeMulti = sizeMulti;
      this.init();
   }

   public Framebuffer() {
      this.init();
   }

   private void init() {
      this.id = GL.genFramebuffer();
      this.bind();
      this.texture = GL.genTexture();
      GL.bindTexture(this.texture);
      GL.defaultPixelStore();
      GL.textureParam(3553, 10242, 33071);
      GL.textureParam(3553, 10243, 33071);
      GL.textureParam(3553, 10241, 9729);
      GL.textureParam(3553, 10240, 9729);
      this.width = (int)((double)mc.getWindow().getWidth() * this.sizeMulti);
      this.height = (int)((double)mc.getWindow().getHeight() * this.sizeMulti);
      GL.textureImage2D(3553, 0, 6408, this.width, this.height, 0, 6408, 5121, null);
      GL.framebufferTexture2D(36160, 36064, 3553, this.texture, 0);
      this.unbind();
   }

   public void bind() {
      GL.bindFramebuffer(this.id);
   }

   public void setViewport() {
      GL.viewport(0, 0, this.width, this.height);
   }

   public void unbind() {
      mc.getMainRenderTarget().bindWrite(false);
   }

   public void resize() {
      GL.deleteFramebuffer(this.id);
      GL.deleteTexture(this.texture);
      this.init();
   }
}
