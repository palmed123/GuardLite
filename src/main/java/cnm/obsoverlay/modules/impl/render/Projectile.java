package cnm.obsoverlay.modules.impl.render;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.EventRender;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.modules.impl.render.projectiles.ProjectileData;
import cnm.obsoverlay.modules.impl.render.projectiles.datas.BasicProjectileData;
import cnm.obsoverlay.modules.impl.render.projectiles.datas.EntityArrowData;
import cnm.obsoverlay.modules.impl.render.projectiles.datas.EntityPotionData;
import cnm.obsoverlay.utils.RayTraceUtils;
import cnm.obsoverlay.utils.RenderUtils;
import cnm.obsoverlay.utils.rotation.RotationManager;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
   name = "Projectiles",
   description = "Renders projectiles",
   category = Category.RENDER
)
public class Projectile extends Module {
   private final EntityArrowData arrowsColor = new EntityArrowData();
   private final EntityPotionData potionsColor = new EntityPotionData();
   private final BasicProjectileData enderPearlColor = new BasicProjectileData(Collections.singleton(ThrownEnderpearl.class), new Color(173, 12, 255));
   private final BasicProjectileData eggColor = new BasicProjectileData(Collections.singleton(ThrownEgg.class), new Color(255, 238, 154));
   private final BasicProjectileData snowballColor = new BasicProjectileData(Collections.singleton(Snowball.class), new Color(255, 255, 255));
   public BooleanValue showArrows = ValueBuilder.create(this, "Show Arrows").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue showPearls = ValueBuilder.create(this, "Show Pearls").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue showPotions = ValueBuilder.create(this, "Show Potions").setDefaultBooleanValue(false).build().getBooleanValue();
   public BooleanValue showEggs = ValueBuilder.create(this, "Show Eggs").setDefaultBooleanValue(false).build().getBooleanValue();
   public BooleanValue showSnowballs = ValueBuilder.create(this, "Show Snowballs").setDefaultBooleanValue(false).build().getBooleanValue();

