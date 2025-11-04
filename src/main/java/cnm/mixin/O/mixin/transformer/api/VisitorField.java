package cnm.mixin.O.mixin.transformer.api;

import cnm.mixin.O.mixin.MixinLoader;
import org.objectweb.asm.MethodVisitor;

public class VisitorField extends MethodVisitor {
    private final String[] target;

    public VisitorField(String[] target) {
        super(MixinLoader.ASM_API);
        this.target = target;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        target[0] = owner + "." + name + " " + descriptor;
    }

    public String[] getTarget() {
        return target;
    }
}
