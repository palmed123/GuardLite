package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventAttack;
import cnm.obsoverlay.events.impl.EventDestroyBlock;
import cnm.obsoverlay.events.impl.EventPositionItem;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class MultiPlayerGameModeTransformer extends ASMTransformer {
    public MultiPlayerGameModeTransformer() {
        super(MultiPlayerGameMode.class);
    }

    public static void onSendPacket(ClientPacketListener instance, Packet<?> pPacket) {
        EventPositionItem event = new EventPositionItem(pPacket);
        Naven.getInstance().getEventManager().call(event);
        if (!event.isCancelled()) {
            instance.send(event.getPacket());
        }
    }

    public static void onStartDestroyBlock(BlockPos pos, Direction face) {
        Naven.getInstance().getEventManager().call(new EventDestroyBlock(pos, face));
    }

    public static boolean hookAttack(Entity target) {
        EventAttack eventAttack = new EventAttack(target, EventAttack.Type.Pre);
        Naven.getInstance().getEventManager().call(eventAttack);
        return eventAttack.isCancelled();
    }

    public static void postAttack(Entity target) {
        EventAttack eventAttack = new EventAttack(target, EventAttack.Type.Post);
        Naven.getInstance().getEventManager().call(eventAttack);
    }

    @Inject(method = "useItem", desc = "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;")
    private void injectUseItem(MethodNode node) {
        String mappedSend = Mapping.get(ClientPacketListener.class, "send", "(Lnet/minecraft/network/protocol/Packet;)V");
        boolean replaced = false;
        int ordinal = 0;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(ClientPacketListener.class))
                    && m.name.equals(mappedSend)
                    && m.desc.equals("(Lnet/minecraft/network/protocol/Packet;)V")) {
                if (ordinal == 0) {
                    MethodInsnNode redirect = new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(MultiPlayerGameModeTransformer.class),
                            "onSendPacket",
                            "(Lnet/minecraft/client/multiplayer/ClientPacketListener;Lnet/minecraft/network/protocol/Packet;)V",
                            false
                    );
                    node.instructions.set(m, redirect);
                    replaced = true;
                    break;
                }
                ordinal++;
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][MultiPlayerGameModeTransformer] Failed to redirect ClientPacketListener.send in useItem (ordinal=0)");
        }
    }

    @Inject(method = "startDestroyBlock", desc = "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z")
    private void injectStartDestroyBlock(MethodNode node) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 1));
        list.add(new VarInsnNode(Opcodes.ALOAD, 2));
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(MultiPlayerGameModeTransformer.class),
                "onStartDestroyBlock",
                "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V",
                false
        ));
        try {
            node.instructions.insert(list);
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][MultiPlayerGameModeTransformer] Failed to inject head for startDestroyBlock: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Inject(method = "attack", desc = "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V")
    private void injectAttack(MethodNode node) {
        try {
            // 在方法开头插入 hookAttack 调用，如果返回 true 则直接 return
            InsnList headList = new InsnList();
            headList.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 加载 Entity 参数
            headList.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(MultiPlayerGameModeTransformer.class),
                    "hookAttack",
                    "(Lnet/minecraft/world/entity/Entity;)Z",
                    false
            ));
            LabelNode continueLabel = new LabelNode();
            headList.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel)); // 如果返回 false，跳转到 continueLabel
            headList.add(new InsnNode(Opcodes.RETURN)); // 如果返回 true，直接返回
            headList.add(continueLabel);
            
            node.instructions.insert(headList);
            
            // 在所有 RETURN 指令之前插入 postAttack 调用
            AbstractInsnNode[] instructions = node.instructions.toArray();
            for (AbstractInsnNode insn : instructions) {
                if (insn.getOpcode() == Opcodes.RETURN) {
                    InsnList beforeReturn = new InsnList();
                    beforeReturn.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 加载 Entity 参数
                    beforeReturn.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(MultiPlayerGameModeTransformer.class),
                            "postAttack",
                            "(Lnet/minecraft/world/entity/Entity;)V",
                            false
                    ));
                    node.instructions.insertBefore(insn, beforeReturn);
                }
            }
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][MultiPlayerGameModeTransformer] Failed to inject attack: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}


