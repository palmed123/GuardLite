package cnm.mixin.O.mixin.transformer.api;

import cnm.mixin.O.mixin.MixinLoader;
import org.objectweb.asm.MethodVisitor;

public class VisitorMethod extends MethodVisitor {
    private final String[] target;

    public VisitorMethod(String[] target) {
        super(MixinLoader.ASM_API);
        this.target = target;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        target[0] = owner + "." + name + descriptor;
    }

    public String[] getTarget() {
        return target;
    }
}
