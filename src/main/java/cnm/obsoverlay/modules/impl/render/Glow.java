package cnm.obsoverlay.modules.impl.render;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;

@ModuleInfo(
   name = "Glow",
   description = "Glow effect for entities",
   category = Category.RENDER
)
public class Glow extends Module {
   BooleanValue players = ValueBuilder.create(this, "Player").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue items = ValueBuilder.create(this, "Items").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue mobs = ValueBuilder.create(this, "Mobs").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue animals = ValueBuilder.create(this, "Animals").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue arrows = ValueBuilder.create(this, "Arrows").setDefaultBooleanValue(false).build().getBooleanValue();

   public static boolean shouldGlow(Entity entity) {
      Glow module = (Glow)Naven.getInstance().getModuleManager().getModule(Glow.class);
      if (!module.isEnabled()) {
         return false;
      } else if (entity instanceof Player && module.players.getCurrentValue()) {
         return true;
      } else if (entity instanceof ItemEntity && module.items.getCurrentValue()) {
         return true;
      } else if (entity instanceof Mob && module.mobs.getCurrentValue()) {
         return true;
      } else {
         return entity instanceof Animal && module.animals.getCurrentValue() ? true : entity instanceof Arrow && module.arrows.getCurrentValue();
      }
   }
}
