package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventRayTrace;
import cnm.obsoverlay.events.impl.EventStrafe;
import cnm.obsoverlay.events.impl.EventStuckInBlock;
import cnm.obsoverlay.utils.BlinkingPlayer;
import cnm.obsoverlay.utils.ReflectUtil;
import cnm.obsoverlay.utils.Wrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class EntityTransformer extends ASMTransformer implements Wrapper {
    public EntityTransformer() {
        super(Entity.class);
    }

    // 移除除 getViewVector 之外的所有注入，专注于视线事件
    // 替代 getViewVector 中对 calculateViewVector 的直接调用
    public static Vec3 computeViewVector(Entity self, float pitch, float yaw) {
        if (self == mc.player) {
            try {
                EventRayTrace lookEvent = new EventRayTrace(self, yaw, pitch);
                Naven.getInstance().getEventManager().call(lookEvent);
                yaw = lookEvent.yaw;
                pitch = lookEvent.pitch;
            } catch (Throwable t) {
                // ignore
            }
        }
        try {
            java.lang.reflect.Method calc = ReflectUtil.getMethod(Entity.class, "calculateViewVector", "(FF)Lnet/minecraft/world/phys/Vec3;", float.class, float.class);
            if (calc != null) {
                Object out = calc.invoke(self, pitch, yaw);
                if (out instanceof Vec3) {
                    return (Vec3) out;
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        throw new RuntimeException("Entity.calculateViewVector reflection failed or returned incompatible type; check Mapping for calculateViewVector(FF)");
    }

    public static Vec3 getViewVectorReflective(Entity self, float partialTicks) {
        float pitch;
        float yaw;
        try {
            java.lang.reflect.Method mX = ReflectUtil.getMethod(Entity.class, "getViewXRot", "(F)F", float.class);
            java.lang.reflect.Method mY = ReflectUtil.getMethod(Entity.class, "getViewYRot", "(F)F", float.class);
            if (mX == null || mY == null) {
                throw new RuntimeException("Failed to resolve getViewXRot/getViewYRot via mapping");
            }
            pitch = (float) (Float) mX.invoke(self, partialTicks);
            yaw = (float) (Float) mY.invoke(self, partialTicks);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return computeViewVector(self, pitch, yaw);
    }

    // 等价 @ModifyArg(index=2, ordinal=0) Entity.moveRelative -> Entity.getInputVector(..., float f1, float yaw)
    public static float modifyYaw(float yaw) {
        EventStrafe strafe = new EventStrafe(yaw);
        Naven.getInstance().getEventManager().call(strafe);
        return strafe.getYaw();
    }

    // === makeStuckInBlock tail injection (RETURN) with reflective field set ===
    public static void onStuckTail(Entity self, BlockState state, Vec3 multiplier) {
        try {
            if (mc.player == self) {
                EventStuckInBlock event = new EventStuckInBlock(state, multiplier);
                Naven.getInstance().getEventManager().call(event);
                if (event.isCancelled()) {
                    ReflectUtil.setFieldValue(Entity.class, "stuckSpeedMultiplier", self, Vec3.ZERO);
                    return;
                }
                ReflectUtil.setFieldValue(Entity.class, "stuckSpeedMultiplier", self, event.getStuckSpeedMultiplier());
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    @Inject(method = "getViewVector", desc = "(F)Lnet/minecraft/world/phys/Vec3;")
    private void injectGetViewVector(MethodNode node) {
        if (node == null) {
            return;
        }
        try {
            InsnList body = new InsnList();
            body.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            body.add(new VarInsnNode(Opcodes.FLOAD, 1)); // partialTicks
            body.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(EntityTransformer.class),
                    "getViewVectorReflective",
                    "(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/world/phys/Vec3;",
                    false
            ));
            body.add(new InsnNode(Opcodes.ARETURN));
            node.instructions.clear();
            node.instructions.add(body);
        } catch (Throwable t) {
            // ignore
        }
    }

    @Inject(method = "moveRelative", desc = "(FLnet/minecraft/world/phys/Vec3;)V")
    private void injectMoveRelative_ModifyYaw(MethodNode node) {
        if (node == null) {
            return;
        }
        String mappedGetInputVector = Mapping.get(Entity.class, "getInputVector", "(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;");
        boolean inserted = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKESTATIC
                    && m.owner.equals(Type.getInternalName(Entity.class))
                    && m.name.equals(mappedGetInputVector)
                    && m.desc.equals("(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;")) {
                // 栈顶顺序: ..., Vec3, float f1, float yaw
                // 直接把 yaw 传给 modifyYaw 并替换为返回值
                MethodInsnNode call = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(EntityTransformer.class),
                        "modifyYaw",
                        "(F)F",
                        false
                );
                try {
                    node.instructions.insertBefore(m, call);
                    inserted = true;
                } catch (Throwable t) {
                    // ignore
                }
                break; // 只处理 ordinal=0
            }
        }
    }

    @Inject(method = "makeStuckInBlock", desc = "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V")
    private void injectMakeStuckInBlock_Tail(MethodNode node) {
        if (node == null) {
            return;
        }
        boolean inserted = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                InsnList list = new InsnList();
                list.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                list.add(new VarInsnNode(Opcodes.ALOAD, 1)); // BlockState
                list.add(new VarInsnNode(Opcodes.ALOAD, 2)); // Vec3 multiplier
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(EntityTransformer.class),
                        "onStuckTail",
                        "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V",
                        false
                ));
                node.instructions.insertBefore(insn, list);
                inserted = true;
            }
        }
    }

    // === push head cancellation for BlinkingPlayer ===
    @Inject(method = "push", desc = "(Lnet/minecraft/world/entity/Entity;)V")
    private void injectPush_CancelBlink(MethodNode node) {
        if (node == null) {
            return;
        }
        boolean inserted = false;
        InsnList list = new InsnList();
        LabelNode cont = new LabelNode();
        list.add(new VarInsnNode(Opcodes.ALOAD, 1));
        list.add(new TypeInsnNode(Opcodes.INSTANCEOF, Type.getInternalName(BlinkingPlayer.class)));
        list.add(new JumpInsnNode(Opcodes.IFEQ, cont));
        list.add(new InsnNode(Opcodes.RETURN));
        list.add(cont);
        try {
            node.instructions.insert(list);
            inserted = true;
        } catch (Throwable t) {
            // ignore
        }
    }

}


