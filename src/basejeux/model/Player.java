package basejeux.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Joueur (debug + interactions)
 * Coordonnées en tuiles (pas en pixels).
 */
public class Player {

    // ⚠️ champs EXISTANTS — on ne touche pas
    public int x;
    public int y;
    private int baseHp = 100;
    int currentHp = 100;
    private Map<String, Object> overrides = new HashMap<>();
    public double moveCooldownMs;
    public Map<String, Object> state = new HashMap<>();
    public double attackCooldownRemaining = 0;
    public double maxAttackCooldown = 0;
    public double attackDuration = 0;
    public boolean isAttacking = false;
    public String currentWeapon = "sword_light";
    
    
    // ✅ NOUVEAUX champs (interaction)
    private Direction direction = Direction.DOWN;
    private final Set<String> inventory = new HashSet<>();

    public Player(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.moveCooldownMs = 0;
    }
    
    public int getMaxHp() {
        if (overrides.containsKey("hp"))
            return ((Number) overrides.get("hp")).intValue();
        return baseHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void damage(int amount) {
        currentHp -= amount;
        if (currentHp < 0) currentHp = 0;
    }

    // =====================
    // === INTERACTIONS ====
    // =====================

    /** Direction actuelle du joueur */
    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /** Case devant le joueur (pour INTERACT) */
    public int[] getFrontTile() {
        return switch (direction) {
            case UP    -> new int[]{x, y - 1};
            case DOWN  -> new int[]{x, y + 1};
            case LEFT  -> new int[]{x - 1, y};
            case RIGHT -> new int[]{x + 1, y};
        };
    }

    /** Téléport / déplacement forcé */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // =====================
    // === INVENTAIRE =====
    // =====================

    public boolean hasItem(String itemId) {
        return inventory.contains(itemId);
    }

    public void addItem(String itemId) {
        inventory.add(itemId);
    }
}
