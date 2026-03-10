package basejeux.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import basejeux.model.event.EventType;


public class GameObjectDefinition {

    public String id;
    public String category;
    public String name;
    public String sprite;
    public AttackDefinition attack;
    public Map<String, String> stateSprites;
    
    public String type; // "enemy", "npc", "resource", etc.

    // Données gameplay
    public int hp;
    public int hardness;
    public List<String> required_tools;
    public List<Drop> drops;

    // Animations multiples
    public Animations animations;
    
    // Types d’événements autorisés pour cet objet
    public List<EventType> allowedEvents;


    public Sounds sounds;

    // Visuel / collisions
    public Size footprint;
    public Size spriteSize;
    public String anchor;
    public Offset offset;
    public Boolean collidable;

    // ---- Sous classes ----
    public static class Size {
        public int w;
        public int h;
    }

    public static class Offset {
        public int x;
        public int y;
    }

    // ====== NOUVEAU SYSTEME MULTI-ANIMATIONS ======
    public static class Animations {
        public Map<String, AnimationDef> list;
    }


    public static class Sounds {
        public String hit;
        public String destroy;
    }
}
