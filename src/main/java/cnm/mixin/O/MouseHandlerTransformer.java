package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventMouseClick;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.MouseHandler;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class MouseHandlerTransformer extends ASMTransformer {
    public MouseHandlerTransformer() {
        super(MouseHandler.class);
    }

    public static void onPress(long window, int button, int action, int mods) {
        EventMouseClick event = new EventMouseClick(button, action == 0);
        Naven.getInstance().getEventManager().call(event);
    }

    @Inject(method = "onPress", desc = "(JIII)V")
    private void injectOnPress(MethodNode node) {
        String owner = Type.getInternalName(KeyMapping.class);
        boolean inserted = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKESTATIC
                    && m.owner.equals(owner)
                    && (m.name.equals("set") || m.name.equals(Mapping.get(KeyMapping.class, "set", "(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V")))
                    && m.desc.equals("(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V")) {
                InsnList list = new InsnList();
                list.add(new VarInsnNode(Opcodes.LLOAD, 1));
                list.add(new VarInsnNode(Opcodes.ILOAD, 3));
                list.add(new VarInsnNode(Opcodes.ILOAD, 4));
                list.add(new VarInsnNode(Opcodes.ILOAD, 5));
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(MouseHandlerTransformer.class),
                        "onPress",
                        "(JIII)V",
                        false
                ));
                node.instructions.insertBefore(m, list);
                inserted = true;
                break;
            }
        }
        if (!inserted && MixinLoader.debugging) {
            System.out.println("[ASM][MouseHandlerTransformer] Failed to insert onPress hook before KeyMapping.set in onPress");
        }
    }
}


