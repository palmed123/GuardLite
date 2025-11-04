package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventAttackSlowdown;
import cnm.obsoverlay.events.impl.EventAttackYaw;
import cnm.obsoverlay.events.impl.EventStayingOnGroundSurface;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class PlayerTransformer extends ASMTransformer {
    public PlayerTransformer() {
        super(Player.class);
    }

    public static float hookFixRotation(Player instance) {
        EventAttackYaw event = new EventAttackYaw(instance.getYRot());
        Naven.getInstance().getEventManager().call(event);
        return event.getYaw();
    }

    public static void hookSetDeltaMovement(Player instance, Vec3 vec3) {
        EventAttackSlowdown event = new EventAttackSlowdown();
        Naven.getInstance().getEventManager().call(event);
        if (!event.isCancelled()) {
            instance.setDeltaMovement(vec3);
        }
    }

    public static void hookSetSprinting(Player instance, boolean sprinting) {
        EventAttackSlowdown event = new EventAttackSlowdown();
        Naven.getInstance().getEventManager().call(event);
        if (!event.isCancelled()) {
            instance.setSprinting(sprinting);
        }
    }

    public static boolean overrideIsStaying(boolean original) {
        EventStayingOnGroundSurface event = new EventStayingOnGroundSurface(original);
        Naven.getInstance().getEventManager().call(event);
        return event.isStay();
    }

    @Inject(method = "attack", desc = "(Lnet/minecraft/world/entity/Entity;)V")
    private void injectAttack(MethodNode node) {
        if (node == null) {
            return;
        }
        String mappedGetYRot = Mapping.get(net.minecraft.world.entity.Entity.class, "getYRot", "()F");

        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.name.equals(mappedGetYRot)
                    && m.desc.equals("()F")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(PlayerTransformer.class),
                        "hookFixRotation",
                        "(Lnet/minecraft/world/entity/player/Player;)F",
                        false
                );
                node.instructions.set(m, redirect);
            }
        }
    }

    @Inject(method = "attack", desc = "(Lnet/minecraft/world/entity/Entity;)V")
    private void injectAttack_DeltaMovement(MethodNode node) {
        if (node == null) {
            return;
        }
        String mappedSetDelta = Mapping.get(net.minecraft.world.entity.Entity.class, "setDeltaMovement", "(Lnet/minecraft/world/phys/Vec3;)V");
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.name.equals(mappedSetDelta)
                    && m.desc.equals("(Lnet/minecraft/world/phys/Vec3;)V")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(PlayerTransformer.class),
                        "hookSetDeltaMovement",
                        "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;)V",
                        false
                );
                node.instructions.set(m, redirect);
                break; // 只替换第一个（ordinal=0）
            }
        }
    }

    @Inject(method = "attack", desc = "(Lnet/minecraft/world/entity/Entity;)V")
    private void injectAttack_SetSprinting(MethodNode node) {
        if (node == null) {
            return;
        }
        String mappedSetSprint = Mapping.get(net.minecraft.world.entity.Entity.class, "setSprinting", "(Z)V");
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.name.equals(mappedSetSprint)
                    && m.desc.equals("(Z)V")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(PlayerTransformer.class),
                        "hookSetSprinting",
                        "(Lnet/minecraft/world/entity/player/Player;Z)V",
                        false
                );
                node.instructions.set(m, redirect);
                break; // 只替换第一个（ordinal=0）
            }
        }
    }

    @Inject(method = "isStayingOnGroundSurface", desc = "()Z")
    private void injectIsStayingOnGroundSurface(MethodNode node) {
        if (node == null) {
            return;
        }
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.IRETURN) {
                InsnList list = new InsnList();
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(PlayerTransformer.class),
                        "overrideIsStaying",
                        "(Z)Z",
                        false
                ));
                node.instructions.insertBefore(insn, list);
            }
        }
    }
}

