package cnm.obsoverlay.modules.impl.misc;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.ui.notification.Notification;
import cnm.obsoverlay.ui.notification.NotificationLevel;
import cnm.obsoverlay.utils.TimeHelper;

@ModuleInfo(
   name = "ClientFriend",
   description = "Treat other users as friend!",
   category = Category.MISC
)
public class ClientFriend extends Module {
   public static TimeHelper attackTimer = new TimeHelper();

   @Override
   public void onDisable() {
      attackTimer.reset();
      Notification notification = new Notification(NotificationLevel.INFO, "You can attack other players after 15 seconds.", 15000L);
      Naven.getInstance().getNotificationManager().addNotification(notification);
   }
}
