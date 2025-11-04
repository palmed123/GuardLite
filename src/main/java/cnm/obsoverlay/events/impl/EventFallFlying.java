package cnm.obsoverlay.events.impl;

import cnm.obsoverlay.events.api.events.Event;

public class EventFallFlying implements Event {
   private float pitch;

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public float getPitch() {
      return this.pitch;
   }

   public EventFallFlying(float pitch) {
      this.pitch = pitch;
   }
}
