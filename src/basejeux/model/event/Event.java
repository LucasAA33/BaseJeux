package basejeux.model.event;

import java.util.Map;

public class Event {

    public EventType type;
    
    
    public String animation;
    public String teleportTo;
    public Integer spawnX;
    public Integer spawnY;
    public Map<String, Object> setState;
    public String requireItem;
    public Event onAnimationEnd;
    
}
