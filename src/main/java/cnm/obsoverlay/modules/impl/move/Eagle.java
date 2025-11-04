package cnm.obsoverlay.modules.impl.move;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventMotion;
import cnm.obsoverlay.events.impl.EventMoveInput;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import cnm.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;

/**
 * Eagle: 在方块边缘自动潜行（辅助桥接/防跌落），灵感来自 LiquidBounce ModuleEagle。
 * 集成到本项目的事件/值系统，并尽量复用 SafeWalk 的边缘判断逻辑。
 */
@ModuleInfo(
   name = "Eagle",
   description = "Legit trick to build faster. Auto-sneak near edges.",
   category = Category.MOVEMENT
)
public class Eagle extends Module {
   private final BooleanValue backwards = ValueBuilder.create(this, "OnlyBackwards")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();
   private final BooleanValue onlyWithBlocks = ValueBuilder.create(this, "OnlyWithBlocks")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   private float forward = 0;


   public static boolean isOnBlockEdge(float sensitivity) {
      return !mc.level
              .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate((double)(-sensitivity), 0.0, (double)(-sensitivity)))
              .iterator()
              .hasNext();
   }

   /**
    * 处理移动输入事件：在边缘处将输入中的潜行状态设为 true。
    * 本项目有 EventMoveInput，可无侵入地修改 sneak。
    */
   @EventTarget
   public void onMoveInput(EventMoveInput event) {
      LocalPlayer player = mc.player;
      if (player == null) return;

      if (!player.onGround()) {
         return;
      }

      if (event.getForward() != 0) this.forward = event.getForward();

      if (backwards.getCurrentValue() && forward > 0) {
         return;
      }

      if (onlyWithBlocks.getCurrentValue() &&
              (mc.player.getMainHandItem().isEmpty() ||
                      !(mc.player.getMainHandItem().getItem() instanceof BlockItem))) {
         return;
      }

      // 复用 SafeWalk 的边缘检测，改用更灵活的灵敏度来自配置
      boolean closeToEdge = isOnBlockEdge(0.3F);

      if (!player.getAbilities().flying && closeToEdge) {
         event.setSneak(true);
      }
   }

    @Override
    public void onEnable() {
        forward = 0;
    }

    @Override
    public void onDisable() {
        forward = 0;
    }
}


