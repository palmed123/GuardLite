package cnm.obsoverlay.modules.impl.combat;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.*;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.modules.impl.misc.KillSay;
import cnm.obsoverlay.modules.impl.misc.Teams;
import cnm.obsoverlay.modules.impl.move.Blink;
import cnm.obsoverlay.modules.impl.move.Stuck;
import cnm.obsoverlay.modules.impl.render.HUD;
import cnm.obsoverlay.utils.*;
import cnm.obsoverlay.utils.math.MathUtils;
import cnm.obsoverlay.utils.renderer.Fonts;
import cnm.obsoverlay.utils.rotation.RotationManager;
import cnm.obsoverlay.utils.rotation.RotationUtils;
import cnm.obsoverlay.utils.vector.Vector2f;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import cnm.obsoverlay.values.impl.FloatValue;
import cnm.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@ModuleInfo(
        name = "KillAura",
        description = "Automatically attacks entities",
        category = Category.COMBAT
)
public class Aura extends Module {
    private static final float[] targetColorRed = new float[]{0.78431374F, 0.0F, 0.0F, 0.23529412F};
    private static final float[] targetColorGreen = new float[]{0.0F, 0.78431374F, 0.0F, 0.23529412F};
    public static Entity target;
    public static Entity aimingTarget;
    public static List<Entity> targets = new ArrayList<>();
    public static Vector2f rotation;
    public Color textColor = new Color(200, 200, 200, 255);

    private final AttackScheduler attackScheduler = new AttackScheduler();

