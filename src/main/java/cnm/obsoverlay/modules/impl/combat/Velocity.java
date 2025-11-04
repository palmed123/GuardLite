package cnm.obsoverlay.modules.impl.combat;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.EventMoveInput;
import cnm.obsoverlay.events.impl.EventPacket;
import cnm.obsoverlay.events.impl.EventRender2D;
import cnm.obsoverlay.events.impl.EventUpdate;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.utils.ChatUtils;
import cnm.obsoverlay.utils.PacketSnapshot;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import cnm.obsoverlay.values.impl.FloatValue;
import cnm.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.LinkedBlockingQueue;

@ModuleInfo(name = "Velocity", category = Category.COMBAT, description = "Reduces knockback.")
public class Velocity extends Module {
    //    private final BooleanValue logging = ValueBuilder.create(this, "Logging")
//            .setDefaultBooleanValue(false)
//            .build()
//            .getBooleanValue();
    // Global Values
    ModeValue mode = ValueBuilder.create(this, "Mode").setModes("Attack", "GrimReduce").build().getModeValue();
    private final BooleanValue jumpReset = ValueBuilder.create(this, "JumpReset")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    private final FloatValue jumpTick = ValueBuilder.create(this, "JumpTick")
            .setDefaultFloatValue(9.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .setVisibility(() -> jumpReset.currentValue)
            .build()
            .getFloatValue();
    // Attack Mode
    private final FloatValue attackTick = ValueBuilder.create(this, "AttackTick")
            .setDefaultFloatValue(6.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .setVisibility(() -> mode.getCurrentMode().equals("Attack"))
            .build()
            .getFloatValue();
    // GrimReduce Mode
    private final ModeValue grimReduceMode = ValueBuilder.create(this, "GrimReduce Mode")
            .setDefaultModeIndex(0)
            .setModes("OneTime", "PerTick")
            .setVisibility(() -> mode.getCurrentMode().equals("GrimReduce"))
            .build()
            .getModeValue();
    private final FloatValue attacks = ValueBuilder.create(this, "Attack Count")
            .setDefaultFloatValue(2.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> mode.getCurrentMode().equals("GrimReduce"))
            .build()
            .getFloatValue();
    private final FloatValue delay = ValueBuilder.create(this, "AlinkMaxDelay")
            .setDefaultFloatValue(2500.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(5000.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> mode.getCurrentMode().equals("GrimReduce"))
            .build()
            .getFloatValue();
    private final BooleanValue logging = ValueBuilder.create(this, "Logging")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.getCurrentMode().equals("GrimReduce"))
            .build()
            .getBooleanValue();

    private final LinkedBlockingQueue<PacketSnapshot> packets = new LinkedBlockingQueue<>();
    private int attackQueue = 0;
    private boolean alink = false;
    private boolean velPacket;

    private void log(String message) {
        if (this.logging.getCurrentValue()) {
            ChatUtils.addChatMessage(message);
        }
    }

    @Override
    public void onDisable() {
        this.attackQueue = 0;
        alink = false;
        BackTrack backtrack = Naven.getInstance().getModuleManager().getModule(BackTrack.class);
        backtrack.alink = false;
        velPacket = false;
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.level == null || mc.player == null) return;
        if (!mode.getCurrentMode().equals("GrimReduce")) return;
        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundSetHealthPacket healthPacket) {
            velPacket = true;
        }

        if (packet instanceof ClientboundSetEntityMotionPacket velocityPacket) {
            if (velocityPacket.getId() != mc.player.getId()) {
                return;
            }
//            if (!velPacket) {
//                log("No Health Packet.");
//                return;
//            }
            velPacket = false;
            HitResult hitResult = mc.hitResult;
            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult result = (EntityHitResult) hitResult;
                Entity entity = result.getEntity();
                if (AntiBots.isBot(entity) || !(entity instanceof Player)) {
                    log("Not player.");
                    alink = true;
                    packets.add(new PacketSnapshot(packet, System.currentTimeMillis()));
                    event.setCancelled(true);
                    return;
                }
                this.attackQueue = (int) this.attacks.getCurrentValue();
                BackTrack backtrack = Naven.getInstance().getModuleManager().getModule(BackTrack.class);
                backtrack.alink = false;
            } else {
                log("Packet event no target.");
                alink = true;
            }
        }

