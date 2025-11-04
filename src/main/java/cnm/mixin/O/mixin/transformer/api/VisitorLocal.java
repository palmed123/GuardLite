package cnm.mixin.O.mixin.transformer.api;

import cnm.mixin.O.mixin.MixinLoader;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class VisitorLocal extends MethodVisitor {
    private final String name;
    private final int[] varIndex;

    public VisitorLocal(String name, int[] varIndex) {
        super(MixinLoader.ASM_API);
        this.name = name;
        this.varIndex = varIndex;
    }

    @Override
    public void visitLocalVariable(String varName, String descriptor, String signature, Label start, Label end, int index) {
        if (name.equals(varName))
            varIndex[0] = index;
        super.visitLocalVariable(varName, descriptor, signature, start, end, index);
    }

    public int[] getTarget() {
        return varIndex;
    }
}
