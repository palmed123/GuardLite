package cnm.obsoverlay.modules.impl.move;

import cn.paradisemc.Native;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventClick;
import cnm.obsoverlay.events.impl.EventRespawn;
import cnm.obsoverlay.events.impl.EventRunTicks;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.ui.notification.Notification;
import cnm.obsoverlay.ui.notification.NotificationLevel;
import cnm.obsoverlay.utils.PacketUtils;
import cnm.obsoverlay.utils.ReflectUtil;
import cnm.obsoverlay.utils.auth.AuthUtils;
import cnm.obsoverlay.utils.rotation.RotationManager;
import cnm.obsoverlay.utils.vector.Vector2f;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;

import java.lang.reflect.Method;

@ModuleInfo(
   name = "AutoMLG",
   description = "Automatically places water when falling",
   category = Category.MOVEMENT
)
public class AutoMLG extends Module {
   FloatValue distance = ValueBuilder.create(this, "Fall Distance")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(15.0F)
      .build()
      .getFloatValue();
   public boolean rotation = false;
   private boolean placeWater = false;
   private int originalSlot;
   private boolean 收水 = false;

   @EventTarget
   public void onPre(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.player != null) {
          if (this.placeWater) {
              this.placeWater = false;
              this.收水 = true;
              if (mc.hitResult.getType() == Type.BLOCK && ((BlockHitResult)mc.hitResult).getDirection() == Direction.UP) {
                  this.useItem(mc.player, mc.level, InteractionHand.MAIN_HAND);
              }
          } else if (收水 && mc.player.onGround()){
              收水 = false;
              this.useItem(mc.player, mc.level, InteractionHand.MAIN_HAND);
              mc.player.getInventory().selected = this.originalSlot;
              rotation = false;
          }
          Scaffold scaffold = Naven.getInstance().getModuleManager().getModule(Scaffold.class);
          if (mc.player.fallDistance > this.distance.getCurrentValue() && !scaffold.isEnabled() && !rotation) {
               for (int i = 0; i < 9; i++) {
                  ItemStack item = mc.player.getInventory().getItem(i);
                  if (!item.isEmpty() && item.getItem() == Items.WATER_BUCKET) {
                     this.originalSlot = mc.player.getInventory().selected;
                     mc.player.getInventory().selected = i;
                      rotation = true;
                      placeWater = true;
                      收水 = false;
                  }
               }
         }
          if (rotation) {
              RotationManager.setRotations(
                      new Vector2f(mc.player.getYRot(), 90.0F),
                      10.0
              );
          }
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
   }

   public InteractionResult useItem(Player pPlayer, Level pLevel, InteractionHand pHand) {
//      MultiPlayerGameModeAccessor gameMode = (MultiPlayerGameModeAccessor)mc.gameMode;
      GameType gameType = (GameType) ReflectUtil.getFieldValue(MultiPlayerGameMode.class, "localPlayerMode", mc.gameMode);
      if (gameType == GameType.SPECTATOR) {
         return InteractionResult.PASS;
      } else {
         Method ensureHasSentCarriedItem = ReflectUtil.getMethod(MultiPlayerGameMode.class, "ensureHasSentCarriedItem", "()V");
          try {
              ensureHasSentCarriedItem.invoke(mc.gameMode);
          } catch (Exception e) {
              throw new RuntimeException(e);
          }
          PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(pHand, id));
         ItemStack itemstack = pPlayer.getItemInHand(pHand);
         if (pPlayer.getCooldowns().isOnCooldown(itemstack.getItem())) {
            return InteractionResult.PASS;
         } else {
            InteractionResult cancelResult = ForgeHooks.onItemRightClick(pPlayer, pHand);
            if (cancelResult != null) {
               return cancelResult;
            } else {
               InteractionResultHolder<ItemStack> interactionresultholder = itemstack.use(pLevel, pPlayer, pHand);
               ItemStack itemstack1 = (ItemStack)interactionresultholder.getObject();
               if (itemstack1 != itemstack) {
                  pPlayer.setItemInHand(pHand, itemstack1);
                  if (itemstack1.isEmpty()) {
                     ForgeEventFactory.onPlayerDestroyItem(pPlayer, itemstack, pHand);
                  }
               }

               return interactionresultholder.getResult();
            }
         }
      }
   }
}
