package cnm.obsoverlay.modules.impl.render;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.*;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.modules.impl.misc.Teams;
import cnm.obsoverlay.ui.notification.Notification;
import cnm.obsoverlay.ui.notification.NotificationLevel;
import cnm.obsoverlay.utils.*;
import cnm.obsoverlay.utils.math.MathUtils;
import cnm.obsoverlay.utils.renderer.Fonts;
import cnm.obsoverlay.utils.renderer.text.CustomTextRenderer;
import cnm.obsoverlay.utils.rotation.RotationUtils;
import cnm.obsoverlay.utils.vector.Vector2f;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import cnm.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(
        name = "NameTags",
        category = Category.RENDER,
        description = "Renders name tags"
)
public class NameTags extends Module {
    private static final int color1 = new Color(0, 0, 0, 40).getRGB();
    private static final int color2 = new Color(0, 0, 0, 80).getRGB();
    private final Map<Entity, Vector2f> entityPositions = new ConcurrentHashMap<>();
    private final List<NameTagData> sharedPositions = new CopyOnWriteArrayList<>();
    private final Map<Player, Integer> aimTicks = new ConcurrentHashMap<>();
    public BooleanValue mcf = ValueBuilder.create(this, "Middle Click Friend").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue showCompassPosition = ValueBuilder.create(this, "Compass Position").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue compassOnly = ValueBuilder.create(this, "Compass Only")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.showCompassPosition.getCurrentValue())
            .build()
            .getBooleanValue();
    public BooleanValue noPlayerOnly = ValueBuilder.create(this, "No Player Only")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.showCompassPosition.getCurrentValue())
            .build()
            .getBooleanValue();
