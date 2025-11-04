package cnm.mixin.O;

import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventUpdateFoV;
import net.minecraft.client.player.AbstractClientPlayer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class AbstractClientPlayerTransformer extends ASMTransformer {
    public AbstractClientPlayerTransformer() {
        super(AbstractClientPlayer.class);
    }

    // 这个方法就是原 Mixin 中 hookFoV 的逻辑
    public static float onFoVUpdate(float originalFov) {
        EventUpdateFoV event = new EventUpdateFoV(originalFov);
        Naven.getInstance().getEventManager().call(event);
        return event.getFov();
    }

    @Inject(method = "getFieldOfViewModifier", desc = "()F")
    private void injectFoVHook(MethodNode methodNode) {
        boolean injected = false;
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.FRETURN) {
                InsnList inject = new InsnList();

                // 将原返回值作为参数传入静态回调
                inject.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(AbstractClientPlayerTransformer.class),
                        "onFoVUpdate",
                        "(F)F",
                        false
                ));

                // 替换原返回值
                try {
                    methodNode.instructions.insertBefore(insn, inject);
                    injected = true;
                } catch (Throwable ex) {
                    if (MixinLoader.debugging) {
                        System.out.println("[ASM][AbstractClientPlayerTransformer] Failed to insert FoV hook: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        }
        if (!injected && MixinLoader.debugging) {
            System.out.println("[ASM][AbstractClientPlayerTransformer] Did not find FRETURN in getFieldOfViewModifier for FoV hook");
        }
    }
}
