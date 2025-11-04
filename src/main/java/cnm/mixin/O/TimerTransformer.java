package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import net.minecraft.client.Timer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class TimerTransformer extends ASMTransformer {
    public TimerTransformer() {
        super(Timer.class);
    }

    @Inject(method = "advanceTime", desc = "(J)I")
    private void injectAdvanceTime(MethodNode node) {
        // Field mappings
        String fPartialTick = Mapping.get(Timer.class, "partialTick", null);
        String fLastMs = Mapping.get(Timer.class, "lastMs", null);
        String fMsPerTick = Mapping.get(Timer.class, "msPerTick", null);
        String fTickDelta = Mapping.get(Timer.class, "tickDelta", null);

        InsnList list = new InsnList();
        LabelNode cont = new LabelNode();

        // if (Naven.TICK_TIMER == 1.0F) goto cont;
        list.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                Type.getInternalName(Naven.class),
                "TICK_TIMER",
                "F"
        ));
        list.add(new InsnNode(Opcodes.FCONST_1));
        list.add(new InsnNode(Opcodes.FCMPG));
        list.add(new JumpInsnNode(Opcodes.IFEQ, cont));

        // this.tickDelta = ((timeMillis - this.lastMs) / this.msPerTick) * Naven.TICK_TIMER;
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new VarInsnNode(Opcodes.LLOAD, 1));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, Type.getInternalName(Timer.class), fLastMs, "J"));
        list.add(new InsnNode(Opcodes.LSUB));
        list.add(new InsnNode(Opcodes.L2F));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, Type.getInternalName(Timer.class), fMsPerTick, "F"));
        list.add(new InsnNode(Opcodes.FDIV));
        list.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Naven.class), "TICK_TIMER", "F"));
        list.add(new InsnNode(Opcodes.FMUL));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD, Type.getInternalName(Timer.class), fTickDelta, "F"));

        // this.lastMs = timeMillis;
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new VarInsnNode(Opcodes.LLOAD, 1));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD, Type.getInternalName(Timer.class), fLastMs, "J"));

        // this.partialTick = this.partialTick + this.tickDelta;
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, Type.getInternalName(Timer.class), fPartialTick, "F"));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, Type.getInternalName(Timer.class), fTickDelta, "F"));
        list.add(new InsnNode(Opcodes.FADD));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD, Type.getInternalName(Timer.class), fPartialTick, "F"));

        // int i = (int)this.partialTick;
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, Type.getInternalName(Timer.class), fPartialTick, "F"));
        list.add(new InsnNode(Opcodes.F2I));
        list.add(new VarInsnNode(Opcodes.ISTORE, 3));

        // this.partialTick -= (float)i;
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, Type.getInternalName(Timer.class), fPartialTick, "F"));
        list.add(new VarInsnNode(Opcodes.ILOAD, 3));
        list.add(new InsnNode(Opcodes.I2F));
        list.add(new InsnNode(Opcodes.FSUB));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD, Type.getInternalName(Timer.class), fPartialTick, "F"));

        // return i;
        list.add(new VarInsnNode(Opcodes.ILOAD, 3));
        list.add(new InsnNode(Opcodes.IRETURN));

        // cont:
        list.add(cont);

        try {
            node.instructions.insert(list);
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][TimerTransformer] Failed to inject advanceTime head: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}


