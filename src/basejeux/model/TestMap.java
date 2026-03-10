package basejeux.model;

import basejeux.data.DataLoader;

public class TestMap {
    public static void main(String[] args) {
        GameMap map = DataLoader.loadMap("assets/data/maps/map_village.json");

        if (map != null) {
            System.out.println("Carte : " + map.name);
            System.out.println("Taille : " + map.width + "x" + map.height);

            // Afficher les terrains
            System.out.println("\n--- Grille des terrains ---");
            for (int y = 0; y < map.height; y++) {
                for (int x = 0; x < map.width; x++) {
                    System.out.print(map.tiles[y][x] + "\t");
                }
                System.out.println();
            }

            // Afficher les objets placés
            System.out.println("\n--- Objets placés ---");
            for (GameMap.MapObject obj : map.objects) {
                System.out.println(" → " + obj.id + " en (" + obj.x + "," + obj.y + ")");
            }
        }
    }
}
