package basejeux.gameplay;

import basejeux.model.AnimationDef;
import basejeux.model.GameMap;

public class AnimationSystem {

    public static void play(
            GameMap.MapObject obj,
            AnimationDef anim,
            Runnable onEnd
    ) {
        // TODO: vraie animation plus tard
        if (onEnd != null) {
            onEnd.run(); // exécution immédiate
        }
    }
}
