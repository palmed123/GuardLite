package cnm.obsoverlay.utils.skidRotation;

import cnm.obsoverlay.utils.ReflectUtil;
import cnm.obsoverlay.utils.math.MathConst;
import cnm.obsoverlay.utils.Wrapper;
import cnm.obsoverlay.utils.vector.Vector2d;
import cnm.obsoverlay.utils.vector.Vector2f;
import cnm.obsoverlay.utils.vector.Vector3d;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@UtilityClass
public class RotationUtil implements Wrapper {

    public Vector2f calculate(final Vector3d from, final Vector3d to) {
        final Vector3d diff = to.subtract(from);
        final double distance = Math.hypot(diff.getX(), diff.getZ());
        final float yaw = (float) (Mth.atan2(diff.getZ(), diff.getX()) * MathConst.TO_DEGREES) - 90.0F;
        final float pitch = (float) (-(Mth.atan2(diff.getY(), distance) * MathConst.TO_DEGREES));
        return new Vector2f(yaw, pitch);
    }

    public Vector2f calculate(final Entity entity) {
        return calculate(entityCustomPositionVector(entity).add(0, Math.max(0, Math.min(mc.player.getY() - entity.getY() +
                mc.player.getEyeHeight(), (entity.getBoundingBox().maxY - entity.getBoundingBox().minY) * 0.9)), 0));
    }

    public Vector2f calculate(final Entity entity, final boolean adaptive, final double range) {
        Vector2f normalRotations = calculate(entity);
        if (!adaptive) {
            return normalRotations;
        }

        // 检查标准旋转是否能打到实体
        Vector2f normalRotUtil = new Vector2f(normalRotations.x, normalRotations.y);
        net.minecraft.world.phys.HitResult normalHit = cnm.obsoverlay.utils.RayTraceUtils.rayCast((float)range, 0.0f, true, normalRotUtil);
        if (normalHit != null && normalHit.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
            return normalRotations;
        }

        // 遍历实体碰撞箱寻找最佳击中点
        for (double yPercent = 1; yPercent >= 0; yPercent -= 0.25 + Math.random() * 0.1) {
            for (double xPercent = 1; xPercent >= -0.5; xPercent -= 0.5) {
                for (double zPercent = 1; zPercent >= -0.5; zPercent -= 0.5) {
                    Vector2f adaptiveRotations = calculate(entityCustomPositionVector(entity).add(
                            (entity.getBoundingBox().maxX - entity.getBoundingBox().minX) * xPercent,
                            (entity.getBoundingBox().maxY - entity.getBoundingBox().minY) * yPercent,
                            (entity.getBoundingBox().maxZ - entity.getBoundingBox().minZ) * zPercent));

                    Vector2f adaptiveRotUtil = new Vector2f(adaptiveRotations.x, adaptiveRotations.y);
                    net.minecraft.world.phys.HitResult rayCastResult = cnm.obsoverlay.utils.RayTraceUtils.rayCast((float)range, 0.0f, true, adaptiveRotUtil);
                    if (rayCastResult != null && rayCastResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
                        return adaptiveRotations;
                    }
                }
            }
        }

        return normalRotations;
    }

    public Vector2f calculate(final Vec3 to, final Direction enumFacing) {
        return calculate(new Vector3d(to.x, to.y, to.z), enumFacing);
    }

    public Vector2f calculate(final Vec3 to) {
        return calculate(entityCustomPositionVector(mc.player).add(0, mc.player.getEyeHeight(), 0), new Vector3d(to.x, to.y, to.z));
    }

    public Vector2f calculate(final BlockPos to) {
        return calculate(entityCustomPositionVector(mc.player).add(0, mc.player.getEyeHeight(), 0), new Vector3d(to.getX(), to.getY(), to.getZ()).add(0.5, 0.5, 0.5));
    }

    public Vector2f calculate(final Vector3d to) {
        return calculate(entityCustomPositionVector(mc.player).add(0, mc.player.getEyeHeight(), 0), to);
    }

    public Vector2f calculate(final Vector3d position, final Direction enumFacing) {
        double x = position.getX() + 0.5D;
        double y = position.getY() + 0.5D;
        double z = position.getZ() + 0.5D;

        x += (double) enumFacing.getNormal().getX() * 0.5D;
        y += (double) enumFacing.getNormal().getY() * 0.5D;
        z += (double) enumFacing.getNormal().getZ() * 0.5D;
        return calculate(new Vector3d(x, y, z));
    }

