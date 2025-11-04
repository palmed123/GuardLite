package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.utils.renderer.GL;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;


public class VertexBufferTransformer extends ASMTransformer {
    public VertexBufferTransformer() {
        super(VertexBuffer.class);
    }

    // Fallback: 反射读取 RenderSystem.AutoStorageIndexBuffer 的 name 字段
    public static int resolveIndexBufferId(Object autoStorageIndexBuffer) {
        if (autoStorageIndexBuffer == null) return -1;
        Class<?> cls = autoStorageIndexBuffer.getClass();
        // 通过 mapping 获取字段名（如 name -> obf 名）
        String mappedField = Mapping.get(cls, "name", null);
        try {
            java.lang.reflect.Field f = cls.getDeclaredField(mappedField);
            f.setAccessible(true);
            return f.getInt(autoStorageIndexBuffer);
        } catch (Throwable ex1) {
            // 回退：尝试常见备选字段名
            for (String candidate : new String[]{"name", "id", "bufferName"}) {
                try {
                    java.lang.reflect.Field f2 = cls.getDeclaredField(candidate);
                    f2.setAccessible(true);
                    return f2.getInt(autoStorageIndexBuffer);
                } catch (Throwable ignored) {
                }
            }
            // 最后回退：扫描第一个 int 实例字段
            try {
                for (java.lang.reflect.Field f3 : cls.getDeclaredFields()) {
                    if (f3.getType() == int.class) {
                        f3.setAccessible(true);
                        return f3.getInt(autoStorageIndexBuffer);
                    }
                }
            } catch (Throwable ignored) {
            }
            if (MixinLoader.debugging) {
                System.out.println("[ASM][VertexBufferTransformer] Failed to resolve AutoStorageIndexBuffer id via mapping and fallbacks: " + ex1.getMessage());
                ex1.printStackTrace();
            }
            return -1;
        }
    }

    @Inject(method = "uploadIndexBuffer", desc = "(Lcom/mojang/blaze3d/vertex/BufferBuilder$DrawState;Ljava/nio/ByteBuffer;)Lcom/mojang/blaze3d/systems/RenderSystem$AutoStorageIndexBuffer;")
    private void injectUploadIndexBuffer(MethodNode node) {
        String fIndexBufferId = Mapping.get(VertexBuffer.class, "indexBufferId", null);

        try {
            int retLocal = node.maxLocals + 1;
            node.maxLocals = retLocal + 1;

            for (AbstractInsnNode insn : node.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.ARETURN) {
                    InsnList list = new InsnList();

                    // store return value
                    list.add(new VarInsnNode(Opcodes.ASTORE, retLocal));

                    LabelNode elseLabel = new LabelNode();
                    LabelNode endLabel = new LabelNode();

                    // if (ret == null) { GL.CURRENT_IBO = this.indexBufferId; } else { GL.CURRENT_IBO = ((ShapeIndexBufferAccessor)ret).getId(); }
                    list.add(new VarInsnNode(Opcodes.ALOAD, retLocal));
                    list.add(new JumpInsnNode(Opcodes.IFNONNULL, elseLabel));

                    // GL.CURRENT_IBO = this.indexBufferId;
                    list.add(new FieldInsnNode(
                            Opcodes.GETSTATIC,
                            Type.getInternalName(GL.class),
                            "CURRENT_IBO",
                            "I"
                    ));
                    // stack now has GL.CURRENT_IBO value, but we actually need to set it, not get. Use PUTSTATIC directly with value.
                    list.remove(list.getLast());
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new FieldInsnNode(
                            Opcodes.GETFIELD,
                            Type.getInternalName(VertexBuffer.class),
                            fIndexBufferId,
                            "I"
                    ));
                    list.add(new FieldInsnNode(
                            Opcodes.PUTSTATIC,
                            Type.getInternalName(GL.class),
                            "CURRENT_IBO",
                            "I"
                    ));
                    list.add(new JumpInsnNode(Opcodes.GOTO, endLabel));

                    // else branch
                    list.add(elseLabel);
                    list.add(new VarInsnNode(Opcodes.ALOAD, retLocal));
                    list.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(VertexBufferTransformer.class),
                            "resolveIndexBufferId",
                            "(Ljava/lang/Object;)I",
                            false
                    ));
                    list.add(new FieldInsnNode(
                            Opcodes.PUTSTATIC,
                            Type.getInternalName(GL.class),
                            "CURRENT_IBO",
                            "I"
                    ));

                    list.add(endLabel);

                    // reload return value
                    list.add(new VarInsnNode(Opcodes.ALOAD, retLocal));

                    node.instructions.insertBefore(insn, list);
                }
            }
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][VertexBufferTransformer] Failed to inject uploadIndexBuffer tail: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}