   @EventTarget
   public void onRender3D(EventRender event) {
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof net.minecraft.world.entity.projectile.Projectile) {
            ProjectileData var8 = this.getProjectileDataByEntity(entity);
            if (var8 != null) {
               PoseStack stack = event.getPMatrixStack();
               stack.pushPose();
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 771);
               GL11.glDisable(2929);
               GL11.glDepthMask(false);
               GL11.glEnable(2848);
               RenderSystem.setShader(GameRenderer::getPositionShader);
               Color color = var8.getColor(entity);
               RenderSystem.setShaderColor((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, 1.0F);
               this.render(stack, entity, var8);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               GL11.glDisable(3042);
               GL11.glEnable(2929);
               GL11.glDepthMask(true);
               GL11.glDisable(2848);
               stack.popPose();
            }
         }
      }
   }

   @EventTarget
   private void onRender(EventRender event) {
      Path pathResult = this.getPath(event.getRenderPartialTicks());
      if (pathResult != null) {
         List<Vec3> path = pathResult.getPath();
         if (path.size() >= 2) {
            PoseStack stack = event.getPMatrixStack();
            stack.pushPose();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glEnable(2848);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            Vec3 camPos = path.get(0);
            this.drawLine(stack, path, camPos);
            if (!path.isEmpty()) {
               Vec3 end = path.get(path.size() - 1);
               this.drawEndOfLine(stack, end, camPos, pathResult.result, event.getRenderPartialTicks());
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDisable(3042);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(2848);
            stack.popPose();
         }
      }
   }

   private void drawLine(PoseStack matrixStack, List<Vec3> path, Vec3 camPos) {
      Matrix4f matrix = matrixStack.last().pose();
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      bufferBuilder.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION);
      float[] colorF = new float[]{1.0F, 1.0F, 1.0F};
      RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 1.0F);

      for (Vec3 point : path) {
         bufferBuilder.vertex(matrix, (float)(point.x - camPos.x), (float)(point.y - camPos.y), (float)(point.z - camPos.z)).endVertex();
      }

      BufferUploader.drawWithShader(bufferBuilder.end());
   }

   private void drawEndOfLine(PoseStack matrixStack, Vec3 end, Vec3 camPos, HitResult result, float partialTicks) {
      AABB bb = new AABB(0.15, 0.15, 0.15, 0.35, 0.35, 0.35);
      float[] colorF = new float[]{1.0F, 1.0F, 1.0F};
      if (result != null) {
         if (result.getType() == Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult)result;
            Direction direction = blockHitResult.getDirection();
            if (direction == Direction.SOUTH) {
               bb = new AABB(0.0, 0.0, 0.0, 0.5, 0.5, 0.1);
            } else if (direction == Direction.NORTH) {
               bb = new AABB(0.0, 0.0, 0.4, 0.5, 0.5, 0.5);
            } else if (direction == Direction.EAST) {
               bb = new AABB(0.0, 0.0, 0.0, 0.1, 0.5, 0.5);
            } else if (direction == Direction.WEST) {
               bb = new AABB(0.4, 0.0, 0.0, 0.5, 0.5, 0.5);
            } else if (direction == Direction.UP) {
               colorF = new float[]{0.0F, 1.0F, 0.0F};
               bb = new AABB(0.0, 0.0, 0.0, 0.5, 0.1, 0.5);
            } else if (direction == Direction.DOWN) {
               bb = new AABB(0.0, 0.4, 0.0, 0.5, 0.5, 0.5);
            }
         } else if (result.getType() == Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult)result;
            colorF = new float[]{1.0F, 0.0F, 0.0F};
            RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.5F);
            Entity entity = entityHitResult.getEntity();
            double motionX = entity.getX() - entity.xo;
            double motionY = entity.getY() - entity.yo;
            double motionZ = entity.getZ() - entity.zo;
            Vec3 cameraPos = RenderUtils.getCameraPos();
            AABB move = entity.getBoundingBox()
               .move(-cameraPos.x, -cameraPos.y, -cameraPos.z)
               .move(-motionX, -motionY, -motionZ)
               .move((double)partialTicks * motionX, (double)partialTicks * motionY, (double)partialTicks * motionZ)
               .inflate(0.1);
            RenderUtils.drawSolidBox(move, matrixStack);
         }
      }

      double renderX = end.x - camPos.x;
      double renderY = end.y - camPos.y;
      double renderZ = end.z - camPos.z;
      matrixStack.pushPose();
      matrixStack.translate(renderX - 0.25, renderY - 0.25, renderZ - 0.25);
      RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.25F);
      RenderUtils.drawSolidBox(bb, matrixStack);
      RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.75F);
      RenderUtils.drawOutlinedBox(bb, matrixStack);
      matrixStack.popPose();
   }

   private Path getPath(float partialTicks) {
      Player player = mc.player;
      ArrayList<Vec3> path = new ArrayList<>();
      ItemStack stack = player.getMainHandItem();
      Item item = stack.getItem();
      if (!stack.isEmpty() && this.isThrowable(item)) {
         double arrowPosX = player.xOld + (player.getX() - player.xOld) * (double)partialTicks;
         double arrowPosY = player.yOld + (player.getY() - player.yOld) * (double)partialTicks + (double)player.getEyeHeight() - 0.1;
         double arrowPosZ = player.zOld + (player.getZ() - player.zOld) * (double)partialTicks;
         double arrowMotionFactor = item instanceof ProjectileWeaponItem ? 1.0 : 0.4;
         double yaw;
         double pitch;
         if (RotationManager.active) {
            if (RotationManager.lastAnimationRotation == null || RotationManager.animationRotation == null) {
               return new Path(path, null);
            }

            yaw = Math.toRadians((double)Mth.lerp(partialTicks, RotationManager.lastAnimationRotation.x, RotationManager.animationRotation.x));
            pitch = Math.toRadians((double)Mth.lerp(partialTicks, RotationManager.lastAnimationRotation.y, RotationManager.animationRotation.y));
         } else {
            yaw = Math.toRadians((double)Mth.lerp(partialTicks, player.yRotO, player.getYRot()));
            pitch = Math.toRadians((double)Mth.lerp(partialTicks, player.xRotO, player.getXRot()));
         }

         double arrowMotionX = -Math.sin(yaw) * Math.cos(pitch) * arrowMotionFactor;
         double arrowMotionY = -Math.sin(pitch) * arrowMotionFactor;
         double arrowMotionZ = Math.cos(yaw) * Math.cos(pitch) * arrowMotionFactor;
         double arrowMotion = Math.sqrt(arrowMotionX * arrowMotionX + arrowMotionY * arrowMotionY + arrowMotionZ * arrowMotionZ);
         arrowMotionX /= arrowMotion;
         arrowMotionY /= arrowMotion;
         arrowMotionZ /= arrowMotion;
         if (item instanceof ProjectileWeaponItem) {
            float bowPower = (float)(72000 - player.getUseItemRemainingTicks()) / 20.0F;
            bowPower = (bowPower * bowPower + bowPower * 2.0F) / 3.0F;
            if (bowPower > 1.0F || bowPower <= 0.1F) {
               bowPower = 1.0F;
            }

            bowPower *= 3.0F;
            arrowMotionX *= (double)bowPower;
            arrowMotionY *= (double)bowPower;
            arrowMotionZ *= (double)bowPower;
         } else {
            arrowMotionX *= 1.5;
            arrowMotionY *= 1.5;
            arrowMotionZ *= 1.5;
         }

         double gravity = this.getProjectileGravity(item);

         for (int i = 0; i < 1000; i++) {
            Vec3 arrowPos = new Vec3(arrowPosX, arrowPosY, arrowPosZ);
            Vec3 postArrowPos = new Vec3(arrowPosX + arrowMotionX, arrowPosY + arrowMotionY, arrowPosZ + arrowMotionZ);
            path.add(arrowPos);
            ClipContext context = new ClipContext(arrowPos, postArrowPos, Block.COLLIDER, Fluid.NONE, mc.player);
            BlockHitResult clip = mc.level.clip(context);
            if (clip.getType() != Type.MISS) {
               return new Path(path, clip);
            }

            Arrow fakeArrow = new Arrow(mc.level, arrowPosX, arrowPosY, arrowPosZ);
            EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
               mc.level,
               fakeArrow,
               arrowPos,
               postArrowPos,
               fakeArrow.getBoundingBox().expandTowards(new Vec3(arrowMotionX, arrowMotionY, arrowMotionZ)).inflate(1.0),
               entity -> entity != player && entity instanceof LivingEntity
            );
            if (entityHitResult != null && entityHitResult.getType() == Type.ENTITY) {
               return new Path(path, entityHitResult);
            }

            arrowPosX += arrowMotionX;
            arrowPosY += arrowMotionY;
            arrowPosZ += arrowMotionZ;
            arrowMotionX *= 0.99;
            arrowMotionY *= 0.99;
            arrowMotionZ *= 0.99;
            arrowMotionY -= gravity;
         }

         return new Path(path, null);
      } else {
         return null;
      }
   }

   private double getProjectileGravity(Item item) {
      if (item instanceof BowItem || item instanceof CrossbowItem) {
         return 0.05;
      } else if (item instanceof PotionItem) {
         return 0.4;
      } else if (item instanceof FishingRodItem) {
         return 0.15;
      } else {
         return item instanceof TridentItem ? 0.015 : 0.03;
      }
   }

   private boolean isThrowable(Item item) {
      return item instanceof BowItem
         || item instanceof CrossbowItem
         || item instanceof SnowballItem
         || item instanceof EggItem
         || item instanceof EnderpearlItem
         || item instanceof SplashPotionItem
         || item instanceof LingeringPotionItem
         || item instanceof FishingRodItem
         || item instanceof TridentItem;
   }

   private void render(PoseStack matrix, Entity entity, ProjectileData projectileInfo) {
      if (entity != null) {
         LocalPlayer thePlayer = mc.player;
         ClientLevel theWorld = mc.level;
         Color color = projectileInfo.getColor(entity);
         if (color == null) {
            color = new Color(255, 255, 255);
         }

         Tesselator tesselator = Tesselator.getInstance();
         BufferBuilder builder = tesselator.getBuilder();
         builder.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
         double posX = entity.getX();
         double posY = entity.getY();
         double posZ = entity.getZ();
         double motionX = entity.getDeltaMovement().x;
         double motionY = entity.getDeltaMovement().y;
         double motionZ = entity.getDeltaMovement().z;
         this.drawVertex(color, builder, matrix, posX, posY, posZ);

         while (true) {
            float data1 = projectileInfo.getData1();
            float data2 = projectileInfo.getData2();
            AABB aabb = new AABB(posX - (double)data1, posY, posZ - (double)data1, posX + (double)data1, posY + (double)data2, posZ + (double)data1);
            Vec3 vec3 = new Vec3(posX, posY, posZ);
            Vec3 vec3WithMotion = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
            HitResult movingObj = RayTraceUtils.rayTraceBlocks(vec3, vec3WithMotion, false, entity instanceof Arrow, false, entity);
            if (!movingObj.getType().equals(Type.MISS)) {
               vec3WithMotion = new Vec3(movingObj.getLocation().x(), movingObj.getLocation().y(), movingObj.getLocation().z());
            }

            List<Entity> getByAABBEntitys = theWorld.getEntities(thePlayer, aabb.contract(motionX, motionY, motionZ).expandTowards(1.0, 1.0, 1.0));
            double lastMinDistance = 0.0;

            for (Entity aabbEntity : getByAABBEntitys) {
               if (aabbEntity instanceof LivingEntity && !(aabbEntity instanceof EnderMan) && aabbEntity.canBeCollidedWith() && !aabbEntity.equals(thePlayer)) {
                  aabb = aabbEntity.getBoundingBox().expandTowards(0.3, 0.3, 0.3);
                  EntityHitResult aabbMovingObj = RayTraceUtils.calculateIntercept(aabb, vec3, vec3WithMotion);
                  if (aabbMovingObj != null) {
                     double distance = vec3.distanceTo(aabbMovingObj.getLocation());
                     if (distance < lastMinDistance || lastMinDistance == 0.0) {
                        lastMinDistance = distance;
                        movingObj = aabbMovingObj;
                     }
                  }
               }
            }

            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            if (!movingObj.getType().equals(Type.MISS)) {
               posX = movingObj.getLocation().x();
               posY = movingObj.getLocation().y();
               posZ = movingObj.getLocation().z();
               break;
            }

            if (posY < -128.0) {
               break;
            }

            motionX *= entity.isInWater() ? 0.8 : 0.99;
            double var39 = motionY * (entity.isInWater() ? 0.8 : 0.99);
            motionZ *= entity.isInWater() ? 0.8 : 0.99;
            motionY = var39 - (double)projectileInfo.getGravity();
            this.drawVertex(color, builder, matrix, posX + motionX, posY + motionY, posZ + motionZ);
         }

         tesselator.end();
      }
   }

   private void drawVertex(Color color, BufferBuilder builder, PoseStack stack, double x, double y, double z) {
      Entity entity = mc.getCameraEntity();
      double d0 = entity.xOld + (entity.getX() - entity.xOld) * (double)mc.getFrameTime();
      double d1 = entity.yOld + (entity.getY() - entity.yOld) * (double)mc.getFrameTime();
      double d2 = entity.zOld + (entity.getZ() - entity.zOld) * (double)mc.getFrameTime();
      builder.vertex(stack.last().pose(), (float)(x - d0), (float)(y - d1) - 1.5F, (float)(z - d2)).color(color.getRGB()).endVertex();
   }

   private ProjectileData getProjectileDataByEntity(Entity entity) {
      if (entity.onGround()) {
         return null;
      } else if (entity.getX() == entity.xOld && entity.getZ() == entity.zOld) {
         return null;
      } else {
         for (ProjectileData data : this.getProjectileInfos()) {
            if (data.isTargetEntity(entity)) {
               return data;
            }
         }

         return null;
      }
   }

   private List<ProjectileData> getProjectileInfos() {
      ArrayList<ProjectileData> infos = new ArrayList<>();
      if (this.showArrows.getCurrentValue()) {
         infos.add(this.arrowsColor);
      }

      if (this.showPotions.getCurrentValue()) {
         infos.add(this.potionsColor);
      }

      if (this.showPearls.getCurrentValue()) {
         infos.add(this.enderPearlColor);
      }

      if (this.showEggs.getCurrentValue()) {
         infos.add(this.eggColor);
      }

      if (this.showSnowballs.getCurrentValue()) {
         infos.add(this.snowballColor);
      }

      return infos;
   }

   public static class Path {
      private final List<Vec3> path;
      private final HitResult result;

      public Path(List<Vec3> path, HitResult result) {
         this.path = path;
         this.result = result;
      }

      public List<Vec3> getPath() {
         return this.path;
      }

      public HitResult getResult() {
         return this.result;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof Path other)) {
            return false;
         } else if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$path = this.getPath();
            Object other$path = other.getPath();
            if (this$path == null ? other$path == null : this$path.equals(other$path)) {
               Object this$result = this.getResult();
               Object other$result = other.getResult();
               return this$result == null ? other$result == null : this$result.equals(other$result);
            } else {
               return false;
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof Path;
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         Object $path = this.getPath();
         result = result * 59 + ($path == null ? 43 : $path.hashCode());
         Object $result = this.getResult();
         return result * 59 + ($result == null ? 43 : $result.hashCode());
      }

      @Override
      public String toString() {
         return "Projectile.Path(path=" + this.getPath() + ", result=" + this.getResult() + ")";
      }
   }
}
