package cnm.obsoverlay.modules.impl.move;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventMotion;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;

@ModuleInfo(
   name = "Sprint",
   description = "Automatically sprints",
   category = Category.MOVEMENT
)
public class Sprint extends Module {
   @EventTarget(0)
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         mc.options.keySprint.setDown(true);
         mc.options.toggleSprint().set(false);
      }
   }

   @Override
   public void onDisable() {
      mc.options.keySprint.setDown(false);
   }
}