//    public BooleanValue shared = ValueBuilder.create(this, "Shared ESP").setDefaultBooleanValue(true).build().getBooleanValue();
    public FloatValue scale = ValueBuilder.create(this, "Scale")
            .setDefaultFloatValue(0.3F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(0.5F)
            .build()
            .getFloatValue();
    List<Vector4f> blurMatrices = new ArrayList<>();
    private BlockPos spawnPosition;
    private Vector2f compassPosition;
    private Player aimingPlayer;

    public static boolean isAiming(Entity targetEntity, float yaw, float pitch) {
        Vec3 playerEye = new Vec3(mc.player.getX(), mc.player.getY() + (double) mc.player.getEyeHeight(), mc.player.getZ());
        HitResult intercept = RotationUtils.getIntercept(targetEntity.getBoundingBox(), new Vector2f(yaw, pitch), playerEye, 150.0);
        if (intercept == null) {
            return false;
        } else {
            return intercept.getType() == Type.ENTITY && intercept.getLocation().distanceTo(playerEye) < 150.0;
        }
    }

    private boolean hasPlayer() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity != mc.player && !(entity instanceof BlinkingPlayer) && entity instanceof Player) {
                return true;
            }
        }

        return false;
    }

    private BlockPos getSpawnPosition(ClientLevel p_117922_) {
        return p_117922_.dimensionType().natural() ? p_117922_.getSharedSpawnPos() : null;
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (!this.mcf.getCurrentValue()) {
                this.aimingPlayer = null;
            } else {
                for (Player player : mc.level.players()) {
                    if (!(player instanceof BlinkingPlayer) && player != mc.player) {
                        if (isAiming(player, mc.player.getYRot(), mc.player.getXRot())) {
                            if (this.aimTicks.containsKey(player)) {
                                this.aimTicks.put(player, this.aimTicks.get(player) + 1);
                            } else {
                                this.aimTicks.put(player, 1);
                            }

                            if (this.aimTicks.get(player) >= 10) {
                                this.aimingPlayer = player;
                                break;
                            }
                        } else if (this.aimTicks.containsKey(player) && this.aimTicks.get(player) > 0) {
                            this.aimTicks.put(player, this.aimTicks.get(player) - 1);
                        } else {
                            this.aimTicks.put(player, 0);
                        }
                    }
                }

                if (this.aimingPlayer != null && this.aimTicks.containsKey(this.aimingPlayer) && this.aimTicks.get(this.aimingPlayer) <= 0) {
                    this.aimingPlayer = null;
                }
            }

            this.spawnPosition = null;
            if (!InventoryUtils.hasItem(Items.COMPASS) && this.compassOnly.getCurrentValue()) {
                return;
            }

            if (this.hasPlayer() && this.noPlayerOnly.getCurrentValue()) {
                return;
            }

            this.spawnPosition = this.getSpawnPosition(mc.level);
        }
    }

    @EventTarget
    public void onShader(EventShader e) {
        for (Vector4f blurMatrix : this.blurMatrices) {
            RenderUtils.drawRoundedRect(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), scale.getCurrentValue(), HUD.bodyColor);
        }
    }

    @EventTarget
    public void update(EventRender e) {
        try {
            this.updatePositions(e.getRenderPartialTicks());
            this.compassPosition = null;
            if (this.spawnPosition != null) {
                this.compassPosition = ProjectionUtils.project(
                        (double) this.spawnPosition.getX() + 0.5,
                        (double) this.spawnPosition.getY() + 1.75,
                        (double) this.spawnPosition.getZ() + 0.5,
                        e.getRenderPartialTicks()
                );
            }
        } catch (Exception var3) {
        }
    }

    @EventTarget
    public void onMouseKey(EventMouseClick e) {
        if (e.getKey() == 2 && !e.isState() && this.mcf.getCurrentValue() && this.aimingPlayer != null) {
            if (FriendManager.isFriend(this.aimingPlayer)) {
                Notification notification = new Notification(
                        NotificationLevel.ERROR, "Removed " + this.aimingPlayer.getName().getString() + " from friends!", 3000L
                );
                Naven.getInstance().getNotificationManager().addNotification(notification);
                FriendManager.removeFriend(this.aimingPlayer);
            } else {
                Notification notification = new Notification(NotificationLevel.SUCCESS, "Added " + this.aimingPlayer.getName().getString() + " as friends!", 3000L);
                Naven.getInstance().getNotificationManager().addNotification(notification);
                FriendManager.addFriend(this.aimingPlayer);
            }
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        this.blurMatrices.clear();
        CustomTextRenderer MiSans_Medium = Fonts.MiSans_Medium;
        if (this.compassPosition != null) {
            Vector2f position = this.compassPosition;
            float scale = this.scale.getCurrentValue();
            String text = "Compass";
            float width = MiSans_Medium.getWidth(text, scale) + scale * 20.0f;
            float height = (float) Fonts.MiSans_Medium.getHeight(true, scale) + scale * 20.0f;
            float startX = position.x - width / 2.0F - 2.0F;
            float startY = position.y - 2.0F;
            RenderUtils.drawRoundedRect(e.getStack(), startX, startY, width, height, scale * 10.0f, HUD.bodyColor);
            blurMatrices.add(new Vector4f(startX, startY, width, height));
            MiSans_Medium.render(e.getStack(), text, startX + scale * 10.0f, startY + scale * 10.0f, Color.white, true, scale);
        }

        for (Entry<Entity, Vector2f> entry : this.entityPositions.entrySet()) {
            if (entry.getKey() != mc.player && entry.getKey() instanceof Player living) {
                e.getStack().pushPose();
                float hp = living.getHealth();
                if (hp > 20.0F) {
                    living.setHealth(20.0F);
                }

                Vector2f position = entry.getValue();
                String prefix = "";
                if (Teams.isSameTeam(living)) {
                    prefix = prefix + "§aTeam ";
                }

                if (FriendManager.isFriend(living)) {
                    prefix = prefix + "§aFriend ";
                }

                if (this.aimingPlayer == living) {
                    prefix = prefix + "§cAiming ";
                }

                String name = living.getName().getString();
                String health = "§c" + Math.round(hp) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : "") + "Health";
                float scale = this.scale.getCurrentValue();
                float prefixWidth = 0;
                if (!prefix.isEmpty())
                    prefixWidth = MiSans_Medium.getWidth(prefix, scale) + scale * 20.0f - MiSans_Medium.getWidth(" ", scale);
                float nameWidth = MiSans_Medium.getWidth(name, scale) + scale * 20.0f;
                float healthWidth = MiSans_Medium.getWidth(health, scale) + scale * 20.0f;
                float width = prefixWidth + scale * 10.0f + nameWidth + scale * 10.0f + healthWidth;
                float height = (float) Fonts.MiSans_Medium.getHeight(true, scale) + scale * 20.0f;
                float startX = position.x - width / 2.0F - 2.0F;
                float startY = position.y - 2.0F;

                RenderUtils.drawRoundedRect(e.getStack(), startX, startY, prefixWidth, height, scale * 10.0f, HUD.bodyColor);
                blurMatrices.add(new Vector4f(startX, startY, prefixWidth, height));
                MiSans_Medium.render(e.getStack(), prefix, startX + scale * 10.0f, startY + scale * 10.0f, Color.white, true, scale);
                startX += prefixWidth + scale * 10.0f;

                RenderUtils.drawRoundedRect(e.getStack(), startX, startY, nameWidth, height, scale * 10.0f, HUD.bodyColor);
                blurMatrices.add(new Vector4f(startX, startY, nameWidth, height));
                MiSans_Medium.render(e.getStack(), name, startX + scale * 10.0f, startY + scale * 10.0f, Color.white, true, scale);
                startX += nameWidth + scale * 10.0f;

                RenderUtils.drawRoundedRect(e.getStack(), startX, startY, healthWidth, height, scale * 10.0f, HUD.bodyColor);
                blurMatrices.add(new Vector4f(startX, startY, healthWidth, height));
                MiSans_Medium.render(e.getStack(), health, startX + scale * 10.0f, startY + scale * 10.0f, Color.white, true, scale);
                startX += healthWidth;

                e.getStack().popPose();
            }
        }

//        if (this.shared.getCurrentValue()) {
//            for (NameTagData data : this.sharedPositions) {
//                e.getStack().pushPose();
//                Vector2f positionx = data.render();
//                String textx = "§aShared§f | " + data.displayName();
//                float scale = this.scale.getCurrentValue();
//                float width = Fonts.MiSans_Medium.getWidth(textx, scale);
//                double delta = 1.0 - data.health() / data.maxHealth();
//                double height = Fonts.MiSans_Medium.getHeight(true, scale);
//                this.blurMatrices
//                        .add(
//                                new Vector4f(positionx.x - width / 2.0F - 2.0F, positionx.y - 2.0F, positionx.x + width / 2.0F + 2.0F, (float) ((double) positionx.y + height))
//                        );
//                RenderUtils.fill(
//                        e.getStack(),
//                        positionx.x - width / 2.0F - 2.0F,
//                        positionx.y - 2.0F,
//                        positionx.x + width / 2.0F + 2.0F,
//                        (float) ((double) positionx.y + height),
//                        color1
//                );
//                RenderUtils.fill(
//                        e.getStack(),
//                        positionx.x - width / 2.0F - 2.0F,
//                        positionx.y - 2.0F,
//                        (float) ((double) (positionx.x + width / 2.0F + 2.0F) - (double) (width + 4.0F) * delta),
//                        (float) ((double) positionx.y + height),
//                        color2
//                );
//                Fonts.MiSans_Medium.setAlpha(0.8F);
//                Fonts.MiSans_Medium.render(e.getStack(), textx, positionx.x - width / 2.0F, positionx.y - 1.0F, Color.WHITE, true, scale);
//                Fonts.MiSans_Medium.setAlpha(1.0F);
//                e.getStack().popPose();
//            }
//        }
    }

    private void updatePositions(float renderPartialTicks) {
        this.entityPositions.clear();
        this.sharedPositions.clear();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player && !entity.getName().getString().startsWith("CIT-")) {
                double x = MathUtils.interpolate(renderPartialTicks, entity.xo, entity.getX());
                double y = MathUtils.interpolate(renderPartialTicks, entity.yo, entity.getY()) + (double) entity.getBbHeight() + 0.5;
                double z = MathUtils.interpolate(renderPartialTicks, entity.zo, entity.getZ());
                Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
                vector.setY(vector.getY() - 2.0F);
                this.entityPositions.put(entity, vector);
            }
        }

