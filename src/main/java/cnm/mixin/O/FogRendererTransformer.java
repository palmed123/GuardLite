package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.modules.impl.render.AntiBlindness;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class FogRendererTransformer extends ASMTransformer {
    public FogRendererTransformer() {
        super(FogRenderer.class);
    }

    public static boolean redirectedHasEffect(LivingEntity instance, MobEffect effect) {
        if (effect == MobEffects.BLINDNESS && Naven.getInstance().getModuleManager().getModule(AntiBlindness.class).isEnabled()) {
            return false;
        }
        return instance.hasEffect(effect);
    }

    @Inject(method = "setupColor", desc = "(Lnet/minecraft/client/Camera;FLnet/minecraft/client/Minecraft;F)V")
    private void injectSetupColor(MethodNode node) {
        String mappedHasEffect = Mapping.get(LivingEntity.class, "hasEffect", "(Lnet/minecraft/world/effect/MobEffect;)Z");
        boolean replaced = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(LivingEntity.class))
                    && m.name.equals(mappedHasEffect)
                    && m.desc.equals("(Lnet/minecraft/world/effect/MobEffect;)Z")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(FogRendererTransformer.class),
                        "redirectedHasEffect",
                        "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffect;)Z",
                        false
                );
                node.instructions.set(m, redirect);
                replaced = true;
                break;
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][FogRendererTransformer] Failed to redirect hasEffect in setupColor");
        }
    }

    @Inject(method = "setupFog", desc = "(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V")
    private void injectSetupFog(MethodNode node) {
        String mappedHasEffect = Mapping.get(LivingEntity.class, "hasEffect", "(Lnet/minecraft/world/effect/MobEffect;)Z");
        boolean replaced = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(LivingEntity.class))
                    && m.name.equals(mappedHasEffect)
                    && m.desc.equals("(Lnet/minecraft/world/effect/MobEffect;)Z")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(FogRendererTransformer.class),
                        "redirectedHasEffect",
                        "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffect;)Z",
                        false
                );
                node.instructions.set(m, redirect);
                replaced = true;
                break;
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][FogRendererTransformer] Failed to redirect hasEffect in setupFog");
        }
    }
}


