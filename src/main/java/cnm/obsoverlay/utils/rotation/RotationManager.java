package cnm.obsoverlay.utils.rotation;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.*;
import cnm.obsoverlay.modules.impl.move.Scaffold;
import cnm.obsoverlay.utils.MoveUtils;
import cnm.obsoverlay.utils.Wrapper;
import cnm.obsoverlay.utils.vector.Vector2f;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.util.Mth;

import java.util.function.Function;

public class RotationManager implements Wrapper {
    public static boolean active = false;
    public static boolean smoothed = false;
    public static Vector2f rotations;
    public static Vector2f lastRotations = new Vector2f(0, 0);
    public static Vector2f targetRotations;
    public static Vector2f lastServerRotations;
    public static Vector2f animationRotation;
    public static Vector2f lastAnimationRotation;
    
    private static double rotationSpeed;
    private static Function<Vector2f, Boolean> raycast;
    private static float randomAngle;
    private static final Vector2f offset = new Vector2f(0, 0);

    /**
     * 设置旋转（带射线检测）
     */
    public static void setRotations(final Vector2f rotations, final double rotationSpeed, final Function<Vector2f, Boolean> raycast) {
        RotationManager.targetRotations = rotations;
        RotationManager.rotationSpeed = rotationSpeed * 36;
        RotationManager.raycast = raycast;
        active = true;

        smooth();
    }

    /**
     * 设置旋转（不带射线检测）
     */
    public static void setRotations(final Vector2f rotations, final double rotationSpeed) {
        setRotations(rotations, rotationSpeed, null);
    }


    @EventTarget
    public void onRespawn(EventRespawn e) {
        lastRotations = null;
        rotations = null;
    }

    @EventTarget(4)
    public void updateGlobalYaw(EventRunTicks e) {
        if (e.getType() == EventType.PRE && mc.player != null) {
            if (!active || rotations == null || lastRotations == null || targetRotations == null || lastServerRotations == null) {
                rotations = lastRotations = targetRotations = lastServerRotations = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
            }

            if (active) {
                smooth();
            }
        }
    }

    public static void smooth() {
        if (!smoothed) {
            float targetYaw = targetRotations.x;
            float targetPitch = targetRotations.y;

            // Randomisation (随机化，模拟人类不精准瞄准)
            if (raycast != null && (Math.abs(targetYaw - rotations.x) > 5 || Math.abs(targetPitch - rotations.y) > 5)) {
                final Vector2f trueTargetRotations = new Vector2f(targetRotations.x, targetRotations.y);

                double speed = (Math.random() * Math.random() * Math.random()) * 20;
                randomAngle += (float) ((20 + (float) (Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360)) * (mc.player.tickCount / 10 % 2 == 0 ? -1 : 1));

                offset.x = (float) (offset.x + -Mth.sin((float) Math.toRadians(randomAngle)) * speed);
                offset.y = (float) (offset.y + Mth.cos((float) Math.toRadians(randomAngle)) * speed);

                targetYaw += offset.x;
                targetPitch += offset.y;

                if (!raycast.apply(new Vector2f(targetYaw, targetPitch))) {
                    randomAngle = (float) Math.toDegrees(Math.atan2(trueTargetRotations.x - targetYaw, targetPitch - trueTargetRotations.y)) - 180;

                    targetYaw -= offset.x;
                    targetPitch -= offset.y;

                    offset.x = (float) (offset.x + -Mth.sin((float) Math.toRadians(randomAngle)) * speed);
                    offset.y = (float) (offset.y + Mth.cos((float) Math.toRadians(randomAngle)) * speed);

                    targetYaw = targetYaw + offset.x;
                    targetPitch = targetPitch + offset.y;
                }

                if (!raycast.apply(new Vector2f(targetYaw, targetPitch))) {
                    offset.x = 0;
                    offset.y = 0;

                    targetYaw = (float) (targetRotations.x + Math.random() * 2);
                    targetPitch = (float) (targetRotations.y + Math.random() * 2);
                }
            }

            rotations = smoothRotation(lastRotations, new Vector2f(targetYaw, targetPitch), rotationSpeed + Math.random());
        }

        smoothed = true;
    }

    /**
     * 平滑旋转算法 (完全复制自 RotationUtil.smooth)
     */
    private static Vector2f smoothRotation(final Vector2f lastRotation, final Vector2f targetRotation, final double speed) {
        float yaw = targetRotation.x;
        float pitch = targetRotation.y;
        final float lastYaw = lastRotation.x;
        final float lastPitch = lastRotation.y;

        if (speed != 0) {
            Vector2f move = calculateMove(lastRotation, targetRotation, speed);

            yaw = lastYaw + move.x;
            pitch = lastPitch + move.y;
            
            for (int i = 1; i <= (int) (mc.getFps() / 20f + Math.random() * 10); ++i) {
                if (Math.abs(move.x) + Math.abs(move.y) > 0.0001) {
                    yaw += (Math.random() - 0.5) / 1000;
                    pitch -= Math.random() / 200;
                }

                // Fixing GCD
                final Vector2f rotations = new Vector2f(yaw, pitch);
                final Vector2f fixedRotations = applySensitivityPatch(rotations, lastRotation);

                yaw = fixedRotations.x;
                pitch = Math.max(-90, Math.min(90, fixedRotations.y));
            }
        }

        return new Vector2f(yaw, pitch);
    }

