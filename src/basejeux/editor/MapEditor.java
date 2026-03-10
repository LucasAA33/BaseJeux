package basejeux.editor;

import basejeux.data.DataLoader;
import basejeux.editor.dialog.InteractEventDialog;
import basejeux.model.GameMap;
import basejeux.model.GameMap.MapObject;
import basejeux.model.GameObjectDefinition;
import basejeux.model.TerrainDefinition;
import basejeux.model.event.Event;
import basejeux.model.event.EventType;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;


/**
 * Éditeur de maps :
 * - Peinture de tuiles (point, rectangle, cercle, pot de peinture)
 * - Placement / suppression d'objets (drag)
 * - Resize de la map
 * - Load / Save
 * - Preview dans TileManager
 */
public class MapEditor extends JFrame {

    // ======== DATA ========
    private GameMap editingMap;
    private String currentFilePath = "assets/data/maps/map_created.json";
    
    private JPanel statePanel;
    private GameMap.MapObject selectedObject;

    private final Set<GameMap.MapObject> selectedObjects = new HashSet<>();


    // Modes / brush
    private enum BrushMode {
        TILE,
        OBJECT,
        DELETE
    }

    private BrushMode currentMode = BrushMode.TILE;
    private String currentTileBrush = "grass";
    private String currentObjectBrush = "tree_oak";

    // Modes de peinture de tuiles
    private enum TilePaintMode { POINT, RECT, CERCLE, POT }
    private TilePaintMode currentTilePaintMode = TilePaintMode.POINT;

    // Définitions
    private Map<String, TerrainDefinition> terrainDefs = new LinkedHashMap<>();
    private Map<String, GameObjectDefinition> objectDefs = new LinkedHashMap<>();

    // Rendu
    private static final int TILE_SIZE = 32;
    private final MapPanel mapPanel = new MapPanel();

    // Cache sprites objets
    private final Map<String, Image> objectSpriteCache = new HashMap<>();
    // Cache sprites terrain
    private final Map<String, Image> terrainSpriteCache = new HashMap<>();

    // Hover souris
    private int hoverX = -1, hoverY = -1;
    private boolean hoverInside = false;
    

    // Drag painting
    private boolean leftDragging = false;
    private boolean rightDragging = false;

    // Pour les pinceaux rect/cercle
    private int dragStartX = -1, dragStartY = -1;
    
    // Liste des boutons pour la recherche / filtrage
    private final java.util.List<JButton> tileButtons   = new ArrayList<>();
    private final java.util.List<JButton> objectButtons = new ArrayList<>();

    // Pour pouvoir revalider/redessiner le panneau de droite après filtrage
    private JPanel rightPanelRoot;

    
    //bare d'outils
    private int ICON_SIZE = 32;
    
    



