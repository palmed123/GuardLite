package cnm.obsoverlay.modules;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.modules.impl.render.ClickGUIModule;
import cnm.obsoverlay.modules.impl.render.HUD;
import cnm.obsoverlay.ui.notification.Notification;
import cnm.obsoverlay.ui.notification.NotificationLevel;
import cnm.obsoverlay.utils.SmoothAnimationTimer;
import cnm.obsoverlay.utils.Wrapper;
import cnm.obsoverlay.values.HasValue;
import net.minecraft.sounds.SoundEvents;

public class Module extends HasValue implements Wrapper {
    public static boolean update = true;
    private final SmoothAnimationTimer animation = new SmoothAnimationTimer(100.0F);
    private String name;
    private String prettyName;
    private String description;
    private String suffix;
    private Category category;
    private boolean enabled;
    private int minPermission = 0;
    private int key;

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        super.setName(name);
        this.setPrettyName();
    }

    public Module() {
    }

    private void setPrettyName() {
        StringBuilder builder = new StringBuilder();
        char[] chars = this.name.toCharArray();

        for (int i = 0; i < chars.length - 1; i++) {
            if (Character.isLowerCase(chars[i]) && Character.isUpperCase(chars[i + 1])) {
                builder.append(chars[i]).append(" ");
            } else {
                builder.append(chars[i]);
            }
        }

        builder.append(chars[chars.length - 1]);
        this.prettyName = builder.toString();
    }

    protected void initModule() {
        if (this.getClass().isAnnotationPresent(ModuleInfo.class)) {
            ModuleInfo moduleInfo = this.getClass().getAnnotation(ModuleInfo.class);
            this.name = moduleInfo.name();
            this.description = moduleInfo.description();
            this.category = moduleInfo.category();
            super.setName(this.name);
            this.setPrettyName();
            Naven.getInstance().getHasValueManager().registerHasValue(this);
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void toggle() {
        this.setEnabled(!this.enabled);
    }

    public SmoothAnimationTimer getAnimation() {
        return this.animation;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getPrettyName() {
        return this.prettyName;
    }

    public String getDescription() {
        return this.description;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setSuffix(String suffix) {
        if (suffix == null) {
            this.suffix = null;
            update = true;
        } else if (!suffix.equals(this.suffix)) {
            this.suffix = suffix;
            update = true;
        }
    }

    public Category getCategory() {
        return this.category;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        try {
            Naven naven = Naven.getInstance();
            if (enabled) {
                if (!this.enabled) {
                    this.enabled = true;
                    naven.getEventManager().register(this);
                    this.onEnable();
                    if (!(this instanceof ClickGUIModule)) {
                        HUD module = Naven.getInstance().getModuleManager().getModule(HUD.class);
                        if (module.moduleToggleSound.getCurrentValue()) {
                            mc.player.playSound(SoundEvents.WOODEN_BUTTON_CLICK_ON, 0.5F, 1.3F);
                        }

                        Notification notification = new Notification(NotificationLevel.SUCCESS, this.name + " Enabled!", 3000L);
                        naven.getNotificationManager().addNotification(notification);
                    }
                }
            } else if (this.enabled) {
                this.enabled = false;
                naven.getEventManager().unregister(this);
                this.onDisable();
                if (!(this instanceof ClickGUIModule)) {
                    HUD module = Naven.getInstance().getModuleManager().getModule(HUD.class);
                    if (module.moduleToggleSound.getCurrentValue()) {
                        mc.player.playSound(SoundEvents.WOODEN_BUTTON_CLICK_OFF, 0.5F, 0.8F);
                    }

                    Notification notification = new Notification(NotificationLevel.ERROR, this.name + " Disabled!", 3000L);
                    naven.getNotificationManager().addNotification(notification);
                }
            }
        } catch (Exception var5) {
        }
    }

    public int getMinPermission() {
        return this.minPermission;
    }

    public void setMinPermission(int minPermission) {
        this.minPermission = minPermission;
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
    }
}
