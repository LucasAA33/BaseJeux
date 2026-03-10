package basejeux.data;

import basejeux.model.FamilyWrapper;
import basejeux.model.GameMap;
import basejeux.model.GameObjectDefinition;
import basejeux.model.TerrainDefinition;
import basejeux.model.WeaponDefinition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chargeur / sauvegardeur JSON centralisé.
 */
public class DataLoader {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // -------------------- TERRAIN --------------------

    /** Charge le dictionnaire de terrains depuis un fichier (terrain.json). */
    public static Map<String, TerrainDefinition> loadTerrain(String filePath) {
        try (Reader r = new FileReader(filePath)) {
            Type t = new TypeToken<Map<String, TerrainDefinition>>(){}.getType();
            return GSON.fromJson(r, t);
        } catch (Exception e) {
            System.err.println("loadTerrain ERROR: " + e.getMessage());
            return new HashMap<>();
        }
    }
    public static Map<String, WeaponDefinition> loadWeaponFolderPath(String folderPath) {

        Map<String, WeaponDefinition> result = new HashMap<>();

        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory())
            return result;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return result;

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {

                WeaponDefinition weapon =
                    GSON.fromJson(reader, WeaponDefinition.class);

                if (weapon != null && weapon.id != null) {
                    result.put(weapon.id, weapon);
                }

            } catch (Exception e) {
                System.out.println("[WEAPON LOAD ERROR] " + file.getName());
                e.printStackTrace();
            }
        }

        return result;
    }

    // -------------------- FAMILLES D’OBJETS --------------------

    /** Charge la famille (un seul fichier JSON) et retourne map id -> def. */
    public static Map<String, GameObjectDefinition> loadFamilyFile(String filePath) {
        Map<String, GameObjectDefinition> out = new HashMap<>();
        try (Reader r = new FileReader(filePath)) {
            FamilyWrapper wrapper = GSON.fromJson(r, FamilyWrapper.class);
            if (wrapper != null && wrapper.objects != null) {
                for (GameObjectDefinition def : wrapper.objects) {
                    if (def != null && def.id != null) {
                        out.put(def.id, def);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("loadFamilyFile ERROR (" + filePath + "): " + e.getMessage());
        }
        return out;
    }

    /**
     * **NOUVEAU** : charge toutes les familles présentes dans un dossier
     * (tous les .json), fusionne et renvoie id -> def.
     */
    public static Map<String, GameObjectDefinition> loadFamilyFolderPath(String folderPath) {
        Map<String, GameObjectDefinition> all = new HashMap<>();
        try {
            File folder = new File(folderPath);
            if (!folder.exists() || !folder.isDirectory()) return all;

            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            if (files == null) return all;

            for (File f : files) {
                all.putAll(loadFamilyFile(f.getAbsolutePath()));
            }
        } catch (Exception e) {
            System.err.println("loadFamilyFolderPath ERROR: " + e.getMessage());
        }
        return all;
    }

    // -------------------- MAP --------------------

    /** Charge une GameMap (format déjà en place chez toi). */
    public static GameMap loadMap(String filePath) {
        try (Reader r = new FileReader(filePath)) {
            return GSON.fromJson(r, GameMap.class);
        } catch (Exception e) {
            System.err.println("loadMap ERROR: " + e.getMessage());
            return null;
        }
    }

    /** **NOUVEAU** : sauvegarde une GameMap sur disque (utilisé par l’éditeur). */
    public static void saveMap(String filePath, GameMap map) {
        try (FileWriter fw = new FileWriter(filePath)) {
            GSON.toJson(map, fw);
            fw.flush();
            System.out.println("Map sauvegardée -> " + filePath);
        } catch (Exception e) {
            System.err.println("saveMap ERROR: " + e.getMessage());
        }
    }
}
