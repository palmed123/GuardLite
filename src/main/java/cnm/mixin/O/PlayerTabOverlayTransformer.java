package cnm.mixin.O;

import cnm.mixin.O.mapping.Mapping;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventRenderTabOverlay;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class PlayerTabOverlayTransformer extends ASMTransformer {
    public PlayerTabOverlayTransformer() {
        super(PlayerTabOverlay.class);
    }

    public static List<FormattedCharSequence> hookHeader(Font instance, FormattedText pText, int pMaxWidth) {
        Component component = (Component) pText;
        EventRenderTabOverlay event = new EventRenderTabOverlay(EventType.HEADER, component);
        Naven.getInstance().getEventManager().call(event);
        return instance.split(event.getComponent(), pMaxWidth);
    }

    public static List<FormattedCharSequence> hookFooter(Font instance, FormattedText pText, int pMaxWidth) {
        Component component = (Component) pText;
        EventRenderTabOverlay event = new EventRenderTabOverlay(EventType.FOOTER, component);
        Naven.getInstance().getEventManager().call(event);
        return instance.split(event.getComponent(), pMaxWidth);
    }

    public static Component hookName(PlayerTabOverlay instance, PlayerInfo pPlayerInfo) {
        Component nameForDisplay = instance.getNameForDisplay(pPlayerInfo);
        EventRenderTabOverlay event = new EventRenderTabOverlay(EventType.NAME, nameForDisplay);
        Naven.getInstance().getEventManager().call(event);
        return event.getComponent();
    }

    @Inject(method = "render", desc = "(Lnet/minecraft/client/gui/GuiGraphics;IILcom/mojang/authlib/GameProfile;)V")
    private void injectRender(MethodNode node) {
        String mappedSplit = Mapping.get(Font.class, "split", "(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;");
        String mappedGetName = Mapping.get(PlayerTabOverlay.class, "getNameForDisplay", "(Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;");

        int splitOrdinal = 0;
        boolean replacedHeader = false;
        boolean replacedFooter = false;
        boolean replacedName = false;

        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof MethodInsnNode m) {
                if (m.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && m.owner.equals(Type.getInternalName(Font.class))
                        && m.name.equals(mappedSplit)
                        && m.desc.equals("(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;")) {
                    MethodInsnNode redirect = new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(PlayerTabOverlayTransformer.class),
                            splitOrdinal == 0 ? "hookHeader" : "hookFooter",
                            "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",
                            false
                    );
                    node.instructions.set(m, redirect);
                    if (splitOrdinal == 0) replacedHeader = true;
                    else replacedFooter = true;
                    splitOrdinal++;
                } else if (m.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && m.owner.equals(Type.getInternalName(PlayerTabOverlay.class))
                        && m.name.equals(mappedGetName)
                        && m.desc.equals("(Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;")) {
                    MethodInsnNode redirect = new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(PlayerTabOverlayTransformer.class),
                            "hookName",
                            "(Lnet/minecraft/client/gui/components/PlayerTabOverlay;Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;",
                            false
                    );
                    node.instructions.set(m, redirect);
                    replacedName = true;
                }
            }
        }

        if (MixinLoader.debugging) {
            if (!replacedHeader)
                System.out.println("[ASM][PlayerTabOverlayTransformer] Failed to redirect Font.split HEADER in render");
            if (!replacedFooter)
                System.out.println("[ASM][PlayerTabOverlayTransformer] Failed to redirect Font.split FOOTER in render");
            if (!replacedName)
                System.out.println("[ASM][PlayerTabOverlayTransformer] Failed to redirect getNameForDisplay in render");
        }
    }
}


