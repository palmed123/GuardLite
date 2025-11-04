package cnm.obsoverlay.utils;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventMotion;
import cnm.obsoverlay.events.impl.EventRespawn;
import cnm.obsoverlay.ui.notification.Notification;
import cnm.obsoverlay.ui.notification.NotificationLevel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.antlr.v4.runtime.misc.OrderedHashSet;

public class EntityWatcher implements Wrapper {
   private static final Map<Entity, Set<String>> tags = new ConcurrentHashMap<>();
   private static final Map<String, SharedESPData> sharedESPData = new ConcurrentHashMap<>();

   public static Set<String> getEntityTags(AbstractClientPlayer player) {
//      PotionResolver.resolve((Integer)player.getEntityData().get(LivingEntityAccessor.getEffectColorId()));
      List<MobEffect> effects = (List<MobEffect>) ReflectUtil.getFieldValue(LivingEntity.class, "DATA_EFFECT_COLOR_ID", null);
      effects.remove(MobEffects.ABSORPTION);
      Set<String> currentPlayerTags = new OrderedHashSet();
      if (tags.containsKey(player)) {
         currentPlayerTags.addAll(tags.get(player));
      }

      Set<String> collect = effects.stream()
         .map(effect -> "effect.minecraft." + BuiltInRegistries.MOB_EFFECT.getKey(effect).getPath())
         .collect(Collectors.toSet());
      currentPlayerTags.addAll(collect);
      return currentPlayerTags;
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      tags.clear();
      sharedESPData.clear();
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE && mc.level != null) {
         getSharedESPData().forEach((ign, data) -> {
            if (System.currentTimeMillis() - data.getUpdateTime() > 500L) {
               getSharedESPData().remove(ign);
            }
         });

         for (AbstractClientPlayer player : new ArrayList<AbstractClientPlayer>(mc.level.players())) {
            if (player != mc.player) {
               if (!tags.containsKey(player)) {
                  tags.put(player, new HashSet<>());
               }

               Set<String> playerTags = tags.get(player);
               if ((InventoryUtils.isGodAxe(player.getMainHandItem()) || InventoryUtils.isGodAxe(player.getOffhandItem())) && !playerTags.contains("God Axe")) {
                  Notification notification = new Notification(NotificationLevel.WARNING, player.getName().getString() + " is holding god axe!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("God Axe");
               }

               if ((InventoryUtils.isEnchantedGApple(player.getMainHandItem()) || InventoryUtils.isEnchantedGApple(player.getOffhandItem()))
                  && !playerTags.contains("Enchanted Golden Apple")) {
                  Notification notification = new Notification(
                     NotificationLevel.WARNING, player.getName().getString() + " is holding enchanted golden apple!", 3000L
                  );
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("Enchanted Golden Apple");
               }

               if ((InventoryUtils.isEndCrystal(player.getMainHandItem()) || InventoryUtils.isEndCrystal(player.getOffhandItem()))
                  && !playerTags.contains("End Crystal")) {
                  Notification notification = new Notification(NotificationLevel.WARNING, player.getName().getString() + " is holding end crystal!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("End Crystal");
               }

               if ((InventoryUtils.isKBBall(player.getMainHandItem()) || InventoryUtils.isKBBall(player.getOffhandItem())) && !playerTags.contains("KB Ball")) {
                  Notification notification = new Notification(NotificationLevel.WARNING, player.getName().getString() + " is holding KB Ball!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("KB Ball");
               }

               if ((InventoryUtils.isKBStick(player.getMainHandItem()) || InventoryUtils.isKBStick(player.getOffhandItem()))
                  && !playerTags.contains("KB Stick")) {
                  Notification notification = new Notification(NotificationLevel.WARNING, player.getName().getString() + " is holding KB Stick!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("KB Stick");
               }

               if ((InventoryUtils.getPunchLevel(player.getMainHandItem()) > 2 || InventoryUtils.getPunchLevel(player.getOffhandItem()) > 2)
                  && !playerTags.contains("Punch Bow")) {
                  Notification notification = new Notification(NotificationLevel.WARNING, player.getName().getString() + " is holding Punch Bow!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("Punch Bow");
               }

               if ((InventoryUtils.getPowerLevel(player.getMainHandItem()) > 3 || InventoryUtils.getPowerLevel(player.getOffhandItem()) > 3)
                  && !playerTags.contains("Power Bow")) {
                  Notification notification = new Notification(NotificationLevel.WARNING, player.getName().getString() + " is holding Power Bow!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(notification);
                  playerTags.add("Power Bow");
               }
            }
         }
      }
   }

   public static Map<String, SharedESPData> getSharedESPData() {
      return sharedESPData;
   }
}
