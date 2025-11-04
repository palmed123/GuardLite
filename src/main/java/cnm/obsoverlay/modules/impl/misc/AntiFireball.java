package cnm.obsoverlay.modules.impl.misc;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventMotion;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.modules.impl.move.LongJump;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Fireball;

@ModuleInfo(
   name = "AntiFireball",
   description = "Prevents fireballs from damaging you",
   category = Category.MISC
)
public class AntiFireball extends Module {
   @EventTarget
   public void onMotion(EventMotion e) {
      if (!Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) {
         if (e.getType() == EventType.PRE) {
            Stream<Entity> stream = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), true);
            Optional<Fireball> fireball = stream.filter(entityx -> entityx instanceof Fireball && mc.player.distanceTo(entityx) < 6.0F)
               .map(entityx -> (Fireball)entityx)
               .findFirst();
            if (!fireball.isPresent()) {
               return;
            }

            Fireball entity = fireball.get();
            mc.gameMode.attack(mc.player, entity);
            mc.player.swing(InteractionHand.MAIN_HAND);
         }
      }
   }
}
