package cnm.obsoverlay.utils.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

public class PostProcessRenderer {
   private static Mesh mesh;

   public static void init() {
      mesh = new Mesh(DrawMode.Triangles, Mesh.Attrib.Vec2);
      mesh.begin();
      mesh.quad(mesh.vec2(-1.0, -1.0).next(), mesh.vec2(-1.0, 1.0).next(), mesh.vec2(1.0, 1.0).next(), mesh.vec2(1.0, -1.0).next());
      mesh.end();
   }

   public static void beginRender(PoseStack stack) {
      mesh.beginRender(stack);
   }

   public static void render(PoseStack stack) {
      mesh.render(stack);
   }

   public static void endRender() {
      mesh.endRender();
   }
}
