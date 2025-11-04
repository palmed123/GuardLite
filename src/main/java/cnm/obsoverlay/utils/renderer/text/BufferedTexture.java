package cnm.obsoverlay.utils.renderer.text;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30C;

public class BufferedTexture extends AbstractTexture {
   public BufferedTexture(int width, int height, byte[] data, Format format, Filter filterMin, Filter filterMag) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> this.upload(width, height, data, format, filterMin, filterMag));
      } else {
         this.upload(width, height, data, format, filterMin, filterMag);
      }
   }

   public BufferedTexture(
      int width, int height, ByteBuffer buffer, Format format, Filter filterMin, Filter filterMag
   ) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> this.upload(width, height, buffer, format, filterMin, filterMag));
      } else {
         this.upload(width, height, buffer, format, filterMin, filterMag);
      }
   }

   private void upload(int width, int height, byte[] data, Format format, Filter filterMin, Filter filterMag) {
      ByteBuffer buffer = BufferUtils.createByteBuffer(data.length).put(data);
      this.upload(width, height, buffer, format, filterMin, filterMag);
   }

   private void upload(
      int width, int height, ByteBuffer buffer, Format format, Filter filterMin, Filter filterMag
   ) {
      this.bind();
      GL30C.glPixelStorei(3312, 0);
      GL30C.glPixelStorei(3313, 0);
      GL30C.glPixelStorei(3314, 0);
      GL30C.glPixelStorei(32878, 0);
      GL30C.glPixelStorei(3315, 0);
      GL30C.glPixelStorei(3316, 0);
      GL30C.glPixelStorei(32877, 0);
      GL30C.glPixelStorei(3317, 4);
      GL30C.glTexParameteri(3553, 10242, 10497);
      GL30C.glTexParameteri(3553, 10243, 10497);
      GL30C.glTexParameteri(3553, 10241, filterMin.toOpenGL());
      GL30C.glTexParameteri(3553, 10240, filterMag.toOpenGL());
      ((Buffer)buffer).rewind();
      GL30C.glTexImage2D(3553, 0, format.toOpenGL(), width, height, 0, format.toOpenGL(), 5121, buffer);
   }

   public void load(ResourceManager manager) throws IOException {
   }

   public static enum Filter {
      Nearest,
      Linear;

      public int toOpenGL() {
         return this == Nearest ? 9728 : 9729;
      }
   }

   public static enum Format {
      A,
      RGB,
      RGBA;

      public int toOpenGL() {
         if (this == A) {
            return 6403;
         } else if (this == RGB) {
            return 6407;
         } else {
            return this == RGBA ? 6408 : 0;
         }
      }
   }
}
