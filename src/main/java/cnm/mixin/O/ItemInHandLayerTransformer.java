package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventUpdateHeldItem;
import cnm.obsoverlay.utils.Wrapper;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ItemInHandLayerTransformer extends ASMTransformer implements Wrapper {
    public ItemInHandLayerTransformer() {
        super(ItemInHandLayer.class);
    }

    public static ItemStack hookMainHand(LivingEntity instance) {
        EventUpdateHeldItem event = new EventUpdateHeldItem(InteractionHand.MAIN_HAND, instance.getMainHandItem());
        if (instance == mc.player) {
            Naven.getInstance().getEventManager().call(event);
        }
        return event.getItem();
    }

    public static ItemStack hookOffHand(LivingEntity instance) {
        EventUpdateHeldItem event = new EventUpdateHeldItem(InteractionHand.OFF_HAND, instance.getOffhandItem());
        if (instance == mc.player) {
            Naven.getInstance().getEventManager().call(event);
        }
        return event.getItem();
    }

    @Inject(method = "render", desc = "(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFFF)V")
    private void injectRender(MethodNode node) {
        String mappedGetMain = Mapping.get(LivingEntity.class, "getMainHandItem", "()Lnet/minecraft/world/item/ItemStack;");
        String mappedGetOff = Mapping.get(LivingEntity.class, "getOffhandItem", "()Lnet/minecraft/world/item/ItemStack;");
        boolean replacedMain = false;
        boolean replacedOff = false;

        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(LivingEntity.class))
                    && m.name.equals(mappedGetMain)
                    && m.desc.equals("()Lnet/minecraft/world/item/ItemStack;")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(ItemInHandLayerTransformer.class),
                        "hookMainHand",
                        "(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
                        false
                );
                node.instructions.set(m, redirect);
                replacedMain = true;
            }
        }

        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(LivingEntity.class))
                    && m.name.equals(mappedGetOff)
                    && m.desc.equals("()Lnet/minecraft/world/item/ItemStack;")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(ItemInHandLayerTransformer.class),
                        "hookOffHand",
                        "(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
                        false
                );
                node.instructions.set(m, redirect);
                replacedOff = true;
            }
        }

        if (MixinLoader.debugging) {
            if (!replacedMain)
                System.out.println("[ASM][ItemInHandLayerTransformer] Failed to redirect getMainHandItem in render");
            if (!replacedOff)
                System.out.println("[ASM][ItemInHandLayerTransformer] Failed to redirect getOffhandItem in render");
        }
    }
}