    /**
     * 计算移动量
     */
    private static Vector2f calculateMove(final Vector2f lastRotation, final Vector2f targetRotation, double speed) {
        if (speed == 0) {
            return new Vector2f(0, 0);
        }

        double deltaYaw = Mth.wrapDegrees(targetRotation.x - lastRotation.x);
        final double deltaPitch = (targetRotation.y - lastRotation.y);

        final double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
        final double distributionYaw = Math.abs(deltaYaw / distance);
        final double distributionPitch = Math.abs(deltaPitch / distance);

        final double maxYaw = speed * distributionYaw;
        final double maxPitch = speed * distributionPitch;

        final float moveYaw = (float) Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
        final float movePitch = (float) Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);

        return new Vector2f(moveYaw, movePitch);
    }

    /**
     * 应用鼠标灵敏度修正 (GCD)
     */
    private static Vector2f applySensitivityPatch(final Vector2f rotation, final Vector2f previousRotation) {
        final float mouseSensitivity = (float) (mc.options.sensitivity().get() * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        final double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        final float yaw = previousRotation.x + (float) (Math.round((rotation.x - previousRotation.x) / multiplier) * multiplier);
        final float pitch = previousRotation.y + (float) (Math.round((rotation.y - previousRotation.y) / multiplier) * multiplier);
        return new Vector2f(yaw, Mth.clamp(pitch, -90, 90));
    }

    /**
     * 重置旋转
     */
    public static Vector2f resetRotation(final Vector2f rotation) {
        if (rotation == null) {
            return null;
        }

        final float yaw = rotation.x + Mth.wrapDegrees(mc.player.getYRot() - rotation.x);
        final float pitch = mc.player.getXRot();
        return new Vector2f(yaw, pitch);
    }

    @EventTarget
    public void onAnimation(EventRotationAnimation e) {
        if (animationRotation != null && lastAnimationRotation != null) {
            e.setYaw(animationRotation.x);
            e.setLastYaw(lastAnimationRotation.x);
            e.setPitch(animationRotation.y);
            e.setLastPitch(lastAnimationRotation.y);
        }
    }

    @EventTarget(4)
    public void onPre(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (active && rotations != null) {
                final float yaw = rotations.x;
                final float pitch = rotations.y;

                e.setYaw(yaw);
                e.setPitch(pitch);

                // 设置头部旋转
                mc.player.setYHeadRot(yaw);

                lastServerRotations = new Vector2f(yaw, pitch);

                // 检查是否已经接近目标旋转
                if (Math.abs((rotations.x - mc.player.getYRot()) % 360) < 1 && Math.abs((rotations.y - mc.player.getXRot())) < 1) {
                    active = false;
                    correctDisabledRotations();
                }

                lastRotations = rotations;
            } else {
                lastRotations = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
            }

            // 动画旋转处理
            lastAnimationRotation = animationRotation;
            Scaffold scaffold = Naven.getInstance().getModuleManager().getModule(Scaffold.class);
            if (scaffold.isEnabled() && scaffold.mode.isCurrentMode("Normal") && scaffold.snap.getCurrentValue() && scaffold.hideSnap.getCurrentValue()) {
                animationRotation = scaffold.correctRotation;
            } else {
                animationRotation = new Vector2f(e.getYaw(), e.getPitch());
            }

            targetRotations = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
            smoothed = false;
        }
    }

    /**
     * 修正禁用时的旋转
     */
    private static void correctDisabledRotations() {
        final Vector2f currentRotations = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
        final Vector2f fixedRotations = resetRotation(applySensitivityPatch(currentRotations, lastRotations));

        mc.player.setYRot(fixedRotations.x);
        mc.player.setXRot(fixedRotations.y);
    }

    @EventTarget
    public void onMove(EventMoveInput event) {
        if (active && rotations != null) {
            // 强制进行移动修复
            final float yaw = rotations.x;
            MoveUtils.fixMovement(event, yaw);
        }
    }

    @EventTarget
    public void onMove(EventRayTrace event) {
        if (rotations != null && event.entity == mc.player && active) {
            event.setYaw(rotations.x);
            event.setPitch(rotations.y);
        }
    }

    @EventTarget
    public void onItemRayTrace(EventUseItemRayTrace event) {
        if (rotations != null && active) {
            event.setYaw(rotations.x);
            event.setPitch(rotations.y);
        }
    }

    @EventTarget
    public void onStrafe(EventStrafe event) {
        if (active && rotations != null) {
            event.setYaw(rotations.x);
        }
    }

    @EventTarget
    public void onJump(EventJump event) {
        if (active && rotations != null) {
            event.setYaw(rotations.x);
        }
    }

    @EventTarget(0)
    public void onPositionItem(EventPositionItem e) {
        if (active && rotations != null) {
            PosRot packet = (PosRot) e.getPacket();
            PosRot newPacket = new PosRot(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), rotations.getX(), rotations.getY(), packet.isOnGround());
            e.setPacket(newPacket);
        }
    }

    @EventTarget
    public void onFallFlying(EventFallFlying e) {
        if (rotations != null) {
            e.setPitch(rotations.y);
        }
    }

    @EventTarget
    public void onAttack(EventAttackYaw e) {
        if (rotations != null) {
            e.setYaw(rotations.x);
        }
    }
}
