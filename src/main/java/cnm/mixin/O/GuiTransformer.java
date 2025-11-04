package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventRenderScoreboard;
import cnm.obsoverlay.events.impl.EventSetTitle;
import cnm.obsoverlay.modules.impl.render.NoRender;
import cnm.obsoverlay.modules.impl.render.Scoreboard;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class GuiTransformer extends ASMTransformer {
    public GuiTransformer() {
        super(Gui.class);
    }

    // ===== Helpers =====
    public static void scoreboardHead(GuiGraphics g) {
        g.pose().pushPose();
        Scoreboard module = (Scoreboard) Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
        if (module != null && module.isEnabled()) {
            g.pose().translate(0.0F, module.down.getCurrentValue(), 0.0F);
        }
    }

    public static void scoreboardReturn(GuiGraphics g) {
        g.pose().popPose();
    }

    public static int hookRenderScore(GuiGraphics instance, Font font, String text, int x, int y, int color, boolean dropShadow) {
        Scoreboard module = (Scoreboard) Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
        if (module != null && module.isEnabled() && module.hideScore.getCurrentValue()) return 0;
        return instance.drawString(font, text, x, y, color);
    }

    public static MutableComponent hookScoreboardName(Team team, Component name) {
        MutableComponent mc = PlayerTeam.formatNameForTeam(team, name);
        EventRenderScoreboard event = new EventRenderScoreboard(mc);
        Naven.getInstance().getEventManager().call(event);
        return (MutableComponent) event.getComponent();
    }

    public static Component hookScoreboardTitle(Objective objective) {
        Component c = objective.getDisplayName();
        EventRenderScoreboard event = new EventRenderScoreboard(c);
        Naven.getInstance().getEventManager().call(event);
        return event.getComponent();
    }

    public static Component onSetTitle(Component in) {
        EventSetTitle event = new EventSetTitle(EventType.TITLE, in);
        Naven.getInstance().getEventManager().call(event);
        if (!event.isCancelled()) return event.getTitle();
        return null;
    }

    public static Component onSetSubtitle(Component in) {
        EventSetTitle event = new EventSetTitle(EventType.SUBTITLE, in);
        Naven.getInstance().getEventManager().call(event);
        if (!event.isCancelled()) return event.getTitle();
        return null;
    }

    public static boolean shouldCancelRenderEffects() {
        NoRender noRender = (NoRender) Naven.getInstance().getModuleManager().getModule(NoRender.class);
        return noRender != null && noRender.isEnabled() && noRender.disableEffects.getCurrentValue();
    }

    // ===== Injections =====
    @Inject(method = "displayScoreboardSidebar", desc = "(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V")
    private void injectScoreboardHead(MethodNode node) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 1));
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(GuiTransformer.class),
                "scoreboardHead",
                "(Lnet/minecraft/client/gui/GuiGraphics;)V",
                false
        ));
        node.instructions.insert(list);
    }

    @Inject(method = "displayScoreboardSidebar", desc = "(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V")
    private void injectScoreboardReturn(MethodNode node) {
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                InsnList list = new InsnList();
                list.add(new VarInsnNode(Opcodes.ALOAD, 1));
                list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(GuiTransformer.class),
                        "scoreboardReturn",
                        "(Lnet/minecraft/client/gui/GuiGraphics;)V",
                        false
                ));
                node.instructions.insertBefore(insn, list);
            }
        }
    }

    @Inject(method = "displayScoreboardSidebar", desc = "(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V")
    private void redirectScoreText(MethodNode node) {
        String mappedDrawString = Mapping.get(GuiGraphics.class, "drawString", "(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I");
        boolean replaced = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(GuiGraphics.class))
                    && m.name.equals(mappedDrawString)
                    && m.desc.equals("(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(GuiTransformer.class),
                        "hookRenderScore",
                        "(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
                        false
                );
                node.instructions.set(m, redirect);
                replaced = true;
                break;
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][GuiTransformer] Failed to redirect GuiGraphics.drawString in displayScoreboardSidebar");
        }
    }

    @Inject(method = "displayScoreboardSidebar", desc = "(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V")
    private void redirectScoreName(MethodNode node) {
        String mappedFormat = Mapping.get(PlayerTeam.class, "formatNameForTeam", "(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;");
        boolean replaced = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKESTATIC
                    && m.owner.equals(Type.getInternalName(PlayerTeam.class))
                    && m.name.equals(mappedFormat)
                    && m.desc.equals("(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(GuiTransformer.class),
                        "hookScoreboardName",
                        "(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;",
                        false
                );
                node.instructions.set(m, redirect);
                replaced = true;
                break;
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][GuiTransformer] Failed to redirect PlayerTeam.formatNameForTeam in displayScoreboardSidebar");
        }
    }

    @Inject(method = "displayScoreboardSidebar", desc = "(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V")
    private void redirectScoreTitle(MethodNode node) {
        String mappedGetDisplayName = Mapping.get(Objective.class, "getDisplayName", "()Lnet/minecraft/network/chat/Component;");
        boolean replaced = false;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m
                    && m.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && m.owner.equals(Type.getInternalName(Objective.class))
                    && m.name.equals(mappedGetDisplayName)
                    && m.desc.equals("()Lnet/minecraft/network/chat/Component;")) {
                MethodInsnNode redirect = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(GuiTransformer.class),
                        "hookScoreboardTitle",
                        "(Lnet/minecraft/world/scores/Objective;)Lnet/minecraft/network/chat/Component;",
                        false
                );
                node.instructions.set(m, redirect);
                replaced = true;
                break;
            }
        }
        if (!replaced && MixinLoader.debugging) {
            System.out.println("[ASM][GuiTransformer] Failed to redirect Objective.getDisplayName in displayScoreboardSidebar");
        }
    }

    @Inject(method = "setTitle", desc = "(Lnet/minecraft/network/chat/Component;)V")
    private void injectSetTitle(MethodNode node) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 1));
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(GuiTransformer.class),
                "onSetTitle",
                "(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;",
                false
        ));
        LabelNode cont = new LabelNode();
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new JumpInsnNode(Opcodes.IFNULL, cont));

        String fTitle = Mapping.get(Gui.class, "title", null);
        String fTitleTime = Mapping.get(Gui.class, "titleTime", null);
        String fFadeIn = Mapping.get(Gui.class, "titleFadeInTime", null);
        String fStay = Mapping.get(Gui.class, "titleStayTime", null);
        String fFadeOut = Mapping.get(Gui.class, "titleFadeOutTime", null);

        // this.title = <ret>
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new InsnNode(Opcodes.SWAP));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD, Type.getInternalName(Gui.class), fTitle, Type.getDescriptor(Component.class)));

        // this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, Type.getInternalName(Gui.class), fFadeIn, "I"));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, Type.getInternalName(Gui.class), fStay, "I"));
        list.add(new InsnNode(Opcodes.IADD));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, Type.getInternalName(Gui.class), fFadeOut, "I"));
        list.add(new InsnNode(Opcodes.IADD));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new InsnNode(Opcodes.SWAP));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD, Type.getInternalName(Gui.class), fTitleTime, "I"));
        list.add(new InsnNode(Opcodes.RETURN));

        list.add(cont);
        list.add(new InsnNode(Opcodes.POP)); // pop null
        node.instructions.insert(list);
    }

    @Inject(method = "setSubtitle", desc = "(Lnet/minecraft/network/chat/Component;)V")
    private void injectSetSubtitle(MethodNode node) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 1));
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(GuiTransformer.class),
                "onSetSubtitle",
                "(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;",
                false
        ));
        LabelNode cont = new LabelNode();
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new JumpInsnNode(Opcodes.IFNULL, cont));

        String fSubtitle = Mapping.get(Gui.class, "subtitle", null);
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new InsnNode(Opcodes.SWAP));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD, Type.getInternalName(Gui.class), fSubtitle, Type.getDescriptor(Component.class)));
        list.add(new InsnNode(Opcodes.RETURN));

        list.add(cont);
        list.add(new InsnNode(Opcodes.POP));
        node.instructions.insert(list);
    }

    @Inject(method = "renderEffects", desc = "(Lnet/minecraft/client/gui/GuiGraphics;)V")
    private void injectRenderEffects(MethodNode node) {
        InsnList list = new InsnList();
        LabelNode cont = new LabelNode();
        list.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(GuiTransformer.class),
                "shouldCancelRenderEffects",
                "()Z",
                false
        ));
        list.add(new JumpInsnNode(Opcodes.IFEQ, cont));
        list.add(new InsnNode(Opcodes.RETURN));
        list.add(cont);
        node.instructions.insert(list);
    }
}


