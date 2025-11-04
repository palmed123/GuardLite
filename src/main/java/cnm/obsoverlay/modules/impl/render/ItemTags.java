package cnm.obsoverlay.modules.impl.render;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.EventRender;
import cnm.obsoverlay.events.impl.EventRender2D;
import cnm.obsoverlay.events.impl.EventShader;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.modules.impl.misc.ChestStealer;
import cnm.obsoverlay.utils.InventoryUtils;
import cnm.obsoverlay.utils.math.MathUtils;
import cnm.obsoverlay.utils.ProjectionUtils;
import cnm.obsoverlay.utils.RenderUtils;
import cnm.obsoverlay.utils.renderer.Fonts;
import cnm.obsoverlay.utils.renderer.text.CustomTextRenderer;
import cnm.obsoverlay.utils.vector.Vector2f;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import cnm.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnchantedGoldenAppleItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import org.joml.Vector4f;

@ModuleInfo(
   name = "ItemTags",
   description = "Show item tags.",
   category = Category.RENDER
)
public class ItemTags extends Module {
   private final ConcurrentHashMap<ItemEntity, Vector2f> entityPositions = new ConcurrentHashMap<>();
   private final List<Vector4f> blurMatrices = new ArrayList<>();
   public FloatValue scale = ValueBuilder.create(this, "Scale")
      .setDefaultFloatValue(0.25F)
      .setFloatStep(0.01F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(0.5F)
      .build()
      .getFloatValue();
   BooleanValue allItems = ValueBuilder.create(this, "All Items").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue godItems = ValueBuilder.create(this, "God Items")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue diamond = ValueBuilder.create(this, "Diamond")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue gold = ValueBuilder.create(this, "Gold")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue iron = ValueBuilder.create(this, "Iron")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue enderPearl = ValueBuilder.create(this, "Ender Pearl")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue goldenApple = ValueBuilder.create(this, "Golden Apple")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue usefulItem = ValueBuilder.create(this, "Useful Item")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();

   private static String getDisplayName(ItemEntity ent) {
      ItemStack item = ent.getItem();
      return item.getDisplayName().getString() + " * " + item.getCount();
   }

   private boolean isValidItem(ItemStack stack) {
      if (stack == null) {
         return false;
      } else if (stack.isEmpty()) {
         return false;
      } else if (this.allItems.getCurrentValue()) {
         return true;
      } else {
         if (this.godItems.getCurrentValue()) {
            if (InventoryUtils.isKBBall(stack)) {
               return true;
            }

            if (stack.getItem() instanceof EnchantedGoldenAppleItem) {
               return true;
            }

            if (InventoryUtils.isGodAxe(stack)) {
               return true;
            }
         }

         if (this.diamond.getCurrentValue() && stack.getItem() == Items.DIAMOND) {
            return true;
         } else if (this.gold.getCurrentValue() && stack.getItem() == Items.GOLD_INGOT) {
            return true;
         } else if (this.iron.getCurrentValue() && stack.getItem() == Items.IRON_INGOT) {
            return true;
         } else if (this.enderPearl.getCurrentValue() && stack.getItem() == Items.ENDER_PEARL) {
            return true;
         } else if (this.goldenApple.getCurrentValue() && stack.getItem() == Items.GOLDEN_APPLE) {
            return true;
         } else {
            if (this.usefulItem.getCurrentValue()) {
               if (stack.getItem() instanceof BlockItem && stack.getCount() < 8) {
                  return false;
               }

               if ((stack.getItem() instanceof SnowballItem || stack.getItem() instanceof EggItem) && stack.getCount() < 3) {
                  return false;
               }

               if (ChestStealer.isItemUseful(stack)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private boolean isGodItem(ItemStack stack) {
      if (InventoryUtils.isKBBall(stack)) {
         return true;
      } else if (stack.getItem() instanceof EnchantedGoldenAppleItem) {
         return true;
      } else {
         return stack.getItem() instanceof EndCrystalItem ? true : InventoryUtils.isGodAxe(stack);
      }
   }

   private void updatePositions(float renderPartialTicks) {
      this.entityPositions.clear();

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof ItemEntity) {
            ItemEntity itemEntity = (ItemEntity)entity;
            if (this.isValidItem(itemEntity.getItem())) {
               double x = MathUtils.interpolate(renderPartialTicks, entity.xo, entity.getX());
               double y = MathUtils.interpolate(renderPartialTicks, entity.yo, entity.getY()) + (double)entity.getBbHeight() + 0.5;
               double z = MathUtils.interpolate(renderPartialTicks, entity.zo, entity.getZ());
               Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
               vector.setY(vector.getY() - 2.0F);
               this.entityPositions.put(itemEntity, vector);
            }
         }
      }
   }

   @EventTarget
   public void update(EventRender event) {
      try {
         this.updatePositions(event.getRenderPartialTicks());
      } catch (Exception var3) {
      }
   }

   @EventTarget
   public void onShader(EventShader e) {
      for (Vector4f blurMatrix : this.blurMatrices) {
         RenderUtils.fill(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), HUD.bodyColor);
      }
   }

   @EventTarget
   public void on2DRender(EventRender2D e) {
      try {
         PoseStack stack = e.getStack();
         this.blurMatrices.clear();

         for (ItemEntity ent : this.entityPositions.keySet()) {
            if (ent != null) {
               Vector2f renderPositions = this.entityPositions.get(ent);
               stack.pushPose();
               CustomTextRenderer MiSans_Medium = Fonts.MiSans_Medium;
               String str = getDisplayName(ent);
               float allWidth = MiSans_Medium.getWidth(str, (double)this.scale.getCurrentValue()) + 8.0F;
               this.blurMatrices
                  .add(new Vector4f(renderPositions.x - allWidth / 2.0F, renderPositions.y - 14.0F, renderPositions.x + allWidth / 2.0F, renderPositions.y));
               if (this.isGodItem(ent.getItem())) {
                  MiSans_Medium.render(
                     stack,
                     str,
                     (double)(renderPositions.x - allWidth / 2.0F + 4.0F),
                     (double)(renderPositions.y - 12.0F),
                     Color.RED,
                     true,
                     (double)this.scale.getCurrentValue()
                  );
               } else {
                  MiSans_Medium.render(
                     stack,
                     str,
                     (double)(renderPositions.x - allWidth / 2.0F + 4.0F),
                     (double)(renderPositions.y - 12.0F),
                     Color.WHITE,
                     true,
                     (double)this.scale.getCurrentValue()
                  );
               }

               stack.popPose();
            }
         }
      } catch (Exception var9) {
      }
   }
}
