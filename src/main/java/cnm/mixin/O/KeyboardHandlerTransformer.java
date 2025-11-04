package cnm.mixin.O;

import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventKey;
import net.minecraft.client.KeyboardHandler;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class KeyboardHandlerTransformer extends ASMTransformer {
    public KeyboardHandlerTransformer() {
        super(KeyboardHandler.class);
    }

    public static void onKeyPress(long windowPtr, int key, int scanCode, int action, int modifiers) {
        if (key != -1 && Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
            Naven.getInstance().getEventManager().call(new EventKey(key, action != 0));
        }
    }

    @Inject(method = "keyPress", desc = "(JIIII)V")
    private void injectKeyPress(MethodNode node) {
        // Insert at HEAD: call onKeyPress(windowPtr, key, scanCode, action, modifiers)
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.LLOAD, 1));
        list.add(new VarInsnNode(Opcodes.ILOAD, 3));
        list.add(new VarInsnNode(Opcodes.ILOAD, 4));
        list.add(new VarInsnNode(Opcodes.ILOAD, 5));
        list.add(new VarInsnNode(Opcodes.ILOAD, 6));
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(KeyboardHandlerTransformer.class),
                "onKeyPress",
                "(JIIII)V",
                false
        ));
        try {
            node.instructions.insert(list);
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][KeyboardHandlerTransformer] Failed to insert head call in keyPress: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}


