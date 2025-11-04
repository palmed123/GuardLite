package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventClick;
import cnm.obsoverlay.events.impl.EventRunTicks;
import cnm.obsoverlay.events.impl.EventShutdown;
import cnm.obsoverlay.modules.impl.render.Glow;
import cnm.obsoverlay.utils.AnimationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class MinecraftTransformer extends ASMTransformer {
    private static final int skipTicks = 0;
    private static long lastFrameMs = 0L;

    public MinecraftTransformer() {
        super(Minecraft.class);
    }

    // ===== Helpers =====
    public static void onInit() {
        try {
            System.setProperty("java.awt.headless", "false");
            ModList.get().getMods().removeIf(modInfox -> modInfox.getModId().contains("guardlite"));
            List<IModFileInfo> fileInfoToRemove = new ArrayList<>();
            for (IModFileInfo fileInfo : ModList.get().getModFiles()) {
                for (IModInfo modInfo : fileInfo.getMods()) {
                    if (modInfo.getModId().contains("guardlite")) {
                        fileInfoToRemove.add(fileInfo);
                    }
                }
            }
            ModList.get().getModFiles().removeAll(fileInfoToRemove);
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][MinecraftTransformer] onInit failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public static void onShutdown() {
        if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
            Naven.getInstance().getEventManager().call(new EventShutdown());
        }
    }

    public static void tickPre() {
        if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
            Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.PRE));
        }
    }

    public static void tickPost() {
        if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
            Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.POST));
        }
    }

    public static boolean shouldEntityAppearGlowing(Entity entity) {
        return Glow.shouldGlow(entity);
    }

    public static void runTickHead() {
        long currentTime = System.nanoTime() / 1_000_000L;
        int deltaTime = (int) (currentTime - lastFrameMs);
        lastFrameMs = currentTime;
        AnimationUtils.delta = deltaTime;
    }

    public static float fixSkipTicks(float g) {
        if (skipTicks > 0) {
            return 0.0F;
        }
        return g;
    }

    public static void renderWrapper(GameRenderer renderer, float g, long j, boolean z) {
        renderer.render(fixSkipTicks(g), j, z);
    }

    public static boolean clickEvent() {
        if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
            EventClick event = new EventClick();
            Naven.getInstance().getEventManager().call(event);
            return event.isCancelled();
        }
        return false;
    }

    // ===== Injections =====
    @Inject(method = "<init>", desc = "(Lnet/minecraft/client/main/GameConfig;)V")
    private void injectInit(MethodNode node) {
        boolean inserted = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                InsnList list = new InsnList();
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(MinecraftTransformer.class),
                        "onInit",
                        "()V",
                        false
                ));
                try {
                    node.instructions.insertBefore(insn, list);
                    inserted = true;
                } catch (Throwable ex) {
                    if (MixinLoader.debugging) {
                        System.out.println("[ASM][MinecraftTransformer] Failed to insert onInit in <init>: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        }
        if (!inserted && MixinLoader.debugging) {
            System.out.println("[ASM][MinecraftTransformer] Did not find RETURN in <init> for onInit insertion");
        }
    }

    @Inject(method = "close", desc = "()V")
    private void injectClose(MethodNode node) {
        InsnList list = new InsnList();
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(MinecraftTransformer.class),
                "onShutdown",
                "()V",
                false
        ));
        try {
            node.instructions.insert(list);
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][MinecraftTransformer] Failed to insert onShutdown in close: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Inject(method = "tick", desc = "()V")
    private void injectTickHead(MethodNode node) {
        InsnList list = new InsnList();
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(MinecraftTransformer.class),
                "tickPre",
                "()V",
                false
        ));
        try {
            node.instructions.insert(list);
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][MinecraftTransformer] Failed to insert tickPre in tick HEAD: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Inject(method = "tick", desc = "()V")
    private void injectTickTail(MethodNode node) {
        boolean inserted = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                InsnList list = new InsnList();
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(MinecraftTransformer.class),
                        "tickPost",
                        "()V",
                        false
                ));
                try {
                    node.instructions.insertBefore(insn, list);
                    inserted = true;
                } catch (Throwable ex) {
                    if (MixinLoader.debugging) {
                        System.out.println("[ASM][MinecraftTransformer] Failed to insert tickPost in tick TAIL: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        }
        if (!inserted && MixinLoader.debugging) {
            System.out.println("[ASM][MinecraftTransformer] Did not find RETURN in tick for tickPost insertion");
        }
    }

    @Inject(method = "shouldEntityAppearGlowing", desc = "(Lnet/minecraft/world/entity/Entity;)Z")
    private void injectShouldEntityAppearGlowing(MethodNode node) {
        InsnList list = new InsnList();
        LabelNode cont = new LabelNode();
        list.add(new VarInsnNode(Opcodes.ALOAD, 1));
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(MinecraftTransformer.class),
                "shouldEntityAppearGlowing",
                "(Lnet/minecraft/world/entity/Entity;)Z",
                false
        ));
        list.add(new JumpInsnNode(Opcodes.IFEQ, cont));
        list.add(new InsnNode(Opcodes.ICONST_1));
        list.add(new InsnNode(Opcodes.IRETURN));
        list.add(cont);
        try {
            node.instructions.insert(list);
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][MinecraftTransformer] Failed to insert glowing override: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Inject(method = "runTick", desc = "(Z)V")
    private void injectRunTickHead(MethodNode node) {
        if (node == null) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][MinecraftTransformer] Target method not found: runTick HEAD (node is null)");
            }
            return;
        }
        InsnList list = new InsnList();
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(MinecraftTransformer.class),
                "runTickHead",
                "()V",
                false
        ));
        try {
            node.instructions.insert(list);
        } catch (Throwable ex) {
            if (MixinLoader.debugging) {
                System.out.println("[ASM][MinecraftTransformer] Failed to insert runTickHead in runTick HEAD: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Inject(method = "runTick", desc = "(Z)V")
    private void injectRunTickRenderArg(MethodNode node) {
        String mappedRender = Mapping.get(GameRenderer.class, "render", "(FJZ)V");
        boolean replaced = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(GameRenderer.class))
                    && m.name.equals(mappedRender)
                    && m.desc.equals("(FJZ)V")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(MinecraftTransformer.class),
                        "renderWrapper",
                        "(Lnet/minecraft/client/renderer/GameRenderer;FJZ)V",
                        false
                );
                node.instructions.set(m, redirect);
                replaced = true;
                break;
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][MinecraftTransformer] Failed to wrap GameRenderer.render in runTick");
        }
    }

    @Inject(method = "handleKeybinds", desc = "()V")
    private void injectHandleKeybindsClickEvent(MethodNode node) {
        String mappedIsUsing = Mapping.get(LocalPlayer.class, "isUsingItem", "()Z");
        boolean inserted = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(LocalPlayer.class))
                    && m.name.equals(mappedIsUsing)
                    && m.desc.equals("()Z")) {
                InsnList list = new InsnList();
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(MinecraftTransformer.class),
                        "clickEvent",
                        "()Z",
                        false
                ));
                LabelNode cont = new LabelNode();
                list.add(new JumpInsnNode(Opcodes.IFEQ, cont));
                list.add(new InsnNode(Opcodes.RETURN));
                list.add(cont);
                try {
                    node.instructions.insertBefore(m, list);
                    inserted = true;
                } catch (Throwable ex) {
                    if (MixinLoader.debugging) {
                        System.out.println("[ASM][MinecraftTransformer] Failed to insert clickEvent in handleKeybinds: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
                break; // ordinal 0
            }
        }
        if (!inserted && MixinLoader.debugging) {
            System.out.println("[ASM][MinecraftTransformer] Did not find LocalPlayer.isUsingItem in handleKeybinds");
        }
    }
}


