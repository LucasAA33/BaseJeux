package basejeux.model;

import basejeux.data.DataLoader;
import java.util.Map;

public class TestJson {
    public static void main(String[] args) {
        // Charger un fichier famille unique (ex: tree.json)
        Map<String, GameObjectDefinition> trees =
                DataLoader.loadFamilyFile("assets/data/environment/tree.json");

        GameObjectDefinition appleTree = trees.get("tree_apple");
        if (appleTree != null) {
            System.out.println("Le " + appleTree.name + " a " + appleTree.hp + " PV.");
        } else {
            System.out.println("tree_apple introuvable.");
        }
    }
}
