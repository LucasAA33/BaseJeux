package basejeux.gameplay;

import basejeux.model.GameMap;
import basejeux.model.GameObjectDefinition;
import basejeux.model.Player;
import basejeux.model.AnimationDef;
import basejeux.model.event.Event;
import basejeux.model.event.EventType;

import java.util.Map;

public class InteractionSystem {

    private final Map<String, GameObjectDefinition> objectDefs;

    public InteractionSystem(Map<String, GameObjectDefinition> objectDefs) {
        this.objectDefs = objectDefs;
    }

    // =========================
    // === INTERACTION MAIN ====
    // =========================

    public void tryInteract(GameMap map, Player player) {

        // 🔹 Étape 1 : trouver l’objet devant le joueur
        GameMap.MapObject target = findObjectInFront(map, player);
        if (target == null) return;

        // 🔹 Étape 2 : récupérer l’event INTERACT
        Event interact = null;
        for (Event ev : target.events) {
            if (ev.type == EventType.INTERACT) {
                interact = ev;
                break;
            }
        }
        if (interact == null) return;
        // 🔹 Étape 3 : définition objet
        GameObjectDefinition def = objectDefs.get(target.id);
        if (def == null) return;

        // 🔹 Étape 4 : condition item
        if (interact.requireItem != null &&
            !player.hasItem(interact.requireItem)) {
            return;
        }

        // 🔹 Étape 5 : jouer animation
        AnimationDef anim = def.animations.list.get(interact.animation);
        if (anim == null) return;

        AnimationSystem.play(target, anim, () -> {
            // Pour l’instant : rien après animation
            // (état / téléport viendront ensuite)
        });
    }

    // =========================
    // === UTILITAIRE ========
    // =========================

    private GameMap.MapObject findObjectInFront(GameMap map, Player player) {
        int[] front = player.getFrontTile();
        int fx = front[0];
        int fy = front[1];

        for (GameMap.MapObject obj : map.objects) {
            if (obj.x == fx && obj.y == fy) {
                return obj;
            }
        }
        return null;
    }
}
