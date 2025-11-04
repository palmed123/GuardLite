package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventServerSetPosition;
import cnm.obsoverlay.utils.Wrapper;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ClientPacketListenerTransformer extends ASMTransformer implements Wrapper {
    public ClientPacketListenerTransformer() {
        super(ClientPacketListener.class);
    }

    public static void onSendPacket(Connection instance, Packet<?> pPacket) {
        EventServerSetPosition event = new EventServerSetPosition(pPacket);
        Naven.getInstance().getEventManager().call(event);
        instance.send(event.getPacket());
    }

    @Inject(method = "handleMovePlayer", desc = "(Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;)V")
    public void handleMovePlayer(MethodNode methodNode) {
        int ordinal = 0;
        String mappedSend = Mapping.get(Connection.class, "send", "(Lnet/minecraft/network/protocol/Packet;)V");
        boolean replaced = false;
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m) {
                if (m.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && m.owner.equals(Type.getInternalName(Connection.class))
                        && m.name.equals(mappedSend)
                        && m.desc.equals("(Lnet/minecraft/network/protocol/Packet;)V")) {
                    if (ordinal == 1) { // ordinal = 1 -> 第二次出现
                        MethodInsnNode redirect = new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                Type.getInternalName(ClientPacketListenerTransformer.class),
                                "onSendPacket",
                                "(Lnet/minecraft/network/Connection;Lnet/minecraft/network/protocol/Packet;)V",
                                false
                        );
                        methodNode.instructions.set(m, redirect);
                        replaced = true;
                        break;
                    }
                    ordinal++;
                }
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][ClientPacketListenerTransformer] Failed to redirect Connection." + mappedSend + " ordinal=1 in handleMovePlayer");
        }
    }
}
