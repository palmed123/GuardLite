package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventUpdateHeldItem;
import cnm.obsoverlay.utils.Wrapper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ItemInHandRendererTransformer extends ASMTransformer implements Wrapper {
    public ItemInHandRendererTransformer() {
        super(ItemInHandRenderer.class);
    }

    public static ItemStack hookMainHand(LocalPlayer player) {
        EventUpdateHeldItem event = new EventUpdateHeldItem(InteractionHand.MAIN_HAND, player.getMainHandItem());
        if (player == mc.player) {
            Naven.getInstance().getEventManager().call(event);
        }
        return event.getItem();
    }

    public static ItemStack hookOffHand(LocalPlayer player) {
        EventUpdateHeldItem event = new EventUpdateHeldItem(InteractionHand.OFF_HAND, player.getOffhandItem());
        if (player == mc.player) {
            Naven.getInstance().getEventManager().call(event);
        }
        return event.getItem();
    }

    @Inject(method = "tick", desc = "()V")
    private void injectTick(MethodNode node) {
        // 方法声明在 Entity/LivingEntity，避免映射不到
        String mappedGetMain = Mapping.get(net.minecraft.world.entity.LivingEntity.class, "getMainHandItem", "()Lnet/minecraft/world/item/ItemStack;");
        String mappedGetOff = Mapping.get(net.minecraft.world.entity.LivingEntity.class, "getOffhandItem", "()Lnet/minecraft/world/item/ItemStack;");
        boolean replacedMain = false;
        boolean replacedOff = false;

        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.name.equals(mappedGetMain)
                    && m.desc.equals("()Lnet/minecraft/world/item/ItemStack;")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(ItemInHandRendererTransformer.class),
                        "hookMainHand",
                        "(Lnet/minecraft/client/player/LocalPlayer;)Lnet/minecraft/world/item/ItemStack;",
                        false
                );
                node.instructions.set(m, redirect);
                replacedMain = true;
            }
        }

        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.name.equals(mappedGetOff)
                    && m.desc.equals("()Lnet/minecraft/world/item/ItemStack;")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(ItemInHandRendererTransformer.class),
                        "hookOffHand",
                        "(Lnet/minecraft/client/player/LocalPlayer;)Lnet/minecraft/world/item/ItemStack;",
                        false
                );
                node.instructions.set(m, redirect);
                replacedOff = true;
            }
        }

        if (MixinLoader.debugging) {
            if (!replacedMain)
                System.out.println("[ASM][ItemInHandRendererTransformer] Failed to redirect LocalPlayer.getMainHandItem in tick");
            if (!replacedOff)
                System.out.println("[ASM][ItemInHandRendererTransformer] Failed to redirect LocalPlayer.getOffhandItem in tick");
        }
    }
}


