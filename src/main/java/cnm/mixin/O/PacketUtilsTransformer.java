package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.utils.MixinProtectionUtils;
import net.minecraft.network.protocol.PacketUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class PacketUtilsTransformer extends ASMTransformer {
    public PacketUtilsTransformer() {
        super(PacketUtils.class);
    }

    @Inject(method = "ensureRunningOnSameThread", desc = "(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V")
    private static void injectEnsureRunningOnSameThread(MethodNode node) {
        try {
            // Clear original body and replace with:
            // MixinProtectionUtils.onEnsureRunningOnSameThread(LOGGER, packet, listener, executor);
            // return;
            InsnList list = new InsnList();
            String mappedLogger = Mapping.get(PacketUtils.class, "LOGGER", null);
            list.add(new FieldInsnNode(
                    Opcodes.GETSTATIC,
                    Type.getInternalName(PacketUtils.class),
                    mappedLogger,
                    Type.getDescriptor(org.slf4j.Logger.class)
            ));
            list.add(new VarInsnNode(Opcodes.ALOAD, 0)); // packet
            list.add(new VarInsnNode(Opcodes.ALOAD, 1)); // listener
            list.add(new VarInsnNode(Opcodes.ALOAD, 2)); // executor
            list.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(MixinProtectionUtils.class),
                    "onEnsureRunningOnSameThread",
                    "(Lorg/slf4j/Logger;Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
                    false
            ));
            list.add(new InsnNode(Opcodes.RETURN));

            node.instructions.clear();
            node.tryCatchBlocks.clear();
            node.instructions.add(list);
            node.exceptions = null; // method will no longer throw
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][PacketUtilsTransformer] Failed to rewrite ensureRunningOnSameThread: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}


