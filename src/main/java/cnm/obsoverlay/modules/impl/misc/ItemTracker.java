package cnm.obsoverlay.modules.impl.misc;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.EventRender;
import cnm.obsoverlay.events.impl.EventRender2D;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.utils.EntityWatcher;
import cnm.obsoverlay.utils.math.MathUtils;
import cnm.obsoverlay.utils.ProjectionUtils;
import cnm.obsoverlay.utils.SharedESPData;
import cnm.obsoverlay.utils.renderer.Fonts;
import cnm.obsoverlay.utils.vector.Vector2f;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.Entity;
import org.antlr.v4.runtime.misc.OrderedHashSet;

@ModuleInfo(
   name = "EffectTags",
   description = "Show the player's effect tags.",
   category = Category.MISC
)
public class ItemTracker extends Module {
   private final BooleanValue debug = ValueBuilder.create(this, "Debug").setDefaultBooleanValue(false).build().getBooleanValue();
   private final BooleanValue shared = ValueBuilder.create(this, "Shared").setDefaultBooleanValue(true).build().getBooleanValue();
   private final List<TargetInfo> entityPositions = new CopyOnWriteArrayList<>();

   @EventTarget
   public void update(EventRender e) {
      try {
         this.updatePositions(e.getRenderPartialTicks());
      } catch (Exception var3) {
      }
   }

   private void updatePositions(float renderPartialTicks) {
      this.entityPositions.clear();

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity != mc.player && entity instanceof AbstractClientPlayer) {
            double x = MathUtils.interpolate(renderPartialTicks, entity.xo, entity.getX());
            double y = MathUtils.interpolate(renderPartialTicks, entity.yo, entity.getY()) + (double)entity.getBbHeight();
            double z = MathUtils.interpolate(renderPartialTicks, entity.zo, entity.getZ());
            Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
            this.entityPositions
               .add(new TargetInfo((AbstractClientPlayer)entity, vector, EntityWatcher.getEntityTags((AbstractClientPlayer)entity)));
         }
      }

      if (this.shared.getCurrentValue()) {
         Map<String, SharedESPData> dataMap = EntityWatcher.getSharedESPData();

         for (SharedESPData value : dataMap.values()) {
            double x = value.getPosX();
            double y = value.getPosY() + (double)mc.player.getBbHeight();
            double z = value.getPosZ();
            Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
            this.entityPositions.add(new TargetInfo(null, vector, Set.of(value.getTags())));
         }
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      for (TargetInfo info : this.entityPositions) {
         e.getStack().pushPose();
         double y = 0.0;

         for (String entityTag : info.getDescription()) {
            Fonts.MiSans_Medium
               .render(
                  e.getStack(),
                  I18n.get(entityTag, new Object[0]),
                  (double)(info.getPosition().x + 10.0F),
                  (double)info.getPosition().y + y,
                  Color.RED,
                  true,
                  0.3F
               );
            y += Fonts.MiSans_Medium.getHeight(true, 0.3F);
         }

         if (this.debug.getCurrentValue() && info.getPlayer() != null) {
            AbstractClientPlayer player = info.getPlayer();
            OrderedHashSet<String> debugInfos = new OrderedHashSet();
            debugInfos.add("X: " + player.getX());
            debugInfos.add("Y: " + player.getY());
            debugInfos.add("Z: " + player.getZ());
            debugInfos.add("Ticks: " + player.tickCount);

            for (String debugInfo : debugInfos) {
               Fonts.MiSans_Medium.render(e.getStack(), debugInfo, (double)(info.getPosition().x + 10.0F), (double)info.getPosition().y + y, Color.RED, true, 0.35F);
               y += Fonts.MiSans_Medium.getHeight(true, 0.35F);
            }
         }

         e.getStack().popPose();
      }
   }

   private static class TargetInfo {
      AbstractClientPlayer player;
      Vector2f position;
      Set<String> description;

      public AbstractClientPlayer getPlayer() {
         return this.player;
      }

      public Vector2f getPosition() {
         return this.position;
      }

      public Set<String> getDescription() {
         return this.description;
      }

      public void setPlayer(AbstractClientPlayer player) {
         this.player = player;
      }

      public void setPosition(Vector2f position) {
         this.position = position;
      }

      public void setDescription(Set<String> description) {
         this.description = description;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof TargetInfo other)) {
            return false;
         } else if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$player = this.getPlayer();
            Object other$player = other.getPlayer();
            if (this$player == null ? other$player == null : this$player.equals(other$player)) {
               Object this$position = this.getPosition();
               Object other$position = other.getPosition();
               if (this$position == null ? other$position == null : this$position.equals(other$position)) {
                  Object this$description = this.getDescription();
                  Object other$description = other.getDescription();
                  return this$description == null ? other$description == null : this$description.equals(other$description);
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof TargetInfo;
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         Object $player = this.getPlayer();
         result = result * 59 + ($player == null ? 43 : $player.hashCode());
         Object $position = this.getPosition();
         result = result * 59 + ($position == null ? 43 : $position.hashCode());
         Object $description = this.getDescription();
         return result * 59 + ($description == null ? 43 : $description.hashCode());
      }

      @Override
      public String toString() {
         return "ItemTracker.TargetInfo(player=" + this.getPlayer() + ", position=" + this.getPosition() + ", description=" + this.getDescription() + ")";
      }

      public TargetInfo(AbstractClientPlayer player, Vector2f position, Set<String> description) {
         this.player = player;
         this.position = position;
         this.description = description;
      }
   }
}
