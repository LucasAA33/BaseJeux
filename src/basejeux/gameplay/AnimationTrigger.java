package basejeux.gameplay;

import basejeux.model.GameMap;
import basejeux.model.GameObjectDefinition;

public interface AnimationTrigger {
    void play(GameMap.MapObject obj,
              GameObjectDefinition def,
              String animationName);
}