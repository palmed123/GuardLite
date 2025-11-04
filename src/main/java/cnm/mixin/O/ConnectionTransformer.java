package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventGlobalPacket;
import cnm.obsoverlay.utils.NetworkUtils;
import cnm.obsoverlay.utils.ReflectUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.RunningOnDifferentThreadException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Method;

public class ConnectionTransformer extends ASMTransformer {
    public ConnectionTransformer() {
        super(Connection.class);
    }

    // 替代 genericsFtw 的回调：事件触发后再调用原始静态方法
    public static void onGenericsFtw(Packet<?> pPacket, PacketListener pListener) {
        try {
            EventGlobalPacket event = new EventGlobalPacket(EventType.RECEIVE, pPacket);
            Naven.getInstance().getEventManager().call(event);
            if (!event.isCancelled()) {
                Method m = ReflectUtil.getMethod(Connection.class, "genericsFtw", "(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V", Packet.class, PacketListener.class);
                m.invoke(null, event.getPacket(), pListener);
            }
        } catch (Exception ex) {
            if (ex instanceof RunningOnDifferentThreadException || ex.getCause() instanceof RunningOnDifferentThreadException) {
                return; // 按 MC 语义：交由主线程稍后处理，当前线程直接返回
            }
            if (MixinLoader.debugging) {
                System.out.println("[ASM][ConnectionTransformer]: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // 替代 send 内部对 sendPacket 的调用
    public static void onSend(Connection instance, Packet<?> pInPacket, PacketSendListener pFutureListeners) {
        try {
            if (NetworkUtils.passthroughsPackets.contains(pInPacket)) {
                NetworkUtils.passthroughsPackets.remove(pInPacket);
                Method m = ReflectUtil.getMethod(Connection.class, "sendPacket", "(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", Packet.class, PacketSendListener.class);
                m.invoke(instance, pInPacket, pFutureListeners);
            } else {
                EventGlobalPacket event = new EventGlobalPacket(EventType.SEND, pInPacket);
                Naven.getInstance().getEventManager().call(event);
                if (!event.isCancelled()) {
                    Method m = ReflectUtil.getMethod(Connection.class, "sendPacket", "(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", Packet.class, PacketSendListener.class);
                    m.invoke(instance, event.getPacket(), pFutureListeners);
                }
            }
        } catch (Exception ex) {
            if (ex instanceof RunningOnDifferentThreadException || ex.getCause() instanceof RunningOnDifferentThreadException) {
                return; // 忽略并不打印
            }
            if (MixinLoader.debugging) {
                System.out.println("[ASM][ConnectionTransformer] Reflect invoke sendPacket failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // 已移除 connect/connectToServer 的 hook

    @Inject(method = "channelRead0", desc = "(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V")
    private void injectChannelRead0(MethodNode node) {
        String mappedGenerics = Mapping.get(Connection.class, "genericsFtw", "(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V");
        boolean replacedGenerics = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKESTATIC
                    && m.owner.equals(Type.getInternalName(Connection.class))
                    && m.name.equals(mappedGenerics)
                    && m.desc.equals("(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V")) {
                MethodInsnNode replacement = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(ConnectionTransformer.class),
                        "onGenericsFtw",
                        "(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V",
                        false
                );
                node.instructions.set(m, replacement);
                replacedGenerics = true;
                break;
            }
        }
        if (!replacedGenerics && MixinLoader.debugging) {
            System.out.println("[ASM][ConnectionTransformer] Failed to replace Connection." + mappedGenerics + " in channelRead0");
        }
    }

    @Inject(method = "send", desc = "(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V")
    private void injectSend(MethodNode node) {
        String mappedSendPacket = Mapping.get(Connection.class, "sendPacket", "(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V");
        boolean replacedSend = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(Connection.class))
                    && m.name.equals(mappedSendPacket)
                    && m.desc.equals("(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V")) {
                MethodInsnNode replacement = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(ConnectionTransformer.class),
                        "onSend",
                        "(Lnet/minecraft/network/Connection;Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
                        false
                );
                // 原本是调用虚方法 instance.sendPacket(packet, listener)
                // 栈顶顺序保持: this, packet, listener -> 静态方法签名 (Connection, Packet, PacketSendListener)
                node.instructions.set(m, replacement);
                replacedSend = true;
                break;
            }
        }
        if (!replacedSend && MixinLoader.debugging) {
            System.out.println("[ASM][ConnectionTransformer] Failed to redirect Connection." + mappedSendPacket + " in send");
        }
    }

    // connect/connectToServer 注入已按需求删除
}


