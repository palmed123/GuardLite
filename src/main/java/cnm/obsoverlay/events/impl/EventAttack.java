package cnm.obsoverlay.events.impl;

import cnm.obsoverlay.events.api.events.callables.EventCancellable;
import lombok.Getter;
import net.minecraft.world.entity.Entity;


@Getter
public class EventAttack extends EventCancellable {
    private final Entity target;
    private final Type type;

    public EventAttack(Entity target, Type type) {
        this.target = target;
        this.type = type;
    }

    public enum Type {
        Pre,
        Post
    }
}