//        if (this.shared.getCurrentValue()) {
//            Map<String, SharedESPData> dataMap = EntityWatcher.getSharedESPData();
//
//            for (SharedESPData value : dataMap.values()) {
//                double x = value.getPosX();
//                double y = value.getPosY() + (double) mc.player.getBbHeight() + 0.5;
//                double z = value.getPosZ();
//                Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
//                vector.setY(vector.getY() - 2.0F);
//                String displayName = value.getDisplayName();
//                displayName = displayName
//                        + "§f | §c"
//                        + Math.round(value.getHealth())
//                        + (value.getAbsorption() > 0.0 ? "+" + Math.round(value.getAbsorption()) : "")
//                        + "HP";
//                this.sharedPositions
//                        .add(new NameTagData(displayName, value.getHealth(), value.getMaxHealth(), value.getAbsorption(), new Vec3(x, y, z), vector));
//            }
//        }
    }

    private record NameTagData(String displayName, double health, double maxHealth, double absorption, Vec3 position,
                               Vector2f render) {

        @Override
            public boolean equals(Object o) {
                if (o == this) {
                    return true;
                } else if (!(o instanceof NameTagData other)) {
                    return false;
                } else if (!other.canEqual(this)) {
                    return false;
                } else if (Double.compare(this.health(), other.health()) != 0) {
                    return false;
                } else if (Double.compare(this.maxHealth(), other.maxHealth()) != 0) {
                    return false;
                } else if (Double.compare(this.absorption(), other.absorption()) != 0) {
                    return false;
                } else {
                    Object this$displayName = this.displayName();
                    Object other$displayName = other.displayName();
                    if (Objects.equals(this$displayName, other$displayName)) {
                        Object this$position = this.position();
                        Object other$position = other.position();
                        if (Objects.equals(this$position, other$position)) {
                            Object this$render = this.render();
                            Object other$render = other.render();
                            return Objects.equals(this$render, other$render);
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }

            protected boolean canEqual(Object other) {
                return other instanceof NameTagData;
            }

            @Override
            public int hashCode() {
                int PRIME = 59;
                int result = 1;
                long $health = Double.doubleToLongBits(this.health());
                result = result * 59 + (int) ($health >>> 32 ^ $health);
                long $maxHealth = Double.doubleToLongBits(this.maxHealth());
                result = result * 59 + (int) ($maxHealth >>> 32 ^ $maxHealth);
                long $absorption = Double.doubleToLongBits(this.absorption());
                result = result * 59 + (int) ($absorption >>> 32 ^ $absorption);
                Object $displayName = this.displayName();
                result = result * 59 + ($displayName == null ? 43 : $displayName.hashCode());
                Object $position = this.position();
                result = result * 59 + ($position == null ? 43 : $position.hashCode());
                Object $render = this.render();
                return result * 59 + ($render == null ? 43 : $render.hashCode());
            }

            @Override
            public String toString() {
                return "NameTags.NameTagData(displayName="
                        + this.displayName()
                        + ", health="
                        + this.health()
                        + ", maxHealth="
                        + this.maxHealth()
                        + ", absorption="
                        + this.absorption()
                        + ", position="
                        + this.position()
                        + ", render="
                        + this.render()
                        + ")";
            }
        }
}
