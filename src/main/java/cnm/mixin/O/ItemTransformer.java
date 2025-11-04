package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.impl.EventUseItemRayTrace;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class ItemTransformer extends ASMTransformer {
    public ItemTransformer() {
        super(Item.class);
    }

    public static float hookRayTraceYRot(Entity instance) {
        float origYaw = instance.getYRot();
        float origPitch = instance.getXRot();
        EventUseItemRayTrace event = new EventUseItemRayTrace(origYaw, origPitch);
        Naven.getInstance().getEventManager().call(event);
        if (MixinLoader.debugging) {
            System.out.println("[ASM][ItemTransformer] RayTrace YRot: orig=" + origYaw + " -> event=" + event.getYaw());
        }
        return event.getYaw();
    }

    public static float hookRayTraceXRot(Entity instance) {
        float origYaw = instance.getYRot();
        float origPitch = instance.getXRot();
        EventUseItemRayTrace event = new EventUseItemRayTrace(origYaw, origPitch);
        Naven.getInstance().getEventManager().call(event);
        if (MixinLoader.debugging) {
            System.out.println("[ASM][ItemTransformer] RayTrace XRot: orig=" + origPitch + " -> event=" + event.getPitch());
        }
        return event.getPitch();
    }

    public static void onEnterRayTrace(Player instance) {
        if (MixinLoader.debugging) {
            System.out.println("[ASM][ItemTransformer] Enter getPlayerPOVHitResult: playerYaw=" + instance.getYRot() + ", playerPitch=" + instance.getXRot());
        }
    }

    @Inject(method = "getPlayerPOVHitResult", desc = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;")
    private void injectGetPlayerPOVHitResult_HeadLog(MethodNode node) {
        if (node == null) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][ItemTransformer] Target method not found: getPlayerPOVHitResult (node is null) [HeadLog]");
            }
            return;
        }
        try {
            InsnList head = new InsnList();
            head.add(new VarInsnNode(Opcodes.ALOAD, 1));
            head.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(ItemTransformer.class),
                    "onEnterRayTrace",
                    "(Lnet/minecraft/world/entity/player/Player;)V",
                    false
            ));
            node.instructions.insert(head);
            if (MixinLoader.debugging) {
                System.out.println("[ASM][ItemTransformer] [HeadLog] Inserted onEnterRayTrace at head of getPlayerPOVHitResult");
            }
        } catch (Throwable t) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][ItemTransformer] [HeadLog] Failed to insert head enter log: " + t.getMessage());
                t.printStackTrace();
            }
        }
    }

    @Inject(method = "getPlayerPOVHitResult", desc = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;")
    private void injectGetPlayerPOVHitResult_RedirectYaw(MethodNode node) {
        if (node == null) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][ItemTransformer] Target method not found: getPlayerPOVHitResult (node is null) [Yaw]");
            }
            return;
        }
        String mappedGetYRot = Mapping.get(net.minecraft.world.entity.Entity.class, "getYRot", "()F");
        boolean replacedYaw = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.name.equals(mappedGetYRot)
                    && m.desc.equals("()F")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(ItemTransformer.class),
                        "hookRayTraceYRot",
                        "(Lnet/minecraft/world/entity/Entity;)F",
                        false
                );
                try {
                    node.instructions.set(m, redirect);
                    replacedYaw = true;
                } catch (Throwable ex) {
                    if (MixinLoader.debugging) {
                        System.out.println("[ASM][ItemTransformer] [Yaw] Failed to redirect getYRot: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        }
        if (MixinLoader.debugging) {
            System.out.println("[ASM][ItemTransformer] [Yaw] redirect status: " + replacedYaw);
        }
    }

    @Inject(method = "getPlayerPOVHitResult", desc = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;")
    private void injectGetPlayerPOVHitResult_RedirectPitch(MethodNode node) {
        if (node == null) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][ItemTransformer] Target method not found: getPlayerPOVHitResult (node is null) [Pitch]");
            }
            return;
        }
        String mappedGetXRot = Mapping.get(net.minecraft.world.entity.Entity.class, "getXRot", "()F");
        boolean replacedPitch = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.name.equals(mappedGetXRot)
                    && m.desc.equals("()F")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(ItemTransformer.class),
                        "hookRayTraceXRot",
                        "(Lnet/minecraft/world/entity/Entity;)F",
                        false
                );
                try {
                    node.instructions.set(m, redirect);
                    replacedPitch = true;
                } catch (Throwable ex) {
                    if (MixinLoader.debugging) {
                        System.out.println("[ASM][ItemTransformer] [Pitch] Failed to redirect getXRot: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        }
        if (MixinLoader.debugging) {
            System.out.println("[ASM][ItemTransformer] [Pitch] redirect status: " + replacedPitch);
        }
    }
}


