package basejeux.gameplay;

import basejeux.model.GameMap;
import basejeux.model.Player;

public interface GameMode {

    /**
     * Appelé à chaque tick logique.
     */
    void update(double dt);

    /**
     * Quand un objet prend des dégâts.
     */
    void onObjectDamaged(GameMap.MapObject obj, int damage);
    
    void playerAttack();
}