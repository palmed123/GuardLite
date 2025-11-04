package cnm.obsoverlay.modules.impl.move;

import cn.paradisemc.Native;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventMotion;
import cnm.obsoverlay.events.impl.EventRespawn;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.utils.ReflectUtil;
import cnm.obsoverlay.utils.auth.AuthUtils;
import net.minecraft.world.entity.LivingEntity;

@ModuleInfo(
   name = "NoJumpDelay",
   description = "Removes the delay when jumping",
   category = Category.MOVEMENT
)
public class NoJumpDelay extends Module {
   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         ReflectUtil.setFieldValue(LivingEntity.class, "noJumpDelay", mc.player, 0);
      }
   }
}