    BooleanValue targetHud = ValueBuilder.create(this, "Target HUD").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue targetEsp = ValueBuilder.create(this, "Target ESP").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue attackPlayer = ValueBuilder.create(this, "Attack Player").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue attackInvisible = ValueBuilder.create(this, "Attack Invisible").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue attackAnimals = ValueBuilder.create(this, "Attack Animals").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue attackMobs = ValueBuilder.create(this, "Attack Mobs").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue multi = ValueBuilder.create(this, "Multi Attack").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue infSwitch = ValueBuilder.create(this, "Infinity Switch").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue preferBaby = ValueBuilder.create(this, "Prefer Baby").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue moreParticles = ValueBuilder.create(this, "More Particles").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue keepSprint = ValueBuilder.create(this, "Keep Sprint").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue autoDisable = ValueBuilder.create(this, "Auto Disable").setDefaultBooleanValue(false).build().getBooleanValue();
    FloatValue aimRange = ValueBuilder.create(this, "Aim Range")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(6.0F)
            .build()
            .getFloatValue();
    FloatValue aps = ValueBuilder.create(this, "APS")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();
    FloatValue switchSize = ValueBuilder.create(this, "Switch Size")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5.0F)
            .setVisibility(() -> !this.infSwitch.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue switchAttackTimes = ValueBuilder.create(this, "Switch Delay (Attack Times)")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    FloatValue fov = ValueBuilder.create(this, "FoV")
            .setDefaultFloatValue(360.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(10.0F)
            .setMaxFloatValue(360.0F)
            .build()
            .getFloatValue();
    FloatValue rotateSpeed = ValueBuilder.create(this, "Rotation Speed")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(180.0F)
            .build()
            .getFloatValue();
    FloatValue yawJitterRange = ValueBuilder.create(this, "Yaw Jitter Range")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.05F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(5.0F)
            .build()
            .getFloatValue();
    FloatValue pitchJitterRange = ValueBuilder.create(this, "Pitch Jitter Range")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.05F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(5.0F)
            .build()
            .getFloatValue();
    FloatValue yawJitterChance = ValueBuilder.create(this, "Yaw Jitter Chance")
            .setDefaultFloatValue(25.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(100.0F)
            .build()
            .getFloatValue();
    FloatValue pitchJitterChance = ValueBuilder.create(this, "Pitch Jitter Chance")
            .setDefaultFloatValue(25.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(100.0F)
            .build()
            .getFloatValue();
    FloatValue hurtTime = ValueBuilder.create(this, "Hurt Time")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    ModeValue priority = ValueBuilder.create(this, "Priority").setModes("Health", "FoV", "Range", "None").build().getModeValue();
    RotationUtils.Data lastRotationData;
    RotationUtils.Data rotationData;
    int attackTimes = 0;
    float attacks = 0.0F;
    private int index;
    private Vector4f blurMatrix;
    private SmoothAnimationTimer healthBar = new SmoothAnimationTimer(20);
    private SmoothAnimationTimer headSize = new SmoothAnimationTimer(100);
    private SmoothAnimationTimer headXY = new SmoothAnimationTimer(100);
    float yawJitter = 0;
    float pitchJitter = 0;

    @EventTarget
    public void onShader(EventShader e) {
        if (this.blurMatrix != null && this.targetHud.getCurrentValue()) {
            RenderUtils.drawRoundedRect(e.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), 3.0F, 1073741824);
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        this.blurMatrix = null;
        if (target instanceof LivingEntity living && this.targetHud.getCurrentValue()) {
            healthBar.target = living.getHealth() / living.getMaxHealth();
            if (target instanceof Player && ((Player) target).hurtTime > 5) {
                headSize.target = 4;
                headXY.target = 2;
            } else {
                headSize.target = 0;
                headXY.target = 0;
            }
            healthBar.update(true);
            headSize.update(true);
            headXY.update(true);
            e.getStack().pushPose();
            float x = (float) mc.getWindow().getGuiScaledWidth() / 2.0F + 10.0F;
            float y = (float) mc.getWindow().getGuiScaledHeight() / 2.0F + 10.0F;
            String targetName = target.getName().getString() + (living.isBaby() ? " (Baby)" : "");
            float width = Math.max(Fonts.MiSans_Medium.getWidth(targetName, 0.4F) + 46.0F, 125.0F);
            this.blurMatrix = new Vector4f(x, y, width, 40.0f);
            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(e.getStack(), x, y, width, 40.0f, 5.0F, HUD.headerColor);
            StencilUtils.erase(true);
            RenderUtils.fillBound(e.getStack(), x, y, 40.0f, 40.0f, HUD.headerColor);
            RenderUtils.fillBound(e.getStack(), x + 40.0f, y, width - 40.0f, 40.0f, HUD.bodyColor);
            StencilUtils.dispose();
            Fonts.MiSans_Medium.render(e.getStack(), targetName, x + 43.0f, y + 5.0F, Color.white, true, 0.35F);
            Fonts.MiSans_Medium
                    .render(
                            e.getStack(),
                            Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : "") + "Health",
                            x + 43.0f,
                            y + (28.0f - Fonts.MiSans_Medium.getHeight(true, 0.325f)),
                            textColor,
                            true,
                            0.325F
                    );

            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(e.getStack(), x + 43.0f, y + 30.0f, width - 46.0f, 5.0F, 2.0f, -1);
            StencilUtils.erase(true);
            RenderUtils.fillBound(e.getStack(), x + 43.0f, y + 30.0f, width - 46.0f, 5.0F, new Color(255, 255, 255, 75).getRGB());
            RenderUtils.fillBound(e.getStack(), x + 43.0f, y + 30.0f, (width - 46.0f) * healthBar.value, 5.0f, textColor.getRGB());
            StencilUtils.dispose();

            if (target instanceof AbstractClientPlayer player) {
                float headX = x + 3.0f + headXY.value;
                float headY = y + 3.0f + headXY.value;
                float headSize = 34.0f - this.headSize.value;
//                float headColor = 50.0f * hurtValue;
                StencilUtils.write(false);
                RenderUtils.drawRoundedRect(e.getStack(), headX, headY, headSize, headSize, 5.0F, -1);
                StencilUtils.erase(true);
                RenderUtils.drawPlayerHead(e.getStack(), headX, headY, headSize, headSize, player);
//                RenderUtils.fillBound(e.getStack(), (int) headX, (int) headY, (int) headSize, (int) headSize, new Color(255, 0, 0, headColor).getRGB());
                StencilUtils.dispose();

            }
            e.getStack().popPose();
        }
    }

    @EventTarget
    public void onRender(EventRender e) {
        if (this.targetEsp.getCurrentValue()) {
            PoseStack stack = e.getPMatrixStack();
            float partialTicks = e.getRenderPartialTicks();
            stack.pushPose();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glEnable(2848);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderUtils.applyRegionalRenderOffset(stack);

            for (Entity entity : targets) {
                if (entity instanceof LivingEntity living) {
                    float[] color = target == living ? targetColorRed : targetColorGreen;
                    stack.pushPose();
                    RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
                    double motionX = entity.getX() - entity.xo;
                    double motionY = entity.getY() - entity.yo;
                    double motionZ = entity.getZ() - entity.zo;
                    AABB boundingBox = entity.getBoundingBox()
                            .move(-motionX, -motionY, -motionZ)
                            .move((double) partialTicks * motionX, (double) partialTicks * motionY, (double) partialTicks * motionZ);
                    RenderUtils.drawSolidBox(boundingBox, stack);
                    stack.popPose();
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

    @Override
    public void onEnable() {
        rotation = null;
        this.index = 0;
        target = null;
        aimingTarget = null;
        targets.clear();
        headSize.speed = 0.5f;
        headXY.speed = 0.5f;
    }

    @Override
    public void onDisable() {
        target = null;
        aimingTarget = null;
        super.onDisable();
    }

    @EventTarget
    public void onRespawn(EventRespawn e) {
        target = null;
        aimingTarget = null;
        if (autoDisable.currentValue) this.toggle();
    }

    @EventTarget
    public void onAttackSlowdown(EventAttackSlowdown e) {
        if (keepSprint.getCurrentValue()) {
            e.setCancelled(true);
        }
    }

    @EventTarget
    public void onMotion(EventRunTicks event) {
        if (event.getType() == EventType.PRE && mc.player != null) {
            if (mc.screen instanceof AbstractContainerScreen
                    || Naven.getInstance().getModuleManager().getModule(Stuck.class).isEnabled()
                    || InventoryUtils.shouldDisableFeatures()) {
                target = null;
                aimingTarget = null;
                this.rotationData = null;
                rotation = null;
                this.lastRotationData = null;
                targets.clear();
                return;
            }

            boolean isSwitch = this.switchSize.getCurrentValue() > 1.0F;
            this.setSuffix(this.multi.getCurrentValue() ? "Multi" : (isSwitch ? "Switch" : "Single"));
            this.updateAttackTargets();
            aimingTarget = this.shouldPreAim();
            this.lastRotationData = this.rotationData;
            this.rotationData = null;
            if (aimingTarget != null) {
                this.rotationData = RotationUtils.getRotationDataToEntity(aimingTarget);
                if (this.rotationData.getRotation() != null) {
                    float targetYaw = this.rotationData.getRotation().x;
                    float targetPitch = this.rotationData.getRotation().y;

                    // 添加抖动
                    float yawRandomChance = (float) MathUtils.getRandomDoubleInRange(0.0, 100.0);
                    if (yawRandomChance < this.yawJitterChance.getCurrentValue()) {
                        yawJitter = (float) MathUtils.getRandomDoubleInRange(-this.yawJitterRange.getCurrentValue(), this.yawJitterRange.getCurrentValue());
                    }
                    float pitchRandomChance = (float) MathUtils.getRandomDoubleInRange(0.0, 100.0);
                    if (pitchRandomChance < this.pitchJitterChance.getCurrentValue()) {
                        pitchJitter = (float) MathUtils.getRandomDoubleInRange(-this.pitchJitterRange.getCurrentValue(), this.pitchJitterRange.getCurrentValue());
                    }

                    targetYaw += yawJitter;
                    targetPitch += pitchJitter;

                    // 计算旋转速度
                    float rotateSpeed = this.rotateSpeed.getCurrentValue();
                    HitResult hitResult = mc.hitResult;
                    if (hitResult.getType() == HitResult.Type.ENTITY) {
                        EntityHitResult result = (EntityHitResult) hitResult;
                        if (aimingTarget == result.getEntity()) {
                            rotateSpeed = rotateSpeed / 10.0F;
                        }
                    }

                    // 使用 RotationManager 进行平滑转头
                    Vector2f targetRotation = new Vector2f(targetYaw, targetPitch);
                    RotationManager.setRotations(targetRotation, rotateSpeed);
                    rotation = RotationManager.rotations;
                } else {
                    rotation = null;
                }
            } else {
                rotation = null;
            }

            if (targets.isEmpty()) {
                target = null;
                return;
            }

            if (this.index > targets.size() - 1) {
                this.index = 0;
            }

            if (targets.size() > 1
                    && ((float) this.attackTimes >= this.switchAttackTimes.getCurrentValue() || this.rotationData != null && this.rotationData.getDistance() > 3.0)) {
                this.attackTimes = 0;

                for (int i = 0; i < targets.size(); i++) {
                    this.index++;
                    if (this.index > targets.size() - 1) {
                        this.index = 0;
                    }

                    Entity nextTarget = targets.get(this.index);
                    RotationUtils.Data data = RotationUtils.getRotationDataToEntity(nextTarget);
                    if (data.getDistance() < 3.0) {
                        break;
                    }
                }
            }

            if (this.index > targets.size() - 1 || !isSwitch) {
                this.index = 0;
            }

            target = targets.get(this.index);

            if (this.attacks < 1.0F) {
                this.attacks = Math.min(1.0F, this.attacks + this.aps.getCurrentValue() / 20.0F);
            }
        }
    }

    @EventTarget
    public void onClick(EventClick e) {
        if (mc.player.getUseItem().isEmpty()
                && mc.screen == null
                && !NetworkUtils.isServerLag()
                && !Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled()) {

            if (attackScheduler.isClickTick()) {
                attackScheduler.attack(RotationManager.rotations, () -> {
                    if (!validateAttackConditions()) {
                        return false;
                    }

                    Entity target = shouldPreAim();
                    if (target == null) {
                        return false;
                    }

                    if (!isValidAttack(target)) {
                        return false;
                    }

                    boolean attacked = false;
                    if (this.attacks >= 1.0F) {
                        this.doAttack();
                        this.attacks--;
                        attacked = true;
                    }

                    return attacked;
                });
            }
        }
    }

    private boolean validateAttackConditions() {
        if (mc.player == null) return false;
        if (targets.isEmpty()) return false;

        return this.attacks >= 1.0F;
    }

    public Entity shouldPreAim() {
        Entity target = Aura.target;
        if (target == null) {
            List<Entity> aimTargets = this.getTargets();
            if (!aimTargets.isEmpty()) {
                target = aimTargets.get(0);
            }
        }

        return target;
    }

    public void doAttack() {
        if (!targets.isEmpty()) {
            Entity target = getClosestTarget();
            if (target == null) return;

            double distance = mc.player.distanceTo(target);
            double attackRange = 3.0;
            if (distance > attackRange) {
                return;
            }

            if (this.multi.getCurrentValue()) {
                int attacked = 0;

                for (Entity entity : targets) {
                    if (RotationUtils.getDistance(entity, mc.player.getEyePosition(), RotationManager.rotations) < 3.0) {
                        this.attackEntity(entity);
                        if (++attacked >= 2) {
                            break;
                        }
                    }
                }
            } else {
                this.attackEntity(target);
            }
        }
    }

    public void updateAttackTargets() {
        targets = this.getTargets();
    }

    private Entity getClosestTarget() {
        if (targets.isEmpty()) return null;

        return targets.stream()
                .min(Comparator.comparingDouble(entity -> mc.player.distanceTo(entity)))
                .orElse(null);
    }

    public boolean isValidTarget(Entity entity) {
        if (entity == mc.player) {
            return false;
        } else if (entity instanceof LivingEntity living) {
            if (living instanceof BlinkingPlayer) {
                return false;
            } else {
                AntiBots module = (AntiBots) Naven.getInstance().getModuleManager().getModule(AntiBots.class);
                if (module == null || !module.isEnabled() || !AntiBots.isBot(entity) && !AntiBots.isBedWarsBot(entity)) {
                    if (Teams.isSameTeam(living)) {
                        return false;
                    } else if (FriendManager.isFriend(living)) {
                        return false;
                    } else if (living.isDeadOrDying() || living.getHealth() <= 0.0F) {
                        return false;
                    } else if (entity instanceof ArmorStand) {
                        return false;
                    } else if (entity.isInvisible() && !this.attackInvisible.getCurrentValue()) {
                        return false;
                    } else if (entity instanceof Player && !this.attackPlayer.getCurrentValue()) {
                        return false;
                    } else if (!(entity instanceof Player) || !((double) entity.getBbWidth() < 0.5) && !living.isSleeping()) {
                        if ((entity instanceof Mob || entity instanceof Slime || entity instanceof Bat || entity instanceof AbstractGolem)
                                && !this.attackMobs.getCurrentValue()) {
                            return false;
                        } else if ((entity instanceof Animal || entity instanceof Squid) && !this.attackAnimals.getCurrentValue()) {
                            return false;
                        } else {
                            return (!(entity instanceof Villager) || this.attackAnimals.getCurrentValue()) && (!(entity instanceof Player) || !entity.isSpectator());
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public boolean isValidAttack(Entity entity) {
        if (!this.isValidTarget(entity)) {
            return false;
        } else if (entity instanceof LivingEntity && (float) ((LivingEntity) entity).hurtTime > this.hurtTime.getCurrentValue()) {
            return false;
        } else {
            Vec3 closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePosition(), entity.getBoundingBox());
            return !(closestPoint.distanceTo(mc.player.getEyePosition()) > (double) this.aimRange.getCurrentValue()) && RotationUtils.inFoV(entity, this.fov.getCurrentValue() / 2.0F);
        }
    }

    public void attackEntity(Entity entity) {
        this.attackTimes++;
        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();
        mc.player.setYRot(RotationManager.rotations.x);
        mc.player.setXRot(RotationManager.rotations.y);
        if (entity instanceof Player && !AntiBots.isBot(entity)) {
            KillSay.attackedPlayers.add(entity.getName().getString());
        }

        mc.gameMode.attack(mc.player, entity);
        mc.player.swing(InteractionHand.MAIN_HAND);
        if (this.moreParticles.getCurrentValue()) {
            mc.player.magicCrit(entity);
            mc.player.crit(entity);
        }

        mc.player.setYRot(currentYaw);
        mc.player.setXRot(currentPitch);
    }

    private List<Entity> getTargets() {
        Stream<Entity> stream = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), true)
                .filter(entity -> entity instanceof Entity)
                .filter(this::isValidAttack);
        List<Entity> possibleTargets = stream.collect(Collectors.toList());
        if (this.priority.isCurrentMode("Range")) {
            possibleTargets.sort(Comparator.comparingDouble(o -> (double) o.distanceTo(mc.player)));
        } else if (this.priority.isCurrentMode("FoV")) {
            possibleTargets.sort(
                    Comparator.comparingDouble(o -> (double) RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x))
            );
        } else if (this.priority.isCurrentMode("Health")) {
            possibleTargets.sort(Comparator.comparingDouble(o -> o instanceof LivingEntity living ? (double) living.getHealth() : 0.0));
        }

        if (this.preferBaby.getCurrentValue() && possibleTargets.stream().anyMatch(entity -> entity instanceof LivingEntity && ((LivingEntity) entity).isBaby())) {
            possibleTargets.removeIf(entity -> !(entity instanceof LivingEntity) || !((LivingEntity) entity).isBaby());
        }

        possibleTargets.sort(Comparator.comparing(o -> o instanceof EndCrystal ? 0 : 1));
        return this.infSwitch.getCurrentValue()
                ? possibleTargets
                : possibleTargets.subList(0, (int) Math.min((float) possibleTargets.size(), this.switchSize.getCurrentValue()));
    }

    private class AttackScheduler {
        private final TimeHelper timer = new TimeHelper();
        private long lastAttackTime = 0;

        public boolean isClickTick() {
            return timer.delay((long)(1000.0f / Aura.this.aps.getCurrentValue()));
        }

        public boolean attack(Vector2f rotation, AttackAction attackAction) {
            boolean success = attackAction.execute();
            if (success) {
                timer.reset();
                lastAttackTime = System.currentTimeMillis();
            }
            return success;
        }

        public boolean willClickAt(int ticks) {
            long attackInterval = (long)(1000.0f / Aura.this.aps.getCurrentValue());
            long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
            return timeSinceLastAttack + (ticks * 50) >= attackInterval;
        }
    }

    @FunctionalInterface
    private interface AttackAction {
        boolean execute();
    }
}
