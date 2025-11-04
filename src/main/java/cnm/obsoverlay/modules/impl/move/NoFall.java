package cnm.obsoverlay.modules.impl.move;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventMotion;
import cnm.obsoverlay.events.impl.EventPacket;
import cnm.obsoverlay.events.impl.EventRespawn;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.FloatValue;
import cnm.obsoverlay.values.impl.ModeValue;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import java.lang.reflect.Field;

@ModuleInfo(
   name = "NoFall",
   description = "Prevents fall damage",
   category = Category.MOVEMENT
)
public class NoFall extends Module {

    FloatValue distance = ValueBuilder.create(this, "Fall Distance")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(3.0F)
            .setMaxFloatValue(15.0F)
            .build()
            .getFloatValue();

    boolean lastOnGround = false;
    float lastFallDis = 0.0f;

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.level == null || event.isCancelled()) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ServerboundMovePlayerPacket move) {

            if (move.isOnGround() && !lastOnGround && lastFallDis >= distance.getCurrentValue()) {
                event.setCancelled(true);
                double x = move.getX(mc.player.getX());
                double y = move.getY(mc.player.getY());
                double z = move.getZ(mc.player.getZ());
                float xRot = move.getXRot(mc.player.getXRot());
                float yRot = move.getYRot(mc.player.getYRot());

                mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(x + 1000.0f, y, z, xRot, yRot, false));

            }

            lastOnGround = move.isOnGround();
            lastFallDis = mc.player.fallDistance;
        }
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            lastOnGround = mc.player.onGround();
        } else {
            lastOnGround = true;
        }
        lastFallDis = 0.0f;
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            lastOnGround = mc.player.onGround();
        } else {
            lastOnGround = true;
        }
        lastFallDis = 0.0f;
    }
}
