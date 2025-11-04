package cnm.mixin.O;

import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.modules.impl.render.ViewClip;
import net.minecraft.client.Camera;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class CameraTransformer extends ASMTransformer {
    public CameraTransformer() {
        super(Camera.class);
    }

    public static boolean shouldOverrideZoom() {
        if (Naven.getInstance() == null || Naven.getInstance().getModuleManager() == null) return false;
        ViewClip module = (ViewClip) Naven.getInstance().getModuleManager().getModule(ViewClip.class);
        return module != null && module.isEnabled();
    }

    public static double computeZoom(double startingDistance) {
        ViewClip module = (ViewClip) Naven.getInstance().getModuleManager().getModule(ViewClip.class);
        return startingDistance * (double) module.scale.getCurrentValue() * (double) module.personViewAnimation.value / 100.0;
    }

    @Inject(method = "getMaxZoom", desc = "(D)D")
    private void injectGetMaxZoom(MethodNode methodNode) {
        InsnList instructions = new InsnList();
        LabelNode continueLabel = new LabelNode();

        instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(CameraTransformer.class),
                "shouldOverrideZoom",
                "()Z",
                false
        ));

        instructions.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        instructions.add(new VarInsnNode(Opcodes.DLOAD, 1));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(CameraTransformer.class),
                "computeZoom",
                "(D)D",
                false
        ));
        instructions.add(new InsnNode(Opcodes.DRETURN));
        instructions.add(continueLabel);

        try {
            methodNode.instructions.insert(instructions);
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][CameraTransformer] Failed to inject head logic into getMaxZoom: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}