    public Vector2f applySensitivityPatch(final Vector2f rotation) {
        final Vector2f previousRotation = entityPreviousRotation(mc.player);
        final float mouseSensitivity = (float) (mc.options.sensitivity().get() * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        final double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        final float yaw = previousRotation.x + (float) (Math.round((rotation.x - previousRotation.x) / multiplier) * multiplier);
        final float pitch = previousRotation.y + (float) (Math.round((rotation.y - previousRotation.y) / multiplier) * multiplier);
        return new Vector2f(yaw, Mth.clamp(pitch, -90, 90));
    }

    public Vector2f applySensitivityPatch(final Vector2f rotation, final Vector2f previousRotation) {
        final float mouseSensitivity = (float) (mc.options.sensitivity().get() * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        final double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        final float yaw = previousRotation.x + (float) (Math.round((rotation.x - previousRotation.x) / multiplier) * multiplier);
        final float pitch = previousRotation.y + (float) (Math.round((rotation.y - previousRotation.y) / multiplier) * multiplier);
        return new Vector2f(yaw, Mth.clamp(pitch, -90, 90));
    }

    public Vector2f relateToPlayerRotation(final Vector2f rotation) {
        final Vector2f previousRotation = entityPreviousRotation(mc.player);
        final float yaw = previousRotation.x + Mth.wrapDegrees(rotation.x - previousRotation.x);
        final float pitch = Mth.clamp(rotation.y, -90, 90);
        return new Vector2f(yaw, pitch);
    }

    public Vector2f resetRotation(final Vector2f rotation) {
        if (rotation == null) {
            return null;
        }

        final float yaw = rotation.x + Mth.wrapDegrees(mc.player.getYRot() - rotation.x);
        final float pitch = mc.player.getXRot();
        return new Vector2f(yaw, pitch);
    }

    public Vector2f move(final Vector2f targetRotation, final double speed) {
        Vector2f lastRot = cnm.obsoverlay.utils.rotation.RotationManager.lastRotations;
        Vector2f from = lastRot != null ? new Vector2f(lastRot.x, lastRot.y) : new Vector2f(mc.player.getYRot(), mc.player.getXRot());
        return move(from, targetRotation, speed);
    }

    public Vector2f move(final Vector2f lastRotation, final Vector2f targetRotation, double speed) {
        if (speed != 0) {

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

        return new Vector2f(0, 0);
    }

    public Vector2f smooth(final Vector2f targetRotation, final double speed) {
        Vector2f lastRot = cnm.obsoverlay.utils.rotation.RotationManager.lastRotations;
        Vector2f from = lastRot != null ? new Vector2f(lastRot.x, lastRot.y) : new Vector2f(mc.player.getYRot(), mc.player.getXRot());
        return smooth(from, targetRotation, speed);
    }

    public Vector2f smooth(final Vector2f lastRotation, final Vector2f targetRotation, final double speed) {
        float yaw = targetRotation.x;
        float pitch = targetRotation.y;
        final float lastYaw = lastRotation.x;
        final float lastPitch = lastRotation.y;

        if (speed != 0) {
            Vector2f move = move(targetRotation, speed);

            yaw = lastYaw + move.x;
            pitch = lastPitch + move.y;
            for (int i = 1; i <= (int) (mc.getFps() / 20f + Math.random() * 10); ++i) {

                if (Math.abs(move.x) + Math.abs(move.y) > 0.0001) {
                    yaw += (Math.random() - 0.5) / 1000;
                    pitch -= Math.random() / 200;
                }

                /*
                 * Fixing GCD
                 */
                final Vector2f rotations = new Vector2f(yaw, pitch);
                final Vector2f fixedRotations = RotationUtil.applySensitivityPatch(rotations);

                /*
                 * Setting rotations
                 */
                yaw = fixedRotations.x;
                pitch = Math.max(-90, Math.min(90, fixedRotations.y));
            }
        }

        return new Vector2f(yaw, pitch);
    }

    public Vector3d entityCustomPositionVector(Entity entity) {
        return new Vector3d(entity.position());
    }

    public Vector2f entityPreviousRotation(LocalPlayer player) {
        float lastYaw = (float) ReflectUtil.getFieldValue(player.getClass(), "yRotLast", player);
        float lastPitch = (float) ReflectUtil.getFieldValue(player.getClass(), "xRotLast", player);
        return new Vector2f(lastYaw, lastPitch);
    }
}