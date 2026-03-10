package basejeux.model;

import basejeux.data.DataLoader;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Gère la couleur d'affichage des tuiles.
 * Les couleurs sont déduites des infos du terrain.json.
 */
public class TileColorMan {  // <== ICI: le nom de la classe doit matcher le nom du fichier

    private final Map<String, Color> tileColors = new HashMap<>();
    private final Map<String, TerrainDefinition> terrainDefs;

    public TileColorMan() {
        // Charge toutes les définitions de terrain depuis le JSON
        terrainDefs = DataLoader.loadTerrain("assets/data/terrain/terrain.json");

        if (terrainDefs != null) {
            for (Map.Entry<String, TerrainDefinition> entry : terrainDefs.entrySet()) {
                TerrainDefinition def = entry.getValue();
                Color color = chooseColorFor(def);
                tileColors.put(def.id, color);
            }
        }
    }

    /**
     * Retourne la couleur associée à une tuile "grass", "water", etc.
     */
    public Color getColor(String tileType) {
        return tileColors.getOrDefault(tileType, Color.GRAY);
    }

    /**
     * Logique pour choisir une couleur "lisible" selon le type de terrain.
     */
    private Color chooseColorFor(TerrainDefinition def) {
        String id = def.id.toLowerCase();

        // Zone dangereuse = damagePerSecond > 0 (lave etc.)
        if (def.damagePerSecond > 0) {
            return new Color(255, 60, 0); // orange/rouge vif
        }

        if (id.contains("grass")) return new Color(0, 160, 0);            // vert
        if (id.contains("dirt") || id.contains("mud")) return new Color(150, 90, 40); // marron
        if (id.contains("sand")) return new Color(230, 230, 120);         // sable
        if (id.contains("water")) return new Color(0, 180, 255);          // bleu clair
        if (id.contains("lava")) return new Color(255, 80, 0);            // rouge/orange lave

        // Si ce n'est pas marchable ou c'est collidable (rocher/eau bloquante)
        if (!def.walkable || def.collidable) {
            return new Color(120, 120, 120); // gris bloquant
        }

        // fallback neutre
        return Color.GRAY;
    }

    /**
     * Optionnel: accéder à la définition complète du terrain.
     */
    public TerrainDefinition getTerrainDef(String tileType) {
        return terrainDefs.get(tileType);
    }
}
