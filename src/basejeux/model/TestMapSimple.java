package basejeux.model;

import basejeux.data.DataLoader;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TestMapSimple extends JFrame {

    private GameMap map;
    private Map<String, Image> tileImages = new HashMap<>();
    private Map<String, Image> objectImages = new HashMap<>();
    private final int TILE_SIZE = 64; // taille d'une tuile en pixels

    public TestMapSimple(GameMap map) {
        this.map = map;

        // Charger les images des tuiles
        loadTileImages();

        // Charger les images des objets depuis le JSON
        loadObjectImages();

        setTitle("Map Graphique");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        MapPanel mapPanel = new MapPanel();
        add(mapPanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadTileImages() {
        String[] tileTypes = {"grass", "dirt", "water", "sand"}; // ajouter d'autres types si nécessaire
        for (String type : tileTypes) {
            Image img = new ImageIcon("assets/images/" + type + ".png").getImage();
            tileImages.put(type, img);
        }
    }

    private void loadObjectImages() {
        for (GameMap.MapObject obj : map.objects) {
            if (!objectImages.containsKey(obj.id)) {
                Image img = new ImageIcon("assets/images/" + obj.id + ".png").getImage();
                objectImages.put(obj.id, img);
            }
        }
    }

    private class MapPanel extends JPanel {
        public MapPanel() {
            setPreferredSize(new Dimension(map.width * TILE_SIZE, map.height * TILE_SIZE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Dessiner les tuiles
            for (int y = 0; y < map.height; y++) {
                for (int x = 0; x < map.width; x++) {
                    String tileType = map.tiles[y][x];
                    Image img = tileImages.get(tileType);
                    if (img != null) {
                        g.drawImage(img, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE, this);
                    } else {
                        // fallback en couleur si l'image n'existe pas
                        g.setColor(Color.GRAY);
                        g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }

            // Dessiner les objets
            for (GameMap.MapObject obj : map.objects) {
                Image img = objectImages.get(obj.id);
                if (img != null) {
                    g.drawImage(img, obj.x * TILE_SIZE, obj.y * TILE_SIZE, TILE_SIZE, TILE_SIZE, this);
                } else {
                    // fallback en rouge si image manquante
                    g.setColor(Color.RED);
                    g.fillOval(obj.x * TILE_SIZE + TILE_SIZE/4, obj.y * TILE_SIZE + TILE_SIZE/4,
                               TILE_SIZE/2, TILE_SIZE/2);
                }
            }
        }
    }

    public static void main(String[] args) {
        // Charger la map depuis le JSON
        GameMap map = DataLoader.loadMap("assets/data/maps/map_village.json");

        if (map != null) {
            SwingUtilities.invokeLater(() -> new TestMapSimple(map));
        } else {
            System.out.println("Erreur : impossible de charger la map.");
        }
    }
}