    public MapEditor() {
        setTitle("Map Editor (V1 stable)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Charger définitions
        terrainDefs = DataLoader.loadTerrain("assets/data/terrain/terrain.json");
        objectDefs.putAll(DataLoader.loadFamilyFolderPath("assets/data/environment"));
        try {
            objectDefs.putAll(DataLoader.loadFamilyFolderPath("assets/data/items"));
        } catch (Exception ignored) {}

        // Précharger sprites
        preloadTerrainSprites();
        preloadObjectSprites();

        // Map par défaut
        createEmptyMap(40, 30);

        // UI
        setJMenuBar(buildMenuBar());
        add(buildToolBar(), BorderLayout.NORTH);         // <--- NOUVELLE TOOLBAR
        add(buildRightPanel(), BorderLayout.EAST);
        add(new JScrollPane(mapPanel), BorderLayout.CENTER);

        mapPanel.setPreferredSize(new Dimension(editingMap.width * TILE_SIZE,
                                                editingMap.height * TILE_SIZE));

        // Raccourcis clavier : Espace = basculer mode / P = preview
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE -> toggleMode();
                    case KeyEvent.VK_P     -> openPreviewInTileManager();
                }
            }
        });

        setFocusable(true);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ========= SPRITES OBJETS / TERRAINS =========
    private void preloadObjectSprites() {
        for (GameObjectDefinition def : objectDefs.values()) {
            if (def.sprite != null && !objectSpriteCache.containsKey(def.id)) {
                objectSpriteCache.put(def.id, tryLoadImage(def.sprite));
            }
        }
    }

    private void preloadTerrainSprites() {
        for (TerrainDefinition def : terrainDefs.values()) {
            if (def.image != null && !terrainSpriteCache.containsKey(def.id)) {
                terrainSpriteCache.put(def.id, tryLoadImage(def.image));
            }
        }
    }

    private Image tryLoadImage(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                System.out.println("[EDITOR SPRITE] " + path + " -> existe=false");
                return null;
            }
            System.out.println("[EDITOR SPRITE] " + path + " -> existe=true");
            return new ImageIcon(path).getImage();
        } catch (Exception e) {
            System.out.println("[EDITOR SPRITE] ERREUR " + path + " : " + e.getMessage());
            return null;
        }
    }

    private Image getObjectImage(String id) {
        Image img = objectSpriteCache.get(id);
        if (img != null) return img;
        GameObjectDefinition def = objectDefs.get(id);
        if (def != null && def.sprite != null) {
            img = tryLoadImage(def.sprite);
            if (img != null) objectSpriteCache.put(def.id, img);
        }
        return img;
    }

    private Image getTerrainImage(String id) {
        Image img = terrainSpriteCache.get(id);
        if (img != null) return img;
        TerrainDefinition def = terrainDefs.get(id);
        if (def != null && def.image != null) {
            img = tryLoadImage(def.image);
            if (img != null) terrainSpriteCache.put(def.id, img);
        }
        return img;
    }
    
    private Object parseValue(String txt) {
        if ("true".equalsIgnoreCase(txt)) return true;
        if ("false".equalsIgnoreCase(txt)) return false;

        try { return Integer.parseInt(txt); }
        catch (Exception ignored) {}

        try { return Double.parseDouble(txt); }
        catch (Exception ignored) {}

        return txt;
    }


    private void addNewState() {
        if (selectedObject == null) return;

        String key = JOptionPane.showInputDialog("Nom du state");
        if (key == null || key.isBlank()) return;

        // Mise à jour UNIQUEMENT du state
        selectedObject.state.put(key, true);

        
        refreshStateEditor();
    }


    
    private void refreshStateEditor() {
        statePanel.removeAll();

        if (selectedObject == null) {
            statePanel.add(new JLabel("Aucun objet sélectionné"));
            statePanel.revalidate();
            statePanel.repaint();
            return;
        }
        if (selectedObjects.size() > 1) {
            statePanel.add(new JLabel(
                selectedObjects.size() + " objets sélectionnés"
            ));
            return;
        }

        JLabel title = new JLabel(
            "Objet : " + selectedObject.id +
            " @ " + selectedObject.x + "," + selectedObject.y
        );
        statePanel.add(title);

        for (Map.Entry<String, Object> entry : selectedObject.state.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            row.add(new JLabel(key));

            JTextField field = new JTextField(value.toString(), 8);

            field.addActionListener(ev -> {
                Object parsed = parseValue(field.getText());
                selectedObject.state.put(key, parsed);
            });

            row.add(field);
            statePanel.add(row);
        }

        JButton add = new JButton("+ Ajouter state");
        add.addActionListener(e -> addNewState());
        statePanel.add(add);

        statePanel.revalidate();
        statePanel.repaint();
    }


    
    private JPanel buildStateEditorPanel() {
        statePanel = new JPanel();
        statePanel.setLayout(new BoxLayout(statePanel, BoxLayout.Y_AXIS));
        statePanel.setBorder(BorderFactory.createTitledBorder("States"));

        refreshStateEditor();
        return statePanel;
    }


 // ========= TOOLBAR (icônes pinceaux) =========
    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        // --- OUTILS DE PEINTURE ---
        JButton btPoint = createToolButton(
                "assets/ui/brush_point.png",
                "Pinceau point (clic & drag)",
                () -> currentTilePaintMode = TilePaintMode.POINT
        );
        JButton btRect = createToolButton(
                "assets/ui/brush_rect.png",
                "Remplir rectangle",
                () -> currentTilePaintMode = TilePaintMode.RECT
        );
        JButton btCircle = createToolButton(
                "assets/ui/brush_circle.png",
                "Remplir cercle",
                () -> currentTilePaintMode = TilePaintMode.CERCLE
        );
        JButton btPot = createToolButton(
                "assets/ui/brush_fill.png",
                "Pot de peinture (flood fill)",
                () -> currentTilePaintMode = TilePaintMode.POT
        );

        bar.add(new JLabel(" Tile tools: "));
        bar.add(btPoint);
        bar.add(btRect);
        bar.add(btCircle);
        bar.add(btPot);

        bar.addSeparator();

        // --- MODES D'ÉDITION ---
        JButton btModeTile = createToolButton(
                "assets/ui/mode_tile.png",
                "Mode TILES",
                () -> currentMode = BrushMode.TILE
        );
        JButton btModeObj = createToolButton(
                "assets/ui/mode_object.png",
                "Mode OBJECTS",
                () -> currentMode = BrushMode.OBJECT
        );
        JButton btModeDelete = createToolButton(
                "assets/ui/bin.png",
                "Mode DELETE (supprimer tuiles / objets)",
                () -> currentMode = BrushMode.DELETE
        );

        bar.add(new JLabel(" Mode: "));
        bar.add(btModeTile);
        bar.add(btModeObj);
        bar.add(btModeDelete);

        return bar;
    }

    private JButton createTileButton(String id) {
        JButton b = new JButton(id);
        b.setHorizontalAlignment(SwingConstants.LEFT);

        Image img = getTerrainImage(id); // utilise déjà ton cache terrain
        if (img != null) {
            Image scaled = scaleNearestNeighbor(img, 24, 24);
            b.setIcon(new ImageIcon(scaled));
        }

        b.addActionListener(e -> currentTileBrush = id);
        tileButtons.add(b);
        return b;
    }

    private JButton createObjectButton(String id) {
        JButton b = new JButton(id);
        b.setHorizontalAlignment(SwingConstants.LEFT);

        Image img = getObjectImage(id); // utilise déjà ton cache objets
        if (img != null) {
            Image scaled = scaleNearestNeighbor(img, 24, 24);
            b.setIcon(new ImageIcon(scaled));
        }

        b.addActionListener(e -> currentObjectBrush = id);
        objectButtons.add(b);
        return b;
    }
    private JPanel createCollapsibleSection(String title, JPanel body) {
        JPanel container = new JPanel(new BorderLayout());
        JToggleButton header = new JToggleButton(title, true);
        header.setFocusPainted(false);
        header.setHorizontalAlignment(SwingConstants.LEFT);

        header.addActionListener(e -> {
            body.setVisible(header.isSelected());
            container.revalidate();
            container.repaint();
        });

        container.add(header, BorderLayout.NORTH);
        container.add(body, BorderLayout.CENTER);
        return container;
    }
    
    private void filterButtons(String query) {
        if (query == null) query = "";
        String q = query.toLowerCase();

        for (JButton b : tileButtons) {
            boolean show = b.getText().toLowerCase().contains(q);
            b.setVisible(show);
        }
        for (JButton b : objectButtons) {
            boolean show = b.getText().toLowerCase().contains(q);
            b.setVisible(show);
        }

        if (rightPanelRoot != null) {
            rightPanelRoot.revalidate();
            rightPanelRoot.repaint();
        }
    }



    private Image scaleNearestNeighbor(Image src, int w, int h) {
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return scaled;
    }
    
    private GameMap.MapObject findObjectAt(int gx, int gy) {
        for (GameMap.MapObject o : editingMap.objects) {
            GameObjectDefinition d = objectDefs.get(o.id);

            int fw = (d != null && d.footprint != null) ? d.footprint.w : 1;
            int fh = (d != null && d.footprint != null) ? d.footprint.h : 1;

            if (gx >= o.x && gx < o.x + fw &&
                gy >= o.y && gy < o.y + fh) {
                return o;
            }
        }
        return null;
    }


    private JButton createToolButton(String iconPath, String tooltip, Runnable onClick) {
        JButton b;

        File f = new File(iconPath);
        if (f.exists()) {
            // Charger PNG original
            ImageIcon rawIcon = new ImageIcon(iconPath);

            // Redimensionner proprement en 32×32 (pixel art friendly)
            Image scaled = rawIcon.getImage().getScaledInstance(
                    32, 32,
                    Image.SCALE_DEFAULT   // ou SCALE_FAST / SCALE_SMOOTH / NEAREST_NEIGHBOR
            );

            // NEAREST-NEIGHBOR pour pixel art
            Image scaledNN = rawIcon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT);


            b = new JButton(new ImageIcon(scaledNN));
        } else {
            b = new JButton(tooltip); // fallback texte
        }

        b.setFocusable(false);
        b.setToolTipText(tooltip);
        b.addActionListener(e -> onClick.run());
        return b;
    }


    // ========= MENUS =========
    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem load    = new JMenuItem("Load...");
        JMenuItem save    = new JMenuItem("Save");
        JMenuItem saveAs  = new JMenuItem("Save As...");
        JMenuItem resize  = new JMenuItem("Resize...");
        JMenuItem preview = new JMenuItem("Preview (TileManager)");

        load.addActionListener(e -> {
            String path = askFilePath("Chemin de la map :");
            if (path != null) loadMapFromFile(path);
        });
        save.addActionListener(e -> saveMapToCurrentFile());
        saveAs.addActionListener(e -> {
            String path = askFilePath("Enregistrer sous :");
            if (path != null) {
                currentFilePath = path;
                saveMapToCurrentFile();
            }
        });
        resize.addActionListener(e -> resizeMapInteractive());
        preview.addActionListener(e -> openPreviewInTileManager());

        file.add(load);
        file.add(save);
        file.add(saveAs);
        file.addSeparator();
        file.add(resize);
        file.addSeparator();
        file.add(preview);

        bar.add(file);
        return bar;
    }

    private JPanel buildRightPanel() {
        rightPanelRoot = new JPanel(new BorderLayout());
        rightPanelRoot.setPreferredSize(new Dimension(260, 0));

        // ==== Barre de recherche en haut ====
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        JLabel searchLabel = new JLabel("Search:");
        JTextField searchField = new JTextField();
        JButton clearBtn = new JButton("X");
        clearBtn.setMargin(new Insets(2, 6, 2, 6));

        clearBtn.addActionListener(e -> {
            searchField.setText("");
            filterButtons("");
        });

        // Quand on tape, on filtre
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            private void changed() {
                filterButtons(searchField.getText().trim());
            }
        });

        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(clearBtn, BorderLayout.EAST);

        rightPanelRoot.add(searchPanel, BorderLayout.NORTH);

        // ==== Corps : sections TILES + OBJECTS ====
        JPanel sections = new JPanel();
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));

        // --- Section TILES ---
        JPanel tilesPanel = new JPanel();
        tilesPanel.setLayout(new BoxLayout(tilesPanel, BoxLayout.Y_AXIS));
        tilesPanel.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        for (String id : terrainDefs.keySet()) {
            tilesPanel.add(createTileButton(id));
        }

        sections.add(createCollapsibleSection("TILES", tilesPanel));
        sections.add(Box.createVerticalStrut(8));

        // --- Section OBJECTS ---
        JPanel objectsPanel = new JPanel();
        objectsPanel.setLayout(new BoxLayout(objectsPanel, BoxLayout.Y_AXIS));
        objectsPanel.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        for (String id : objectDefs.keySet()) {
            objectsPanel.add(createObjectButton(id));
        }

        sections.add(createCollapsibleSection("OBJECTS", objectsPanel));

        rightPanelRoot.add(new JScrollPane(sections), BorderLayout.CENTER);
        
        sections.add(Box.createVerticalStrut(8));
        sections.add(buildStateEditorPanel());
        JPanel tools = new JPanel();
        tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));

        return rightPanelRoot;
    }


    private String askFilePath(String msg) {
        return JOptionPane.showInputDialog(this, msg, currentFilePath);
    }

    // ========= MAP OPS =========
    private String getDefaultTileId() {
        // Priorité à "empty" si tu l'as défini, sinon "grass"
        if (terrainDefs.containsKey("empty")) return "empty";
        return "grass";
    }

    private void createEmptyMap(int w, int h) {
        editingMap = new GameMap();
        editingMap.width = w;
        editingMap.height = h;
        editingMap.tiles = new String[h][w];
        String def = getDefaultTileId();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                editingMap.tiles[y][x] = def;
        editingMap.objects = new java.util.ArrayList<>();
        mapPanel.revalidate();
        mapPanel.repaint();
    }

    private void resizeMap(int newW, int newH) {
        if (newW <= 0 || newH <= 0) return;

        GameMap newMap = new GameMap();
        newMap.width = newW;
        newMap.height = newH;
        newMap.tiles = new String[newH][newW];
        newMap.objects = new java.util.ArrayList<>();

        for (int y = 0; y < Math.min(newH, editingMap.height); y++) {
            for (int x = 0; x < Math.min(newW, editingMap.width); x++) {
                newMap.tiles[y][x] = editingMap.tiles[y][x];
            }
        }
        String def = getDefaultTileId();
        for (int y = 0; y < newH; y++) {
            for (int x = 0; x < newW; x++) {
                if (newMap.tiles[y][x] == null) {
                    newMap.tiles[y][x] = def;
                }
            }
        }

        for (GameMap.MapObject obj : editingMap.objects) {
            GameObjectDefinition d = objectDefs.get(obj.id);
            int fw = (d != null && d.footprint != null) ? d.footprint.w : 1;
            int fh = (d != null && d.footprint != null) ? d.footprint.h : 1;
            if (obj.x >= 0 && obj.y >= 0 &&
                    obj.x + fw <= newW &&
                    obj.y + fh <= newH) {
                newMap.objects.add(obj);
            }
        }

        editingMap = newMap;

        mapPanel.setPreferredSize(new Dimension(newW * TILE_SIZE, newH * TILE_SIZE));
        mapPanel.revalidate();
        mapPanel.repaint();
    }

    private void resizeMapInteractive() {
        String wStr = JOptionPane.showInputDialog(this, "Nouvelle largeur :", editingMap.width);
        if (wStr == null) return;
        String hStr = JOptionPane.showInputDialog(this, "Nouvelle hauteur :", editingMap.height);
        if (hStr == null) return;
        try {
            int w = Integer.parseInt(wStr.trim());
            int h = Integer.parseInt(hStr.trim());
            resizeMap(w, h);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valeurs invalides.");
        }
    }

    private void loadMapFromFile(String path) {
        GameMap m = DataLoader.loadMap(path);
        if (m != null) {
            currentFilePath = path;
            editingMap = m;

            mapPanel.setPreferredSize(new Dimension(editingMap.width * TILE_SIZE,
                                                    editingMap.height * TILE_SIZE));
            mapPanel.revalidate();
            mapPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this, "Impossible de charger " + path);
        }
    }

    private void saveMapToCurrentFile() {
        try {
            DataLoader.saveMap(currentFilePath, editingMap);
            JOptionPane.showMessageDialog(this, "Sauvegardé : " + currentFilePath);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur save : " + ex.getMessage());
        }
    }

    private void openPreviewInTileManager() {
        try {
            if (currentFilePath == null || currentFilePath.isBlank())
                currentFilePath = "assets/data/maps/map_created.json";

            DataLoader.saveMap(currentFilePath, editingMap);
            basejeux.model.TileManager.main(null);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Impossible d'ouvrir TileManager : " + ex.getMessage());
        }
    }

    private void toggleMode() {
        currentMode = (currentMode == BrushMode.TILE) ? BrushMode.OBJECT : BrushMode.TILE;
        mapPanel.repaint();
    }

    // ========= PLACEMENT D'OBJET =========
    private void placeCurrentObjectAtGrid(int gridX, int gridY) {
        if (currentObjectBrush == null || currentObjectBrush.isBlank()) return;

        GameObjectDefinition def = objectDefs.get(currentObjectBrush);
        int fw = (def != null && def.footprint != null) ? def.footprint.w : 1;
        int fh = (def != null && def.footprint != null) ? def.footprint.h : 1;
        String anchor = (def != null && def.anchor != null) ? def.anchor : "bottom";

        int ox = gridX;
        int oy = gridY;

        if ("bottom".equalsIgnoreCase(anchor)) {
            oy = gridY - (fh - 1);
        } else if ("center".equalsIgnoreCase(anchor)) {
            ox = gridX - (fw / 2);
            oy = gridY - (fh / 2);
        }

        ox = Math.max(0, Math.min(ox, editingMap.width  - fw));
        oy = Math.max(0, Math.min(oy, editingMap.height - fh));

        editingMap.objects.add(new GameMap.MapObject(currentObjectBrush, ox, oy));
        mapPanel.repaint();
    }
    private void showObjectContextMenu(MouseEvent e, GameMap.MapObject obj) {
        JPopupMenu menu = new JPopupMenu();

        // --- Éditer states ---
        JMenuItem editStates = new JMenuItem("Éditer states");
        editStates.addActionListener(a -> {
            selectedObject = obj;
            refreshStateEditor();
        });
        menu.add(editStates);

        menu.addSeparator();

        GameObjectDefinition def = objectDefs.get(obj.id);
        if (def == null) {
            return;
        }

        List<EventType> allowed = def.allowedEvents;

        // ✅ Vérification correcte
        if (allowed != null && allowed.contains(EventType.INTERACT)) {

            JMenuItem addInteract = new JMenuItem("➕ Ajouter événement → Interact");
            addInteract.addActionListener(a -> {

                if (obj.events == null) {
                    obj.events = new ArrayList<>();
                }

                Event ev = new Event();
                ev.type = EventType.INTERACT; // ← important

                InteractEventDialog dlg =
                        new InteractEventDialog(MapEditor.this, ev, def);
                dlg.setVisible(true);

                if (dlg.isValidated()) {
                    obj.events.add(ev);
                    repaint();
                }
            });

            menu.add(addInteract);
        }

        // --- Supprimer tous les événements ---
        JMenuItem clearEvents = new JMenuItem("🧹 Supprimer tous les événements");
        clearEvents.addActionListener(a -> {
            if (obj.events != null) {
                obj.events.clear();
                repaint();
            }
        });
        menu.add(clearEvents);

        menu.addSeparator();

        // --- Supprimer l'objet ---
        JMenuItem delete = new JMenuItem("Supprimer l'objet");
        delete.addActionListener(a -> {
            editingMap.objects.remove(obj);
            selectedObject = null;
            refreshStateEditor();
            repaint();
        });
        menu.add(delete);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }




    // ========= PANEL =========
    private class MapPanel extends JPanel {

        MapPanel() {

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    updateHover(e);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    updateHover(e);
                    if (!hoverInside) return;

                    if (currentMode == BrushMode.TILE &&
                        (currentTilePaintMode == TilePaintMode.RECT ||
                         currentTilePaintMode == TilePaintMode.CERCLE)) {

                        if (leftDragging || rightDragging) repaint();
                    } else {
                        if (leftDragging) paintAt(hoverX, hoverY, false);
                        else if (rightDragging) paintAt(hoverX, hoverY, true);
                    }
                }
            });

            addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                	maybeShowContextMenu(e);
                    if (!hoverInside) return;
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        leftDragging = true;
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        rightDragging = true;
                    }
                    int gx = hoverX;
                    int gy = hoverY;

                    if (currentMode == BrushMode.OBJECT) {
                        handleObjectMousePressed(e, gx, gy);
                        repaint();
                        return;
                    }

                    handleTileMousePressed(e, gx, gy);
                    repaint();
                }
                
                private void maybeShowContextMenu(MouseEvent e) {
                    if (currentMode != BrushMode.OBJECT) return;
                    if (!e.isPopupTrigger()) return;

                    int gx = hoverX;
                    int gy = hoverY;

                    GameMap.MapObject found = findObjectAt(gx, gy);
                    if (found != null) {
                        selectedObject = found;
                        refreshStateEditor();
                        showObjectContextMenu(e, found);
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!hoverInside) return;
                    leftDragging = false;
                    rightDragging = false;

                    int gx = hoverX;
                    int gy = hoverY;

                    // ✅ VALIDATION RECTANGLE / CERCLE
                    if (currentMode == BrushMode.TILE &&
                        (currentTilePaintMode == TilePaintMode.RECT ||
                         currentTilePaintMode == TilePaintMode.CERCLE) &&
                        dragStartX >= 0 && dragStartY >= 0) {

                        boolean erase = SwingUtilities.isRightMouseButton(e);

                        if (currentTilePaintMode == TilePaintMode.RECT) {
                            paintRectangle(dragStartX, dragStartY, gx, gy, erase);
                        } else {
                            paintCircle(dragStartX, dragStartY, gx, gy, erase);
                        }

                        dragStartX = -1;
                        dragStartY = -1;
                        maybeShowContextMenu(e);
                        repaint();
                        return;
                    }
                }
            });
        }

            
        private void handleObjectMousePressed(MouseEvent e, int gx, int gy) {

            GameMap.MapObject found = findObjectAt(gx, gy);

            if (SwingUtilities.isLeftMouseButton(e)) {

                boolean shift = e.isShiftDown();

                if (found != null) {

                    if (shift) {
                        if (selectedObjects.contains(found)) {
                            selectedObjects.remove(found);
                        } else {
                            selectedObjects.add(found);
                        }
                    } else {
                        // sélection simple
                        selectedObjects.clear();
                        selectedObjects.add(found);
                    }

                    selectedObject = found;
                    refreshStateEditor();
                    return;
                }

                // clic sur vide → reset sélection
                if (!shift) {
                    selectedObjects.clear();
                    selectedObject = null;
                    refreshStateEditor();
                }

                // placement si objet actif
                placeCurrentObjectAtGrid(gx, gy);
            }
        }

            private void handleTileMousePressed(MouseEvent e, int gx, int gy) {

                if (SwingUtilities.isLeftMouseButton(e)) {

                    if (currentTilePaintMode == TilePaintMode.POINT) {
                        paintAt(gx, gy, false);
                    } else if (currentTilePaintMode == TilePaintMode.RECT ||
                               currentTilePaintMode == TilePaintMode.CERCLE) {
                        dragStartX = gx;
                        dragStartY = gy;
                    } else if (currentTilePaintMode == TilePaintMode.POT) {
                        floodFill(gx, gy, false);
                    }

                } else if (SwingUtilities.isRightMouseButton(e)) {

                    if (currentTilePaintMode == TilePaintMode.POINT) {
                        paintAt(gx, gy, true);
                    } else if (currentTilePaintMode == TilePaintMode.RECT ||
                               currentTilePaintMode == TilePaintMode.CERCLE) {
                        dragStartX = gx;
                        dragStartY = gy;
                    } else if (currentTilePaintMode == TilePaintMode.POT) {
                        floodFill(gx, gy, true);
                    }
                }
            }
            
            private void deleteAt(int gx, int gy) {
                // 1️⃣ Suppression objet (prioritaire)
                Iterator<GameMap.MapObject> it = editingMap.objects.iterator();
                while (it.hasNext()) {
                    GameMap.MapObject o = it.next();
                    GameObjectDefinition d = objectDefs.get(o.id);
                    int fw = (d != null && d.footprint != null) ? d.footprint.w : 1;
                    int fh = (d != null && d.footprint != null) ? d.footprint.h : 1;

                    if (gx >= o.x && gx < o.x + fw &&
                        gy >= o.y && gy < o.y + fh) {
                        it.remove();
                        return; 
                    }
                }

                // 2️⃣ Suppression tuile (fallback)
                editingMap.tiles[gy][gx] = "grass";
            }

        private void updateHover(MouseEvent e) {
            int gx = e.getX() / TILE_SIZE;
            int gy = e.getY() / TILE_SIZE;
            hoverInside = (gx >= 0 && gy >= 0 && gx < editingMap.width && gy < editingMap.height);
            hoverX = gx;
            hoverY = gy;
            repaint();
        }

        private void paintAt(int gx, int gy, boolean erase) {
            if (!hoverInside) return;

            // 🗑 MODE DELETE : priorité absolue
            if (currentMode == BrushMode.DELETE) {
                deleteAt(gx, gy);
                return;
            }

            // 🎨 MODE TILE
            if (currentMode == BrushMode.TILE) {
                if (erase) {
                    editingMap.tiles[gy][gx] = getDefaultTileId();
                } else {
                    editingMap.tiles[gy][gx] = currentTileBrush;
                }
                return;
            }

            // 📦 MODE OBJECT
            if (currentMode == BrushMode.OBJECT) {
                if (!erase) {
                    placeCurrentObjectAtGrid(gx, gy);
                }
            }
        }


        private void paintRectangle(int x0, int y0, int x1, int y1, boolean erase) {
            int minX = Math.max(0, Math.min(x0, x1));
            int maxX = Math.min(editingMap.width - 1, Math.max(x0, x1));
            int minY = Math.max(0, Math.min(y0, y1));
            int maxY = Math.min(editingMap.height - 1, Math.max(y0, y1));

            String id = erase ? getDefaultTileId() : currentTileBrush;

            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    editingMap.tiles[y][x] = id;
                }
            }
        }

        private void paintCircle(int x0, int y0, int x1, int y1, boolean erase) {
            int dx = x1 - x0;
            int dy = y1 - y0;
            int r = Math.max(Math.abs(dx), Math.abs(dy));

            String id = erase ? getDefaultTileId() : currentTileBrush;

            for (int y = y0 - r; y <= y0 + r; y++) {
                for (int x = x0 - r; x <= x0 + r; x++) {
                    if (x < 0 || y < 0 || x >= editingMap.width || y >= editingMap.height) continue;
                    int ddx = x - x0;
                    int ddy = y - y0;
                    if (ddx * ddx + ddy * ddy <= r * r) {
                        editingMap.tiles[y][x] = id;
                    }
                }
            }
        }

        private void floodFill(int gx, int gy, boolean erase) {
            if (gx < 0 || gy < 0 || gx >= editingMap.width || gy >= editingMap.height) return;

            String target = editingMap.tiles[gy][gx];
            String replacement = erase ? getDefaultTileId() : currentTileBrush;

            if (target == null || target.equals(replacement)) return;

            ArrayDeque<Point> stack = new ArrayDeque<>();
            stack.push(new Point(gx, gy));

            while (!stack.isEmpty()) {
                Point p = stack.pop();
                int x = p.x;
                int y = p.y;
                if (x < 0 || y < 0 || x >= editingMap.width || y >= editingMap.height) continue;
                if (!Objects.equals(editingMap.tiles[y][x], target)) continue;

                editingMap.tiles[y][x] = replacement;

                stack.push(new Point(x + 1, y));
                stack.push(new Point(x - 1, y));
                stack.push(new Point(x, y + 1));
                stack.push(new Point(x, y - 1));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // grille + tiles
            for (int y = 0; y < editingMap.height; y++) {
                for (int x = 0; x < editingMap.width; x++) {
                    int px = x * TILE_SIZE;
                    int py = y * TILE_SIZE;

                    String tileId = editingMap.tiles[y][x];

                    Image tileImg = getTerrainImage(tileId);
                    if (tileImg != null) {
                        g2.drawImage(tileImg, px, py, TILE_SIZE, TILE_SIZE, this);
                    } else {
                        g2.setColor(colorForTile(tileId));
                        g2.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                    }

                    g2.setColor(Color.BLACK);
                    g2.drawRect(px, py, TILE_SIZE, TILE_SIZE);
                }
            }

            // prévisualisation rect/cercle
            if (currentMode == BrushMode.TILE &&
                (currentTilePaintMode == TilePaintMode.RECT ||
                 currentTilePaintMode == TilePaintMode.CERCLE) &&
                dragStartX >= 0 && dragStartY >= 0 && hoverInside &&
                (leftDragging || rightDragging)) {

                g2.setColor(new Color(255, 255, 255, 80));
                if (currentTilePaintMode == TilePaintMode.RECT) {
                    int minX = Math.min(dragStartX, hoverX);
                    int maxX = Math.max(dragStartX, hoverX);
                    int minY = Math.min(dragStartY, hoverY);
                    int maxY = Math.max(dragStartY, hoverY);
                    int px = minX * TILE_SIZE;
                    int py = minY * TILE_SIZE;
                    int w  = (maxX - minX + 1) * TILE_SIZE;
                    int h  = (maxY - minY + 1) * TILE_SIZE;
                    g2.fillRect(px, py, w, h);
                } else {
                    int dx = hoverX - dragStartX;
                    int dy = hoverY - dragStartY;
                    int r = Math.max(Math.abs(dx), Math.abs(dy));
                    int px = (dragStartX - r) * TILE_SIZE;
                    int py = (dragStartY - r) * TILE_SIZE;
                    int d  = (2 * r + 1) * TILE_SIZE;
                    g2.fillOval(px, py, d, d);
                }
            }

            // objets : sprite + footprint
            for (GameMap.MapObject obj : editingMap.objects) {
                GameObjectDefinition def = objectDefs.get(obj.id);

                int fw = (def != null && def.footprint != null) ? def.footprint.w : 1;
                int fh = (def != null && def.footprint != null) ? def.footprint.h : 1;

                int baseX = obj.x * TILE_SIZE;
                int baseY = obj.y * TILE_SIZE;

                int drawW = fw * TILE_SIZE;
                int drawH = fh * TILE_SIZE;
                if (def != null && def.spriteSize != null) {
                    drawW = def.spriteSize.w;
                    drawH = def.spriteSize.h;
                }

                int offX = (def != null && def.offset != null) ? def.offset.x : 0;
                int offY = (def != null && def.offset != null) ? def.offset.y : 0;
                String anchor = (def != null && def.anchor != null) ? def.anchor : "bottom";

                int drawX = baseX + offX;
                int drawY = baseY + offY;

                if ("bottom".equalsIgnoreCase(anchor)) {
                    drawY = baseY + (fh * TILE_SIZE) - drawH + offY;
                } else if ("center".equalsIgnoreCase(anchor)) {
                    drawX = baseX + (fw * TILE_SIZE - drawW) / 2 + offX;
                    drawY = baseY + (fh * TILE_SIZE - drawH) / 2 + offY;
                }

                Image sprite = getObjectImage(obj.id);
                if (sprite != null) {
                    g2.drawImage(sprite, drawX, drawY, drawW, drawH, this);
                } else {
                    g2.setColor(Color.RED);
                    g2.fillOval(baseX + TILE_SIZE / 4, baseY + TILE_SIZE / 4,
                               TILE_SIZE / 2, TILE_SIZE / 2);
                    g2.setColor(Color.BLACK);
                    g2.drawOval(baseX + TILE_SIZE / 4, baseY + TILE_SIZE / 4,
                               TILE_SIZE / 2, TILE_SIZE / 2);
                }

                g2.setColor(new Color(0, 150, 0, 80));
                g2.fillRect(baseX, baseY, fw * TILE_SIZE, fh * TILE_SIZE);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(baseX, baseY, fw * TILE_SIZE, fh * TILE_SIZE);

                g2.setColor(Color.WHITE);
                g2.drawString(obj.id, baseX + 3, baseY + 12);
            }

            // HUD texte en haut de la map (info debug)
            g2.setColor(Color.WHITE);
            g2.drawString(
                    "Mode: " + currentMode +
                    " | TileBrush: " + currentTileBrush +
                    " | TilePaintMode: " + currentTilePaintMode +
                    " | Drag gauche = peindre / Drag droit = effacer | Espace = bascule | P = preview",
                    5, 15
            );

            g2.dispose();
        }

        private Color colorForTile(String id) {
            return switch (id) {
                case "water" -> Color.CYAN;
                case "sand"  -> new Color(240, 230, 140);
                case "dirt"  -> new Color(130, 80, 50);
                case "mud"   -> new Color(110, 70, 40);
                case "lava"  -> new Color(255, 90, 0);
                case "empty" -> new Color(10, 10, 10);
                default      -> new Color(40, 160, 40); // grass
            };
        }
    }

    // ========= MAIN =========
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MapEditor::new);
    }
}
