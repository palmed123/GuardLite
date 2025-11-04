package cnm.obsoverlay.modules.impl.combat;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.*;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.FloatValue;
import cnm.obsoverlay.values.impl.ModeValue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.LivingEntity;

@ModuleInfo(
    name = "WTap",
    description = "Cause the target to experience greater knockback",
    category = Category.COMBAT
)
public class WTap extends Module {

    private final ModeValue modeValue = ValueBuilder.create(this, "Mode")
        .setDefaultModeIndex(1)
        .setModes("Wtap", "Legit", "Packet")
        .build()
        .getModeValue();

    private final FloatValue hurtTime = ValueBuilder.create(this, "HurtTime")
        .setDefaultFloatValue(10.0F)
        .setMinFloatValue(0.0F)
        .setMaxFloatValue(10.0F)
        .setFloatStep(1.0F)
        .build()
        .getFloatValue();

    public int tick;
    private boolean wasWKeyPressed = false;

    @EventTarget
    private void onAttack(EventAttack event) {
        if (mc.player == null || mc.level == null) return;

        LivingEntity entity = (LivingEntity) event.getTarget();

        if (entity != null && entity.hurtTime >= hurtTime.getCurrentValue()) {
            String mode = modeValue.getCurrentMode();

            switch (mode) {
                case "Legit", "Wtap" -> tick = 2;

                case "Packet" -> {
                    if (mc.player.isSprinting()) {
                        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                    }

                    mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));

                    mc.player.setSprinting(true);
                    try {
                        mc.player.getClass().getMethod("setWasSprinting", boolean.class).invoke(mc.player, true);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.player == null) return;

        if (modeValue.getCurrentMode().equals("Wtap")) {
            if (tick == 2) {
                wasWKeyPressed = mc.options.keyUp.isDown();
                event.setForward(0.0f);
                tick = 1;
            } else if (tick == 1) {
                if (!wasWKeyPressed) {
                    event.setForward(1.0f);
                } else {
                    event.setForward(1.0f);
                }
                tick = 0;
                wasWKeyPressed = false;
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate eventUpdate) {
        if (mc.player == null) return;

        if (modeValue.getCurrentMode().equals("Legit")) {
            if (tick == 2) {
                mc.player.setSprinting(false);
                tick = 1;
            } else if (tick == 1) {
                mc.player.setSprinting(true);
                tick = 0;
            }
        }
    }

    @Override
    public void onEnable() {
        tick = 0;
        wasWKeyPressed = false;
    }

    @Override
    public void onDisable() {
        tick = 0;
        if (mc.options != null && mc.options.keyUp != null) {
            if (!wasWKeyPressed) {
                mc.options.keyUp.setDown(true);
            } else {
                mc.options.keyUp.setDown(true);
            }
        }
        wasWKeyPressed = false;
    }
}