package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventRotationAnimation;
import cnm.obsoverlay.utils.Wrapper;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class LivingEntityRendererTransformer extends ASMTransformer implements Wrapper {
    public LivingEntityRendererTransformer() {
        super(LivingEntityRenderer.class);
    }

    public static float rotAnimationYaw(float pDelta, float pStart, float pEnd) {
        EventRotationAnimation event = new EventRotationAnimation(pEnd, pStart, 0.0F, 0.0F);
        if (EventRotationAnimation.currentEntity == mc.player) {
            Naven.getInstance().getEventManager().call(event);
        }
        return Mth.rotLerp(pDelta, event.getLastYaw(), event.getYaw());
    }

    public static float rotAnimationPitch(float pDelta, float pStart, float pEnd) {
        EventRotationAnimation event = new EventRotationAnimation(0.0F, 0.0F, pEnd, pStart);
        if (EventRotationAnimation.currentEntity == mc.player) {
            Naven.getInstance().getEventManager().call(event);
        }
        return Mth.lerp(pDelta, event.getLastPitch(), event.getPitch());
    }

    @Inject(method = "render", desc = "(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void injectRenderSetCurrentEntity(MethodNode node) {
        if (node == null) {
            return;
        }
        InsnList head = new InsnList();
        head.add(new VarInsnNode(Opcodes.ALOAD, 1));
        head.add(new FieldInsnNode(
                Opcodes.PUTSTATIC,
                Type.getInternalName(EventRotationAnimation.class),
                "currentEntity",
                Type.getDescriptor(net.minecraft.world.entity.Entity.class)
        ));
        node.instructions.insert(head);
    }

    @Inject(method = "render", desc = "(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void injectRenderRedirectRotLerp(MethodNode node) {
        if (node == null) {
            return;
        }
        String mthOwner = Type.getInternalName(Mth.class);
        String mappedRotLerp = Mapping.get(Mth.class, "rotLerp", "(FFF)F");
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKESTATIC
                    && m.owner.equals(mthOwner)
                    && m.name.equals(mappedRotLerp)
                    && m.desc.equals("(FFF)F")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(LivingEntityRendererTransformer.class),
                        "rotAnimationYaw",
                        "(FFF)F",
                        false
                );
                node.instructions.set(m, redirect);
            }
        }
    }

    @Inject(method = "render", desc = "(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void injectRenderRedirectLerp(MethodNode node) {
        if (node == null) {
            return;
        }
        String mthOwner = Type.getInternalName(Mth.class);
        String mappedLerp = Mapping.get(Mth.class, "lerp", "(FFF)F");
        int ordinal = 0;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKESTATIC
                    && m.owner.equals(mthOwner)
                    && m.name.equals(mappedLerp)
                    && m.desc.equals("(FFF)F")) {
                if (ordinal == 0) {
                    MethodInsnNode redirect = new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(LivingEntityRendererTransformer.class),
                            "rotAnimationPitch",
                            "(FFF)F",
                            false
                    );
                    node.instructions.set(m, redirect);
                    break;
                }
                ordinal++;
            }
        }
    }
}


