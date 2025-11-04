package cnm.obsoverlay.events.impl;

import cnm.obsoverlay.events.api.events.callables.EventCancellable;

public class EventKey extends EventCancellable {
   private final int key;
   private final boolean state;

   public int getKey() {
      return this.key;
   }

   public boolean isState() {
      return this.state;
   }

   public EventKey(int key, boolean state) {
      this.key = key;
      this.state = state;
   }
}
