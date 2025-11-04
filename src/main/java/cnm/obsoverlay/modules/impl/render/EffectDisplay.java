package cnm.obsoverlay.modules.impl.render;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.EventRender2D;
import cnm.obsoverlay.events.impl.EventShader;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.utils.RenderUtils;
import cnm.obsoverlay.utils.SmoothAnimationTimer;
import cnm.obsoverlay.utils.StencilUtils;
import cnm.obsoverlay.utils.renderer.Fonts;
import cnm.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInfo(
        name = "EffectDisplay",
        description = "Displays potion effects on the HUD",
        category = Category.RENDER
)
public class EffectDisplay extends Module {
    private final Map<MobEffect, MobEffectInfo> infos = new ConcurrentHashMap<>();
    private final Color headerColor = new Color(255, 255, 255, 100);
    private final Color bodyColor = new Color(0, 0, 0, 50);
    private final List<Vector4f> blurMatrices = new ArrayList<>();
    private List<Runnable> list;

    @EventTarget(4)
    public void renderIcons(EventRender2D e) {
        this.list.forEach(Runnable::run);
    }

    @EventTarget
    public void onShader(EventShader e) {
        for (Vector4f matrix : this.blurMatrices) {
            RenderUtils.drawRoundedRect(e.getStack(), matrix.x(), matrix.y(), matrix.z(), matrix.w(), 5.0F, HUD.bodyColor);
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            MobEffectInfo info;
            if (this.infos.containsKey(effect.getEffect())) {
                info = this.infos.get(effect.getEffect());
            } else {
                info = new MobEffectInfo();
                this.infos.put(effect.getEffect(), info);
            }

            info.maxDuration = Math.max(info.maxDuration, effect.getDuration());
            info.duration = effect.getDuration();
            info.amplifier = effect.getAmplifier();
            info.shouldDisappear = false;
        }

        int startY = mc.getWindow().getGuiScaledHeight() / 2 - this.infos.size() * 16;
        this.list = Lists.newArrayListWithExpectedSize(this.infos.size());
        this.blurMatrices.clear();

        for (Entry<MobEffect, MobEffectInfo> entry : this.infos.entrySet()) {
            e.getStack().pushPose();
            MobEffectInfo effectInfo = entry.getValue();
            String text = this.getDisplayName(entry.getKey(), effectInfo);
            if (effectInfo.yTimer.value == -1.0F) {
                effectInfo.yTimer.value = (float) startY;
            }

            CustomTextRenderer MiSans_Medium = Fonts.MiSans_Medium;
            effectInfo.width = MiSans_Medium.getWidth(text, 0.3) + 18.0F;
            float x = effectInfo.xTimer.value;
            float y = effectInfo.yTimer.value;
            effectInfo.shouldDisappear = !mc.player.hasEffect(entry.getKey());
            if (effectInfo.shouldDisappear) {
                effectInfo.xTimer.target = -(effectInfo.width + 27.0f) - 20.0F;
                if (x <= -effectInfo.width - 20.0F) {
                    this.infos.remove(entry.getKey());
                }
            } else {
                effectInfo.durationTimer.target = (float) effectInfo.duration / (float) effectInfo.maxDuration * effectInfo.width;
                if (effectInfo.durationTimer.value <= 0.0F) {
                    effectInfo.durationTimer.value = effectInfo.durationTimer.target;
                }

                effectInfo.xTimer.target = 10.0F;
                effectInfo.yTimer.target = (float) startY;
                effectInfo.yTimer.update(true);
            }

            effectInfo.durationTimer.update(true);
            effectInfo.xTimer.update(true);
            StencilUtils.write(false);
            this.blurMatrices.add(new Vector4f(x + 2.0F, y + 2.0F, effectInfo.width - 2.0F + 27.0f, 28.0F));
            RenderUtils.drawRoundedRect(e.getStack(), x + 2.0F, y + 2.0F, effectInfo.width - 2.0F + 27.0f, 28.0F, 5.0F, -1);
            StencilUtils.erase(true);
            RenderUtils.fillBound(e.getStack(), x, y, 27, 30.0F, headerColor.getRGB());
            RenderUtils.fillBound(e.getStack(), x + 27, y, effectInfo.width, 30.0F, this.bodyColor.getRGB());
            RenderUtils.fillBound(e.getStack(), x + 27, y, effectInfo.durationTimer.value, 30.0F, this.bodyColor.getRGB());
            MiSans_Medium.render(e.getStack(), text, x + 33.0F, y + 7.0F, Color.white, true, 0.3);
            String duration = StringUtil.formatTickDuration(effectInfo.duration);
            MiSans_Medium.render(e.getStack(), duration, x + 33.0F, y + 17.0F, Color.LIGHT_GRAY, true, 0.25);
            MobEffectTextureManager mobeffecttexturemanager = mc.getMobEffectTextures();
            TextureAtlasSprite textureatlassprite = mobeffecttexturemanager.get(entry.getKey());
            this.list.add(() -> {
                RenderSystem.setShaderTexture(0, textureatlassprite.atlasLocation());
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                e.getGuiGraphics().blit((int) (x + 6.0F), (int) (y + 8.0F), 1, 18, 18, textureatlassprite);
            });
            StencilUtils.dispose();
            startY += 34;
            e.getStack().popPose();
        }
    }

    public String getDisplayName(MobEffect effect, MobEffectInfo info) {
        String effectName = effect.getDisplayName().getString();
        String amplifierName;
        if (info.amplifier == 0) {
            amplifierName = "";
        } else if (info.amplifier == 1) {
            amplifierName = " " + I18n.get("enchantment.level.2");
        } else if (info.amplifier == 2) {
            amplifierName = " " + I18n.get("enchantment.level.3");
        } else if (info.amplifier == 3) {
            amplifierName = " " + I18n.get("enchantment.level.4");
        } else {
            amplifierName = " " + info.amplifier;
        }

        return effectName + amplifierName;
    }

    public static class MobEffectInfo {
        public SmoothAnimationTimer xTimer = new SmoothAnimationTimer(-60.0F, 0.2F);
        public SmoothAnimationTimer yTimer = new SmoothAnimationTimer(-1.0F, 0.2F);
        public SmoothAnimationTimer durationTimer = new SmoothAnimationTimer(-1.0F, 0.2F);
        public int maxDuration = -1;
        public int duration = 0;
        public int amplifier = 0;
        public boolean shouldDisappear = false;
        public float width;
    }
}
