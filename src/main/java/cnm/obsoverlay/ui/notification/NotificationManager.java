package cnm.obsoverlay.ui.notification;

import cnm.obsoverlay.events.impl.EventRender2D;
import cnm.obsoverlay.events.impl.EventShader;
import cnm.obsoverlay.utils.SmoothAnimationTimer;
import cnm.obsoverlay.utils.Wrapper;
import com.mojang.blaze3d.platform.Window;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager implements Wrapper {
   private final List<Notification> notifications = new CopyOnWriteArrayList<>();

   public void addNotification(Notification notification) {
      if (!this.notifications.contains(notification)) {
         this.notifications.add(notification);
      }
   }

   public void onRenderShadow(EventShader e) {
      for (Notification notification : this.notifications) {
         SmoothAnimationTimer widthTimer = notification.getWidthTimer();
         SmoothAnimationTimer heightTimer = notification.getHeightTimer();
         Window window = mc.getWindow();
         notification.renderShader(
            e.getStack(), (float)window.getGuiScaledWidth() - widthTimer.value + 2.0F, (float)window.getGuiScaledHeight() - 30.0f - heightTimer.value
         );
      }
   }

   public void onRender(EventRender2D e) {
      float height = 5.0F;

      for (Notification notification : this.notifications) {
         e.getStack().pushPose();
         float width = notification.getWidth();
         height += notification.getHeight();
         SmoothAnimationTimer widthTimer = notification.getWidthTimer();
         SmoothAnimationTimer heightTimer = notification.getHeightTimer();
         float lifeTime = (float)(System.currentTimeMillis() - notification.getCreateTime());
         if (lifeTime > (float)notification.getMaxAge()) {
            widthTimer.target = 0.0F;
            heightTimer.target = 0.0F;
            if (widthTimer.isAnimationDone(true)) {
               this.notifications.remove(notification);
            }
         } else {
            widthTimer.target = width;
            heightTimer.target = height;
         }

         widthTimer.update(true);
         heightTimer.update(true);
         Window window = mc.getWindow();
         notification.render(e.getStack(), (float)window.getGuiScaledWidth() - widthTimer.value + 2.0F, (float)window.getGuiScaledHeight() - 30.0f - heightTimer.value);
         e.getStack().popPose();
      }
   }
}