        if (packet instanceof ClientboundPlayerPositionPacket playerPositionPacket) {
            int id = playerPositionPacket.getId();
            if (id == mc.player.getId()) {
                log("Flag detected.");
                clear(false);
            }
        }

        if (alink && (packet instanceof ClientboundSetEntityMotionPacket || packet instanceof ClientboundPingPacket)) {
            packets.add(new PacketSnapshot(packet, System.currentTimeMillis()));
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        this.setSuffix(mode.getCurrentMode());
        switch (mode.getCurrentMode()) {
            case "Attack" -> attackUpdateHandler();
            case "GrimReduce" -> grimReduceUpdateHandler();
        }
    }

    @EventTarget
    public void onRender2d(EventRender2D event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null || mc.getConnection() == null || this.packets.isEmpty())
            return;
        for (PacketSnapshot it : packets) {
            if (System.currentTimeMillis() - it.tick >= delay.getCurrentValue()) {
                clear(false);
                return;
            }
        }
    }

    // JumpReset
    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.player != null && mc.player.hurtTime == jumpTick.getCurrentValue() && jumpReset.currentValue) {
            event.setJump(true);
        }
    }

    private void attackUpdateHandler() {
        if (mc.player == null || mc.level == null || mc.gameMode == null || mc.getConnection() == null) return;
        alink = false;
        BackTrack backtrack = Naven.getInstance().getModuleManager().getModule(BackTrack.class);
        backtrack.alink = false;
        if (mc.player.tickCount <= 20) return;
        if (mc.player.hurtTime >= attackTick.getCurrentValue()) {
            HitResult hitResult = mc.hitResult;
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult result = (EntityHitResult) hitResult;
                Entity entity = result.getEntity();
                if (AntiBots.isBot(entity) || !(entity instanceof Player)) {
                    log("Not player.");
                    return;
                }
//                if (mc.player.isSprinting()) mc.player.setSprinting(false);
                mc.gameMode.attack(mc.player, result.getEntity());
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    private void grimReduceUpdateHandler() {
        if (mc.player == null || mc.getConnection() == null || mc.gameMode == null) return;
        if (mc.player.hurtTime == 0) {
            this.attackQueue = 0;
        }
        HitResult hitResult = mc.hitResult;
        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult result = (EntityHitResult) hitResult;
            Entity entity = result.getEntity();
            if (!AntiBots.isBot(entity) && entity instanceof Player) {
                if (this.attackQueue <= 0 && !packets.isEmpty()) clear(true);
                if (this.attackQueue > 0) {
                    Naven.skipTicks++;
                    if (this.grimReduceMode.getCurrentMode().equals("OneTime")) {
                        for (; attackQueue >= 1; attackQueue--) {
                            if (mc.player.isSprinting()) mc.player.setSprinting(false);
                            mc.gameMode.attack(mc.player, result.getEntity());
                            mc.player.swing(InteractionHand.MAIN_HAND);
                            mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1, 0.6));
                            log("Attack queue: " + attackQueue);
                        }
                    } else if (this.grimReduceMode.getCurrentMode().equals("PerTick")) {
                        if (attackQueue >= 1) {
                            if (mc.player.isSprinting()) mc.player.setSprinting(false);
                            mc.gameMode.attack(mc.player, result.getEntity());
                            mc.player.swing(InteractionHand.MAIN_HAND);
                            mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1, 0.6));
                            log("Attack queue: " + attackQueue);
                        }
                        attackQueue--;
                    }
                    return;
                }
            }
        }
        if (this.attackQueue > 0) log("Pre tick no target.");
    }

    public void clear(boolean attack) {
        while (!this.packets.isEmpty()) {
            try {
                Packet<?> packet = this.packets.poll().packet;
                if (packet != null && mc.getConnection() != null) {
                    Packet<? super ClientPacketListener> clientPacket = (Packet<? super ClientPacketListener>) packet;
                    clientPacket.handle(mc.getConnection());
                }
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        }
        alink = false;
        BackTrack backtrack = Naven.getInstance().getModuleManager().getModule(BackTrack.class);
        backtrack.alink = false;
        if (attack) this.attackQueue = (int) this.attacks.getCurrentValue();
        else this.attackQueue = 0;
    }
}
