package cnm.obsoverlay.modules.impl.combat;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.*;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.modules.impl.move.LongJump;
import cnm.obsoverlay.utils.PacketSnapshot;
import cnm.obsoverlay.utils.RenderUtils;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import cnm.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Title: BackTrack
 * @Author jiuxian_baka
 * @Package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat
 * @Date 2025/8/27 13:51
 * @description: 回溯
 */
@ModuleInfo(
        name = "BackTrack",
        description = "Causes targets to lag allowing you to attack them more efficiently.",
        category = Category.COMBAT
)
public class BackTrack extends Module {
    private static final float[] color = new float[]{0.78431374F, 0.0F, 0.0F, 0.39215686F};
    private static final double POSITION_THRESHOLD = 0.1; // 位置差异阈值
    private final FloatValue delay = ValueBuilder.create(this, "Delay")
            .setDefaultFloatValue(500.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(2000.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    private final FloatValue maxDistance = ValueBuilder.create(this, "Max Distance")
            .setDefaultFloatValue(3.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();
    private final FloatValue minDistance = ValueBuilder.create(this, "Distance")
            .setDefaultFloatValue(0.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();
    private final BooleanValue onlyKillaura = ValueBuilder.create(this, "OnlyKillaura")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final LinkedBlockingQueue<PacketSnapshot> packets = new LinkedBlockingQueue<>();
    //    private final SmoothAnimationTimer progress = new SmoothAnimationTimer(0.0F, 0.2F);
    private final Map<Entity, Vec3> serverPositions = new HashMap<>();
    private boolean working;
    private boolean foundTarget;
    public boolean alink = false;

    @EventTarget
    public void onPreTick(EventRunTicks event) {
        // Only operate on PRE and when world/player are available
        if (event.getType() != EventType.PRE) return;
        if (mc.player == null || mc.level == null) return;
        this.setSuffix((int) delay.getCurrentValue() + " ms");
        if (onlyKillaura.currentValue && !Naven.getInstance().getModuleManager().getModule(Aura.class).isEnabled()) {
            clear();
            return;
        }

        float min = 0.0f;
        float max = this.minDistance.getCurrentValue();
        double minSq = (double) min * (double) min;
        double maxSq = (double) max * (double) max;

        boolean found = false;
        for (AbstractClientPlayer player : mc.level.players()) {
            if (player == mc.player || AntiBots.isBot(player) && AntiBots.isBedWarsBot(player)) {
                continue;
            }
            double distSq = mc.player.distanceToSqr(player.getX(), player.getY(), player.getZ());
            if (distSq >= minSq && distSq <= maxSq) {
                found = true;
                break;
            }
        }

        this.foundTarget = found;
        this.working = this.foundTarget;

    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player != null && mc.level != null) {
            if (e.getType() == EventType.RECEIVE) {
                if (working && !Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled() && !alink) {
                    Packet<?> packet = e.getPacket();
                    Velocity velocityModule = Naven.getInstance().getModuleManager().getModule(Velocity.class);
                    if (packet instanceof ClientboundSetEntityMotionPacket setEntityMotionPacket) {
                        if (setEntityMotionPacket.getId() != mc.player.getId()) return;
                        if (velocityModule.mode.isCurrentMode("GrimReduce")) {
                            clear();
                            alink = true;
                            return;
                        }
                    }
                    if (packet instanceof ClientboundSetHealthPacket healthPacket) {
                        if (healthPacket.getHealth() <= 0) {
                            clear();
                        }
                        return;
                    }
                    if (packet instanceof ClientboundRespawnPacket) {
                        clear();
                        return;
                    }
                    if (packet instanceof ClientboundPlayerPositionPacket playerPositionPacket) {
                        int id = playerPositionPacket.getId();
                        Entity entity = mc.level.getEntity(id);
                        if (id == mc.player.getId()) {
                            clear();
                            return;
                        }

                        if (entity != mc.player && entity instanceof Player) {
                            // 直接使用数据包中的位置作为真实服务器位置
                            double x = playerPositionPacket.getX();
                            double y = playerPositionPacket.getY();
                            double z = playerPositionPacket.getZ();

                            // 检查与玩家眼部的距离
                            Vec3 eyePos = mc.player.getEyePosition();
                            double distance = eyePos.distanceTo(new Vec3(x, y, z));
                            if (distance > maxDistance.getCurrentValue()) {
                                // 检查实体是否在 serverPositions 中，且之前的位置在范围内
                                if (serverPositions.containsKey(entity)) {
                                    Vec3 oldPos = serverPositions.get(entity);
                                    double oldDistance = eyePos.distanceTo(oldPos);
                                    if (oldDistance <= maxDistance.getCurrentValue()) {
                                        clear();
                                        return;
                                    }
                                }
                                return;
                            }

                            serverPositions.put(entity, new Vec3(x, y, z));
                        }
                    }
                    if (packet instanceof ClientboundMoveEntityPacket moveEntityPacket) {
                        Entity entity = moveEntityPacket.getEntity(mc.level);
                        if (entity != mc.player && entity instanceof Player) {
                            // 获取当前服务器位置，如果没有则使用实体当前位置
                            Vec3 currentServerPos = serverPositions.getOrDefault(entity, entity.position());

                            // 使用数据包中的增量更新真实服务器位置
                            double newX = currentServerPos.x + (moveEntityPacket.getXa() / 4096.0);
                            double newY = currentServerPos.y + (moveEntityPacket.getYa() / 4096.0);
                            double newZ = currentServerPos.z + (moveEntityPacket.getZa() / 4096.0);

                            // 检查与玩家眼部的距离
                            Vec3 eyePos = mc.player.getEyePosition();
                            double distance = eyePos.distanceTo(new Vec3(newX, newY, newZ));
                            if (distance > maxDistance.getCurrentValue()) {
                                // 检查实体是否在 serverPositions 中，且之前的位置在范围内
                                if (serverPositions.containsKey(entity)) {
                                    Vec3 oldPos = serverPositions.get(entity);
                                    double oldDistance = eyePos.distanceTo(oldPos);
                                    if (oldDistance <= maxDistance.getCurrentValue()) {
                                        clear();
                                        return;
                                    }
                                }
                                return;
                            }

                            serverPositions.put(entity, new Vec3(newX, newY, newZ));
                        }
                    }
                    if (packet instanceof ClientboundTeleportEntityPacket teleportEntityPacket) {
                        int id = teleportEntityPacket.getId();
                        Entity entity = mc.level.getEntity(id);
                        if (entity != mc.player && entity instanceof Player) {
                            // 直接使用数据包中的传送位置作为真实服务器位置
                            double x = teleportEntityPacket.getX();
                            double y = teleportEntityPacket.getY();
                            double z = teleportEntityPacket.getZ();

                            // 检查与玩家眼部的距离
                            Vec3 eyePos = mc.player.getEyePosition();
                            double distance = eyePos.distanceTo(new Vec3(x, y, z));
                            if (distance > maxDistance.getCurrentValue()) {
                                // 检查实体是否在 serverPositions 中，且之前的位置在范围内
                                if (serverPositions.containsKey(entity)) {
                                    Vec3 oldPos = serverPositions.get(entity);
                                    double oldDistance = eyePos.distanceTo(oldPos);
                                    if (oldDistance <= maxDistance.getCurrentValue()) {
                                        clear();
                                        return;
                                    }
                                }
                                return;
                            }

                            serverPositions.put(entity, new Vec3(x, y, z));
                        }
                    }

                    if (packet instanceof ClientboundPlayerPositionPacket || packet instanceof ClientboundMoveEntityPacket || packet instanceof ClientboundTeleportEntityPacket || packet instanceof ClientboundPingPacket || packet instanceof ClientboundSetEntityMotionPacket) {
                        packets.add(new PacketSnapshot(packet, System.currentTimeMillis()));
                        e.setCancelled(true);
                    }
                } else {
                    clear();
                }
            }
        }
    }

    private void clear() {
        while (!this.packets.isEmpty()) {
            try {
                Packet<?> packet = this.packets.poll().packet;
                if (packet != null && mc.getConnection() != null) {
                    EventBackrackPacket eventBackrackPacket = new EventBackrackPacket(packet);
                    Naven.getInstance().getEventManager().call(eventBackrackPacket);
                    if (eventBackrackPacket.cancelled) return;
                    Packet<? super ClientPacketListener> clientPacket = (Packet<? super ClientPacketListener>) eventBackrackPacket.getPacket();
                    clientPacket.handle(mc.getConnection());
                }
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        }
        serverPositions.clear();
        foundTarget = false;
        working = false;

//        this.progress.target = Mth.clamp(0.0F, 0.0F, 100.0F);

    }


//    @EventTarget
//    public void onRender2D(EventRender2D e) {
//        if (this.foundTarget) {
//            int x = mc.getWindow().getGuiScaledWidth() / 2 - 50;
//            int y = mc.getWindow().getGuiScaledHeight() / 2 + 15;
//            this.progress.update(true);
//            RenderUtils.drawRoundedRect(e.getStack(), (float) x, (float) y, 100.0F, 5.0F, 2.0F, Integer.MIN_VALUE);
//            RenderUtils.drawRoundedRect(e.getStack(), (float) x, (float) y, this.progress.value, 5.0F, 2.0F, mainColor);
//        }
//    }

    @EventTarget
    public void onRender(EventRender e) {

        // Render real server positions similar to Aura targetEsp
        if (!serverPositions.isEmpty()) {
            PoseStack stack = e.getPMatrixStack();
            stack.pushPose();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glEnable(2848);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderUtils.applyRegionalRenderOffset(stack);

            for (Map.Entry<Entity, Vec3> entry : serverPositions.entrySet()) {
                Entity entity = entry.getKey();
                if (entity instanceof LivingEntity living) {
                    Vec3 serverPos = entry.getValue();
                    Vec3 clientPos = entity.position();

                    // 渲染真实的服务器位置（数据包位置）
                    // serverPos 是从数据包获取的真实位置
                    // clientPos 是实体当前位置（因为拦截了数据包所以是旧位置）
                    double distance = serverPos.distanceTo(clientPos);
                    if (distance > POSITION_THRESHOLD) {
                        double motionX = 0.0;
                        double motionY = 0.0;
                        double motionZ = 0.0;
                        AABB bb = living.getBoundingBox().move(-living.getX(), -living.getY(), -living.getZ())
                                .move(serverPos.x, serverPos.y, serverPos.z)
                                .move(-motionX, -motionY, -motionZ);
                        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
                        RenderUtils.drawSolidBox(bb, stack);
                    }
                }
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDisable(3042);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(2848);
            stack.popPose();
        }
    }

    @EventTarget
    public void onRender2d(EventRender2D event) {

        releasePacket();

    }

    @Override
    public void onDisable() {
        clear();
        alink = false;
    }

    public void releasePacket() {
        if (mc.player == null) return;
        this.packets.removeIf(it -> {
            if (System.currentTimeMillis() - it.tick >= delay.getCurrentValue()) {
                Packet<?> packet = it.packet;
                if (packet != null && mc.getConnection() != null) {
                    EventBackrackPacket eventBackrackPacket = new EventBackrackPacket(packet);
                    Naven.getInstance().getEventManager().call(eventBackrackPacket);
                    if (eventBackrackPacket.cancelled) return true;
                    Packet<? super ClientPacketListener> clientPacket = (Packet<? super ClientPacketListener>) eventBackrackPacket.getPacket();
                    clientPacket.handle(mc.getConnection());
                    return true;
                }
            }
            return false;
        });
    }

    @EventTarget
    public void onRespawn(EventRespawn e) {
        clear();
        alink = false;
    }
}

