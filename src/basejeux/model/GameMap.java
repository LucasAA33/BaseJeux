package basejeux.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import basejeux.model.event.Event;

public class GameMap {

    public int width;
    public int height;
    public String[][] tiles;
    public String name;
    public List<MapObject> objects;

    public GameMap() {
        objects = new ArrayList<>();
    }

    public static class MapObject {
        public String id;
        public int x;
        public int y;
        public int currentHp;
        public double attackCooldownRemaining = 0;
        public double hpBarTimer = 0;

        // ✅ ÉTAT PERSISTANT UNIQUE
        public Map<String, Object> state = new HashMap<>();

        // ✅ ÉVÉNEMENTS
        public List<Event> events = new ArrayList<>();
        
        public Map<String, Object> overrides = new HashMap<>();

        public MapObject() {} // IMPORTANT pour Gson

        public MapObject(String id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        public String getInstanceKey() {
            return id + "@" + x + "," + y;
        }
    }
}
