package cnm.obsoverlay.modules.impl.render;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventRender2D;
import cnm.obsoverlay.events.impl.EventShader;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.modules.ModuleManager;
import cnm.obsoverlay.modules.impl.move.Scaffold;
import cnm.obsoverlay.utils.*;
import cnm.obsoverlay.utils.auth.AuthUser;
import cnm.obsoverlay.utils.auth.AuthUtils;
import cnm.obsoverlay.utils.math.MathUtils;
import cnm.obsoverlay.utils.renderer.Fonts;
import cnm.obsoverlay.utils.renderer.text.CustomTextRenderer;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import cnm.obsoverlay.values.impl.FloatValue;
import cnm.obsoverlay.values.impl.ModeValue;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static cnm.obsoverlay.utils.InventoryUtils.getBlockCountInHotbar;

@ModuleInfo(
        name = "HUD",
        description = "Displays information on your screen",
        category = Category.RENDER
)
public class HUD extends Module {
    public static final int headerColor = new Color(255, 255, 255, 100).getRGB();
    public static final int bodyColor = new Color(0, 0, 0, 100).getRGB();
    private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
    private final SmoothAnimationTimer blockCountAnimation = new SmoothAnimationTimer(100.0F);
    public BooleanValue waterMark = ValueBuilder.create(this, "Water Mark").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue stats = ValueBuilder.create(this, "Stats").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue moduleToggleSound = ValueBuilder.create(this, "Module Toggle Sound").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue blockCount = ValueBuilder.create(this, "Block Count").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue notification = ValueBuilder.create(this, "Notification").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue arrayList = ValueBuilder.create(this, "Array List").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue prettyModuleName = ValueBuilder.create(this, "Array List Pretty Module Name")
            .setOnUpdate(value -> Module.update = true)
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public BooleanValue arrayListShortLine = ValueBuilder.create(this, "Array List Short Line")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue arrayListRound = ValueBuilder.create(this, "Array List Round")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue hideRenderModules = ValueBuilder.create(this, "ArrayList Hide Render Modules")
            .setOnUpdate(value -> Module.update = true)
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue rainbow = ValueBuilder.create(this, "ArrayList Rainbow")
            .setDefaultBooleanValue(true)
            .setVisibility(this.arrayList::getCurrentValue)
            .build()
            .getBooleanValue();
    public FloatValue rainbowSpeed = ValueBuilder.create(this, "ArrayList Rainbow Speed")
            .setVisibility(this.arrayList::getCurrentValue)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(15.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public FloatValue rainbowOffset = ValueBuilder.create(this, "ArrayList Rainbow Offset")
            .setVisibility(this.arrayList::getCurrentValue)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(10.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public FloatValue xOffset = ValueBuilder.create(this, "ArrayList X Offset")
            .setVisibility(this.arrayList::getCurrentValue)
            .setMinFloatValue(-100.0F)
            .setMaxFloatValue(100.0F)
            .setDefaultFloatValue(-5.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public FloatValue yOffset = ValueBuilder.create(this, "ArrayList Y Offset")
            .setVisibility(this.arrayList::getCurrentValue)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(100.0F)
            .setDefaultFloatValue(5.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public FloatValue arrayListSize = ValueBuilder.create(this, "ArrayList Size")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultFloatValue(0.25F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();
    public FloatValue arrayListMargin = ValueBuilder.create(this, "ArrayList Margin")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultFloatValue(7.5F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    public ModeValue arrayListDirection = ValueBuilder.create(this, "ArrayList Direction")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes("Right", "Left")
            .build()
            .getModeValue();
    List<Module> renderModules;
    List<Vector4f> blurMatrices = new ArrayList<>();
    List<Vector4f> statsBlur = new ArrayList<>();
    List<Vector4f> crash = new ArrayList<>();

    public String getModuleDisplayName(Module module) {
        String name = this.prettyModuleName.getCurrentValue() ? module.getPrettyName() : module.getName();
        return name + (module.getSuffix() == null ? "" : " §7" + module.getSuffix());
    }

    @EventTarget
    public void notification(EventRender2D e) {
        if (this.notification.getCurrentValue()) {
            Naven.getInstance().getNotificationManager().onRender(e);
        }
    }

    @EventTarget
    public void onShader(EventShader e) {
        // BLUR 和 SHADOW 事件都需要绘制蒙版，但只有 SHADOW 需要绘制通知
        if (this.notification.getCurrentValue() && e.getType() == EventType.SHADOW) {
            Naven.getInstance().getNotificationManager().onRenderShadow(e);
        }

        if (this.stats.getCurrentValue()) {
            for (Vector4f statsBlur : this.statsBlur) {
                RenderUtils.drawRoundedRect(e.getStack(), statsBlur.x(), statsBlur.y(), statsBlur.z(), statsBlur.w(), 5.0f, bodyColor);
            }
        }

        if (this.arrayList.getCurrentValue()) {
            for (Vector4f blurMatrix : this.blurMatrices) {
                RenderUtils.fillBound(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), bodyColor);
            }
        }

//        CustomTextRenderer tenacity = Fonts.tenacity;
//        if (this.waterMark.getCurrentValue()) {
//            e.getStack().pushPose();
//            tenacity.render(e.getStack(), "Guard", 10.0f, 10.0f, Color.white, true, 0.75f);
//            double width = tenacity.getWidth("Guard", true, 0.75f);
//            double height = tenacity.getHeight(true, 0.75f);
//            double liteHeight = tenacity.getHeight(true, 0.5f);
//            tenacity.render(e.getStack(), "Lite", 11.0f + width, 10.0f + height - liteHeight - 1.0f, Color.white, true, 0.5f);
//            e.getStack().popPose();
//        }

    }

    @EventTarget
    public void onRender(EventRender2D e) {
        if (mc.player == null) return;
        CustomTextRenderer font = Fonts.MiSans_Medium;
        CustomTextRenderer tenacity = Fonts.tenacity;

        if (this.waterMark.getCurrentValue()) {
            e.getStack().pushPose();
            tenacity.render(e.getStack(), "Guard", 10.0f, 10.0f, Color.white, true, 0.75f);
            double width = tenacity.getWidth("Guard", true, 0.75f);
            double height = tenacity.getHeight(true, 0.75f);
            double liteHeight = tenacity.getHeight(true, 0.5f);
            tenacity.render(e.getStack(), "Lite", 11.0f + width, 10.0f + height - liteHeight - 1.0f, Color.white, true, 0.5f);
            e.getStack().popPose();
        }

        this.blurMatrices.clear();
        if (this.arrayList.getCurrentValue()) {
            e.getStack().pushPose();
            ModuleManager moduleManager = Naven.getInstance().getModuleManager();
            if (update || this.renderModules == null) {
                this.renderModules = new ArrayList<>(moduleManager.getModules());
                if (this.hideRenderModules.getCurrentValue()) {
                    this.renderModules.removeIf(modulex -> modulex.getCategory() == Category.RENDER);
                }

                this.renderModules.sort((o1, o2) -> {
                    float o1Width = font.getWidth(this.getModuleDisplayName(o1), this.arrayListSize.getCurrentValue());
                    float o2Width = font.getWidth(this.getModuleDisplayName(o2), this.arrayListSize.getCurrentValue());
                    return Float.compare(o2Width, o1Width);
                });
            }

            float maxWidth = this.renderModules.isEmpty()
                    ? 0.0F
                    : font.getWidth(this.getModuleDisplayName(this.renderModules.get(0)), this.arrayListSize.getCurrentValue());
            float arrayListX = this.arrayListDirection.isCurrentMode("Right")
                    ? (float) mc.getWindow().getGuiScaledWidth() - maxWidth - 6.0F + this.xOffset.getCurrentValue()
                    : 3.0F + this.xOffset.getCurrentValue();
            float arrayListY = this.yOffset.getCurrentValue();
            float height = 0.0F;
            double fontHeight = font.getHeight(true, this.arrayListSize.getCurrentValue());

            for (Module module : this.renderModules) {
                SmoothAnimationTimer animation = module.getAnimation();
                if (module.isEnabled()) {
                    animation.target = 100.0F;
                } else {
                    animation.target = 0.0F;
                }

                animation.update(true);
                if (animation.value > 0.0F) {
                    String displayName = this.getModuleDisplayName(module);
                    float stringWidth = font.getWidth(displayName, this.arrayListSize.getCurrentValue());
                    float left = -stringWidth * (1.0F - animation.value / 100.0F);
                    float right = maxWidth - stringWidth * (animation.value / 100.0F);
                    float innerX = this.arrayListDirection.isCurrentMode("Left") ? left : right;
                    float margin = arrayListSize.getCurrentValue() * arrayListMargin.getCurrentValue();

                    if (arrayListRound.currentValue) {
                        StencilUtils.write(false);
                        if (this.arrayListDirection.isCurrentMode("Right")) {
                            RenderUtils.drawRoundedRect(e.getStack(), arrayListX + innerX, arrayListY + height, stringWidth + margin * 2.0f + 1.0f + margin, (float) ((animation.value / 100.0F) * (fontHeight + margin * 2.0f)), margin, -1);
                        } else {
                            RenderUtils.drawRoundedRect(e.getStack(), arrayListX + innerX - margin, arrayListY + height, stringWidth + margin * 2.0f + 1.0f + margin, (float) ((animation.value / 100.0F) * (fontHeight + margin * 2.0f)), margin, -1);
                        }
                        StencilUtils.erase(true);
                        RenderUtils.fillBound(e.getStack(), arrayListX + innerX, arrayListY + height, stringWidth + margin * 2.0f + 1.0f, (float) ((animation.value / 100.0F) * (fontHeight + margin * 2.0f)), bodyColor);
                        StencilUtils.dispose();
                    } else {
                        RenderUtils.fillBound(e.getStack(), arrayListX + innerX, arrayListY + height, stringWidth + margin * 2.0f + 1.0f, (float) ((animation.value / 100.0F) * (fontHeight + margin * 2.0f)), bodyColor);
                    }

                    this.blurMatrices
                            .add(
                                    new Vector4f(
                                            arrayListX + innerX,
                                            arrayListY + height,
                                            stringWidth + margin * 2.0f + 1.0f,
                                            (float) ((animation.value / 100.0F) * (fontHeight + margin * 2.0f))
                                    )
                            );
                    int color = -1;
                    if (this.rainbow.getCurrentValue()) {
                        color = RenderUtils.getRainbowOpaque(
                                (int) (-height * this.rainbowOffset.getCurrentValue()), 1.0F, 1.0F, (21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F
                        );
                    }

                    if (arrayListShortLine.currentValue) RenderUtils.fillBound(e.getStack(), arrayListX + innerX + stringWidth + margin * 2.0f, arrayListY + height + margin, 2.0F, (float) ((animation.value / 100.0F) * fontHeight), color);

                    float alpha = animation.value / 100.0F;
                    font.setAlpha(alpha);
                    font.render(
                            e.getStack(),
                            displayName,
                            arrayListX + innerX + margin,
                            arrayListY + height + margin,
                            new Color(color),
                            true,
                            this.arrayListSize.getCurrentValue()
                    );
                    height += (float) ((double) (animation.value / 100.0F) * (fontHeight + margin * 2.0f));
                }
            }

            font.setAlpha(1.0F);
            e.getStack().popPose();
        }

        if (blockCount.currentValue) {
            boolean onScaffold = Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled();
            if (onScaffold) blockCountAnimation.target = 100; else blockCountAnimation.target = 0;
            blockCountAnimation.update(true);

            int count = getBlockCountInHotbar();
            String text = count + " Block" + (count != 1 ? "s" : "");

            float width = (float) (27.0f + font.getWidth(text, true, 0.4f));
            float height = (float) (font.getHeight(true, 0.4) + 7f);

            float x = mc.getWindow().getGuiScaledWidth() / 2.0f - width / 2.0f;
            float y = mc.getWindow().getGuiScaledHeight() / 2.0f + 50.0f;
            float animationProgress = blockCountAnimation.value / 100.0f;

            e.getStack().pushPose();
            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(e.getStack(), mc.getWindow().getGuiScaledWidth() / 2.0f - (width / 2.0f) * animationProgress, y, width * animationProgress, height, 5.0F, Integer.MIN_VALUE);
            StencilUtils.erase(true);
            RenderUtils.fillBound(e.getStack(), x, y, 20.0f, height, headerColor);
            RenderUtils.fillBound(e.getStack(), x + 20.0f, y, width, height, bodyColor);
            RenderUtils.renderGuiItem(e.getStack(), mc.player.getMainHandItem(), x + 2.0f, y + 2.0f);
            font.render(e.getStack(), text, x + 23.5f, y + 3.5f, Color.white, true, 0.4F);
            StencilUtils.dispose();
            e.getStack().popPose();
            blurMatrices.add(new Vector4f(mc.getWindow().getGuiScaledWidth() / 2.0f - (width / 2.0f) * animationProgress, y, width * animationProgress, height));
        }

        statsBlur.clear();
        if (this.stats.getCurrentValue()) {
//            CustomTextRenderer tenacity = Fonts.tenacity;
            String fps = mc.getFps() + "";
            String username = AuthUser.username;
            if (username == null) {
                username = "null";
            }
            float height = (float) tenacity.getHeight(false, 0.35f) + 6.0f;
            float textHeight = (float) tenacity.getHeight(false, 0.35f);
            float headSize = textHeight + 2f;
            float width = (float) (tenacity.getWidth(fps + " FPS" + username, false, 0.35f) + 17.0f + headSize);
            float fpsWidth = (float) tenacity.getWidth(fps + " FPS", false, 0.35f);
            float nameWidth = (float) tenacity.getWidth(username, false, 0.35f);
            float x = mc.getWindow().getGuiScaledWidth() - 5.0f - width;
            float y = mc.getWindow().getGuiScaledHeight() - 5.0f - height;

            e.getStack().pushPose();

            RenderUtils.drawRoundedRect(e.getStack(), x, y, fpsWidth + 6.0f, height, 5.0f, bodyColor);
            statsBlur.add(new Vector4f(x, y, fpsWidth + 6.0f, height));
            RenderUtils.drawRoundedRect(e.getStack(), x + fpsWidth + 11.0f, y, nameWidth + 6.0f + headSize, height, 5.0f, bodyColor);
            statsBlur.add(new Vector4f(x + fpsWidth + 11.0f, y, nameWidth + 6.0f + headSize, height));

            tenacity.render(e.getStack(), fps + " FPS", x + 3.0f, y + 3.0f, Color.white, false, 0.35f);
            tenacity.render(e.getStack(), username, x + fpsWidth + 14.0f + headSize, y + 3.0f, Color.white, false, 0.35f);

            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(e.getStack(), x + fpsWidth + 13.0f, y + 2.0f, headSize, headSize, headSize * 0.5f, bodyColor);
            StencilUtils.erase(true);
            RenderUtils.drawPlayerHead(e.getStack(), x + fpsWidth + 13.0f, y + 2.0f, headSize, headSize, mc.player);
            StencilUtils.dispose();

            e.getStack().popPose();
        }

        if (AuthUtils.errorCount > 3) {
            e.getStack().pushPose();
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();
            crash.add(new Vector4f(MathUtils.getRandomIntInRange(0, width),  MathUtils.getRandomIntInRange(0, height), MathUtils.getRandomIntInRange(0, width),  MathUtils.getRandomIntInRange(0, height)));
            for (Vector4f vec4f : crash) {
                RenderUtils.fill(e.getStack(), vec4f.x(), vec4f.y(), vec4f.z(), vec4f.w(), new Color(MathUtils.getRandomIntInRange(0, 255), MathUtils.getRandomIntInRange(0, 255), MathUtils.getRandomIntInRange(0, 255), MathUtils.getRandomIntInRange(0, 255)).getRGB());
            }
            e.getStack().popPose();
        }
    }

    @Override
    public void onEnable() {
        blockCountAnimation.speed = 0.5f;
    }

    @Override
    public void onDisable() {
    }
}
