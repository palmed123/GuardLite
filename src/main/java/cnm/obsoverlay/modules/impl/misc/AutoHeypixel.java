package cnm.obsoverlay.modules.impl.misc;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.EventPacket;
import cnm.obsoverlay.files.FileManager;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.utils.ChatUtils;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import net.minecraft.client.Screenshot;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ModuleInfo(name = "AutoHeypixel", description = "Auto play Heypixel server.", category = Category.MISC)
public class AutoHeypixel extends Module {
    public BooleanValue autoScreenshot = ValueBuilder.create(this, "Auto Screenshot").setDefaultBooleanValue(true).build().getBooleanValue();
    @EventTarget
    public void onPacker(EventPacket event) {
        if (mc.player == null || mc.level == null) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundSetTitleTextPacket) {
            boolean win = ((ClientboundSetTitleTextPacket) packet).getText().getString().contains("胜利");
            if (win && autoScreenshot.currentValue) {
                // 切换到其他线程等待1秒后再回到渲染线程截图
                CompletableFuture.runAsync(() -> {
                    try {
                        // 在异步线程中等待1秒
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).thenRun(() -> {
                    // 切回渲染线程执行截图
                    mc.execute(() -> {
                        if (mc.player != null && mc.level != null) {
                            Screenshot.grab(
                                    FileManager.clientFolder,
                                    mc.getMainRenderTarget(),
                                    (message) -> {
                                        ChatUtils.addChatMessage(message.getString());
                                    }
                            );
                        }
                    });
                });
            }
        }
    }



}
