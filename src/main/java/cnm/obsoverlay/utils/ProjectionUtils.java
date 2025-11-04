package cnm.obsoverlay.utils;

import cnm.obsoverlay.utils.vector.Vector2f;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ProjectionUtils implements Wrapper {

   public static Vector2f project(double x, double y, double z, float renderPartialTicks) {
      Vec3 camera_pos = mc.getEntityRenderDispatcher().camera.getPosition();
      Quaternionf cameraRotation = new Quaternionf(mc.getEntityRenderDispatcher().cameraOrientation());
      cameraRotation.conjugate();
      Vector3f result3f = new Vector3f((float)(camera_pos.x - x), (float)(camera_pos.y - y), (float)(camera_pos.z - z));
      result3f.rotate(cameraRotation);
      if ((Boolean)mc.options.bobView().get() && mc.getCameraEntity() instanceof Player playerentity) {
         calculateViewBobbing(playerentity, result3f, renderPartialTicks);
      }

//      double fov = ((GameRendererAccessor)mc.gameRenderer).invokeGetFov(mc.getEntityRenderDispatcher().camera, renderPartialTicks, true);
       double fov = 0;
       try {
           fov = (double) ReflectUtil.getMethod(GameRenderer.class, "getFov", "(Lnet/minecraft/client/Camera;FZ)D", Camera.class, float.class, boolean.class).invoke(mc.gameRenderer, mc.getEntityRenderDispatcher().camera, renderPartialTicks, true);
       } catch (Exception e) {
           throw new RuntimeException(e);
       }
       return calculateScreenPosition(result3f, fov);
   }

   private static void calculateViewBobbing(Player playerentity, Vector3f result3f, float renderPartialTicks) {
      float walked = playerentity.walkDist;
      float f = walked - playerentity.walkDistO;
      float f1 = -(walked + f * renderPartialTicks);
      float f2 = Mth.lerp(renderPartialTicks, playerentity.oBob, playerentity.bob);
      Quaternionf quaternion = new Quaternionf().rotationX(Math.abs(Mth.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F * (float) (Math.PI / 180.0));
      quaternion.conjugate();
      result3f.rotate(quaternion);
      Quaternionf quaternion1 = new Quaternionf().rotationZ(Mth.sin(f1 * (float) Math.PI) * f2 * 3.0F * (float) (Math.PI / 180.0));
      quaternion1.conjugate();
      result3f.rotate(quaternion1);
      Vector3f bobTranslation = new Vector3f(Mth.sin(f1 * (float) Math.PI) * f2 * 0.5F, -Math.abs(Mth.cos(f1 * (float) Math.PI) * f2), 0.0F);
      bobTranslation.y = -bobTranslation.y;
      result3f.add(bobTranslation);
   }

   private static Vector2f calculateScreenPosition(Vector3f result3f, double fov) {
      float halfHeight = (float)mc.getWindow().getGuiScaledHeight() / 2.0F;
      float scaleFactor = halfHeight / (result3f.z() * (float)Math.tan(Math.toRadians(fov / 2.0)));
      return result3f.z() < 0.0F
         ? new Vector2f(
            -result3f.x() * scaleFactor + (float)mc.getWindow().getGuiScaledWidth() / 2.0F,
            (float)mc.getWindow().getGuiScaledHeight() / 2.0F - result3f.y() * scaleFactor
         )
         : new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);
   }
}
