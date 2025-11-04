package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventFallFlying;
import cnm.obsoverlay.events.impl.EventJump;
import cnm.obsoverlay.events.impl.EventRotationAnimation;
import cnm.obsoverlay.modules.impl.render.AntiNausea;
import cnm.obsoverlay.modules.impl.render.FullBright;
import cnm.obsoverlay.utils.Wrapper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class LivingEntityTransformer extends ASMTransformer implements Wrapper {
    public LivingEntityTransformer() {
        super(LivingEntity.class);
    }

    public static float modifyJumpYaw(LivingEntity entity) {
        EventJump event = new EventJump(entity.getYRot());
        Naven.getInstance().getEventManager().call(event);
        return event.getYaw();
    }

    public static float hookModifyFallFlyingPitch(LivingEntity instance) {
        EventFallFlying event = new EventFallFlying(instance.getXRot());
        Naven.getInstance().getEventManager().call(event);
        return event.getPitch();
    }

    public static float modifyHeadYaw(LivingEntity entity) {
        if (entity == mc.player) {
            EventRotationAnimation event = new EventRotationAnimation(entity.getYRot(), 0.0F, 0.0F, 0.0F);
            Naven.getInstance().getEventManager().call(event);
            if (MixinLoader.debugging) {
                System.out.println("[ASM][LivingEntityTransformer] HeadYaw: orig=" + entity.getYRot() + " -> event=" + event.getYaw());
            }
            return event.getYaw();
        }
        return entity.getYRot();
    }

    public static Boolean overrideHasEffect(LivingEntity self, MobEffect effect) {
        if (self == mc.player) {
            FullBright fullBright = (FullBright) Naven.getInstance().getModuleManager().getModule(FullBright.class);
            if (effect == MobEffects.NIGHT_VISION && fullBright != null && fullBright.isEnabled()) {
                return Boolean.TRUE;
            }
            AntiNausea antiNausea = (AntiNausea) Naven.getInstance().getModuleManager().getModule(AntiNausea.class);
            if (effect == MobEffects.CONFUSION && antiNausea != null && antiNausea.isEnabled()) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    @Inject(method = "jumpFromGround", desc = "()V")
    private void injectJumpFromGround(MethodNode node) {
        String mappedGetYRot = Mapping.get(net.minecraft.world.entity.Entity.class, "getYRot", "()F");
        boolean replaced = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && (m.owner.equals(Type.getInternalName(LivingEntity.class)) || m.owner.equals(Type.getInternalName(net.minecraft.world.entity.Entity.class)))
                    && m.name.equals(mappedGetYRot)
                    && m.desc.equals("()F")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(LivingEntityTransformer.class),
                        "modifyJumpYaw",
                        "(Lnet/minecraft/world/entity/LivingEntity;)F",
                        false
                );
                node.instructions.set(m, redirect);
                replaced = true;
                break; // ordinal 0
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][LivingEntityTransformer] Failed to redirect getYRot in jumpFromGround");
        }
    }

    @Inject(method = "travel", desc = "(Lnet/minecraft/world/phys/Vec3;)V")
    private void injectTravel(MethodNode node) {
        String mappedGetXRot = Mapping.get(net.minecraft.world.entity.Entity.class, "getXRot", "()F");
        boolean replaced = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && (m.owner.equals(Type.getInternalName(LivingEntity.class)) || m.owner.equals(Type.getInternalName(net.minecraft.world.entity.Entity.class)))
                    && m.name.equals(mappedGetXRot)
                    && m.desc.equals("()F")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(LivingEntityTransformer.class),
                        "hookModifyFallFlyingPitch",
                        "(Lnet/minecraft/world/entity/LivingEntity;)F",
                        false
                );
                node.instructions.set(m, redirect);
                replaced = true;
                break;
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][LivingEntityTransformer] Failed to redirect getXRot in travel");
        }
    }

    @Inject(method = "hasEffect", desc = "(Lnet/minecraft/world/effect/MobEffect;)Z")
    private void injectHasEffect(MethodNode node) {
        if (node == null) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][LivingEntityTransformer] Target method not found: hasEffect (node is null)");
            }
            return;
        }
        InsnList list = new InsnList();
        LabelNode cont = new LabelNode();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new VarInsnNode(Opcodes.ALOAD, 1));
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(LivingEntityTransformer.class),
                "overrideHasEffect",
                "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffect;)Ljava/lang/Boolean;",
                false
        ));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new JumpInsnNode(Opcodes.IFNULL, cont));
        list.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(Boolean.class),
                "booleanValue",
                "()Z",
                false
        ));
        list.add(new InsnNode(Opcodes.IRETURN));
        list.add(cont);
        list.add(new InsnNode(Opcodes.POP));
        try {
            node.instructions.insert(list);
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][LivingEntityTransformer] Failed to insert hasEffect override: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Inject(method = "tickHeadTurn", desc = "(FF)V")
    private void injectTickHeadTurn(MethodNode node) {
        String mappedGetYRot = Mapping.get(LivingEntity.class, "getYRot", "()F");
        boolean replaced = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(LivingEntity.class))
                    && m.name.equals(mappedGetYRot)
                    && m.desc.equals("()F")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(LivingEntityTransformer.class),
                        "modifyHeadYaw",
                        "(Lnet/minecraft/world/entity/LivingEntity;)F",
                        false
                );
                node.instructions.set(m, redirect);
                replaced = true;
                // 不 break，允许替换多个调用
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][LivingEntityTransformer] Failed to redirect getYRot in tickHeadTurn");
        }
    }
}


