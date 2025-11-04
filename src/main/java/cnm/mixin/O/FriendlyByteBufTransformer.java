package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.modules.impl.render.NameProtect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class FriendlyByteBufTransformer extends ASMTransformer {
    public FriendlyByteBufTransformer() {
        super(FriendlyByteBuf.class);
    }

    public static MutableComponent fromJsonPatched(String json) {
        NameProtect nameProtect = (NameProtect) Naven.getInstance().getModuleManager().getModule(NameProtect.class);
        return Component.Serializer.fromJson(nameProtect.getDisplayName(json));
    }

    @Inject(method = "readComponent", desc = "()Lnet/minecraft/network/chat/Component;")
    private void injectReadComponent(MethodNode node) {
        String owner = Type.getInternalName(Component.Serializer.class);
        String mappedFromJson = Mapping.get(Component.Serializer.class, "fromJson", "(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;");
        boolean replaced = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKESTATIC
                    && m.owner.equals(owner)
                    && m.name.equals(mappedFromJson)
                    && m.desc.equals("(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(FriendlyByteBufTransformer.class),
                        "fromJsonPatched",
                        "(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
                        false
                );
                node.instructions.set(m, redirect);
                replaced = true;
                break;
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][FriendlyByteBufTransformer] Failed to redirect Component.Serializer.fromJson in readComponent");
        }
    }
}


