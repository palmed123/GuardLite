package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.utils.Wrapper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class ClientLevelTransformer extends ASMTransformer implements Wrapper {
    public ClientLevelTransformer() {
        super(ClientLevel.class);
    }


    public static boolean skipTicks(Entity entity) {
        if (Naven.skipTicks > 0 && entity == mc.player) {
            Naven.skipTicks--;
            return true;
        }
        return false;
    }

    @Inject(method = "tickNonPassenger", desc = "(Lnet/minecraft/world/entity/Entity;)V")
    public void tickNonPassenger(MethodNode methodNode) {
        InsnList instructions = new InsnList();
        LabelNode continueLabel = new LabelNode();

        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
            // 定位到 GETFIELD tickCount 的指令
            if (node.getOpcode() == Opcodes.GETFIELD) {
                FieldInsnNode fieldNode = (FieldInsnNode) node;
                if (fieldNode.name.equals(Mapping.get(Entity.class, "tickCount", null)) && fieldNode.desc.equals("I")) {
                    // 在 tickCount 字段访问后插入逻辑
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 加载 Entity 对象
                    instructions.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(ClientLevelTransformer.class),
                            "skipTicks",
                            "(Lnet/minecraft/world/entity/Entity;)Z",
                            false
                    ));
                    instructions.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
                    instructions.add(new InsnNode(Opcodes.RETURN));
                    instructions.add(continueLabel);
                    methodNode.instructions.insert(node, instructions);
                    break;
                }
            }
        }
    }
}
