package basejeux.model;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import basejeux.model.event.Event;
import basejeux.model.event.EventType;
import basejeux.gameplay.GameMode;
import basejeux.gameplay.ActionRPGMode;
import basejeux.gameplay.AnimationTrigger;
import basejeux.data.DataLoader;


/**
 * Map Debug Viewer (player + gameplay overlay + objets sprites avec footprint)
 */
public class TileManager extends JFrame implements AnimationTrigger {
	
	private GameMode gameMode;
    private final GameMap map;
    private final TileColorMan tileColorManager = new TileColorMan();

    private final int TILE_SIZE = 32;    // pixels par tuile
    private final int VIEW_TILES_W = 20; // nb tuiles visibles
    private final int VIEW_TILES_H = 15;
    
    private int debugAttackX = -1;
    private int debugAttackY = -1;
    private double debugAttackTimer = 0;

    // caméra (en tuiles)
    private int cameraX = 0;
    private int cameraY = 0;

    // joueur debug
    private final Player player = new Player(1, 1);
    
    private final Map<String, WeaponDefinition> weaponDefs = new HashMap<>();

    // touches
    private boolean upPressed, downPressed, leftPressed, rightPressed;

    // timer logique (Timer Swing, pas java.util.Timer)
    private final javax.swing.Timer gameTimer;

    // ====== SPRITES OBJETS ======
    private final Map<String, Image> objectSpriteCache = new HashMap<>();
    
    private final Map<String, GameObjectDefinition> objectDefs = new HashMap<>();

    // ====== SPRITES TERRAINS ======
    private final Map<String, Image> terrainSpriteCache = new HashMap<>();

    // id spécial interne pour le joueur dans le tri z-order
    private static final String PLAYER_RENDER_ID = "__PLAYER__";
    
    private static class ActiveAnimation {
        AnimationDef def;
        Image sheet;
        double timer = 0;
        int currentFrame = 0;
        boolean playing = false;
    }
    
    private final Map<GameMap.MapObject, ActiveAnimation> activeAnimations = new HashMap<>();



    public TileManager(GameMap map) {
    	
    	gameMode = new ActionRPGMode(map, player, objectDefs, weaponDefs,(obj, def, animName) -> playAnimation(obj, animName));
        this.map = map;
        
        setTitle("Map Debug Viewer (Player / Gameplay)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // charge définitions d’objets (toutes les familles trouvées)
        loadObjectDefinitionsFolder("assets/data/environment");
        loadObjectDefinitionsFolder("assets/data/items"); // si besoin
        loadWeaponDefinitionsFolder("assets/data/weapons");
        
        for (GameMap.MapObject obj : map.objects) {

            GameObjectDefinition def = objectDefs.get(obj.id);
            if (def == null) continue;

            int hp = def.hp;

            if (obj.overrides != null && obj.overrides.containsKey("hp")) {
                hp = ((Number) obj.overrides.get("hp")).intValue();
            }

            obj.currentHp = hp;
        }

        // DEBUG : voir les IDs
        System.out.println("Objets chargés par TileManager : " + objectDefs.keySet());

        // pre-cache minimal des sprites (à la demande sinon)
        preloadObjectSprites();

        MapPanel mapPanel = new MapPanel();
        add(mapPanel);
        mapPanel.setPreferredSize(new Dimension(VIEW_TILES_W * TILE_SIZE, VIEW_TILES_H * TILE_SIZE));

        // clavier
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyReleased(e);
            }
        });

        setFocusable(true);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // timer ~ 20 FPS logique
        gameTimer = new javax.swing.Timer(50, e -> {
            updateLogic(0.050);
            mapPanel.repaint();
        });
        gameTimer.start();
    }

    // ---------- chargement defs / sprites ----------
    private void loadObjectDefinitionsFolder(String folder) {
        try {
            Map<String, GameObjectDefinition> defs = DataLoader.loadFamilyFolderPath(folder);
            if (defs != null) objectDefs.putAll(defs);
        } catch (Exception ignored) {}
    }
    
 // ---------- chargement des armes ----------
    private void loadWeaponDefinitionsFolder(String folder) {
        try {
            Map<String, WeaponDefinition> defs =
                DataLoader.loadWeaponFolderPath(folder);
            if (defs != null)
                weaponDefs.putAll(defs);
        } catch (Exception ignored) {}
    }

    private void preloadObjectSprites() {
        for (GameObjectDefinition def : objectDefs.values()) {
            if (def.sprite != null && !objectSpriteCache.containsKey(def.id)) {
                objectSpriteCache.put(def.id, tryLoadImage(def.sprite));
            }
        }
    }

    private Image tryLoadImage(String path) {
        try {
            File f = new File(path);
            boolean exists = f.exists();
            if (!exists) return null;
            return new ImageIcon(path).getImage();
        } catch (Exception e) {
            System.out.println("[SPRITE] ERREUR " + path + " : " + e.getMessage());
            return null;
        }
    }

    private Image getObjectImage(String id) {
        Image img = objectSpriteCache.get(id);
        if (img != null) return img;
        GameObjectDefinition def = objectDefs.get(id);
        if (def != null && def.sprite != null) {
            img = tryLoadImage(def.sprite);
            if (img != null) objectSpriteCache.put(id, img);
        }
        return img;
    }

    private Image getTerrainImage(String tileId) {
        Image img = terrainSpriteCache.get(tileId);
        if (img != null) return img;

        TerrainDefinition def = tileColorManager.getTerrainDef(tileId);
        if (def == null || def.image == null) return null;

        img = tryLoadImage(def.image);
        if (img != null) terrainSpriteCache.put(tileId, img);
        return img;
    }

    // ---------- input ----------
    private void handleKeyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {

            // caméra
            case KeyEvent.VK_LEFT  -> cameraX = Math.max(0, cameraX - 1);
            case KeyEvent.VK_RIGHT -> cameraX = Math.min(map.width  - VIEW_TILES_W, cameraX + 1);
            case KeyEvent.VK_UP    -> cameraY = Math.max(0, cameraY - 1);
            case KeyEvent.VK_DOWN  -> cameraY = Math.min(map.height - VIEW_TILES_H, cameraY + 1);
            

            // joueur
            case KeyEvent.VK_Z, KeyEvent.VK_K -> upPressed = true;
            case KeyEvent.VK_S, KeyEvent.VK_J -> downPressed = true;
            case KeyEvent.VK_Q, KeyEvent.VK_H -> leftPressed = true;
            case KeyEvent.VK_D, KeyEvent.VK_L -> rightPressed = true;
            
            case KeyEvent.VK_SPACE -> {
                gameMode.playerAttack();
            }

            case KeyEvent.VK_E -> {
            	
                if (map.objects == null) return;

                for (GameMap.MapObject obj : map.objects) {
                	System.out.println(
                			  "[DEBUG] INTERACT with " + obj.id +
                			  " events=" + obj.events
                			);

                    int dx = Math.abs(obj.x - player.x);
                    int dy = Math.abs(obj.y - player.y);

                    if (dx <= 2 && dy <= 2 && obj.events != null) {
                        for (Event ev : obj.events) {
                            if (ev != null && ev.type == EventType.INTERACT) {
                                executeEvent(ev, obj);
                                return;
                            }
                        }
                    }
                }
            }
            }
        }

    
    private void executeEvent(Event e, GameMap.MapObject obj) {

        if (e.type == EventType.INTERACT) {

            // 1️⃣ Appliquer les états persistants
            if (e.setState != null) {
                obj.state.putAll(e.setState);

            }

            // 2️⃣ Jouer l’animation si présente
            if (e.animation != null) {
                playAnimation(obj, e.animation);
            }
        }

        if (e.type == EventType.TELEPORT) {
            System.out.println("[EVENT] Téléport vers " + e.teleportTo);
        }
    }
    public void debugShowAttack(int x, int y) {
        debugAttackX = x;
        debugAttackY = y;
        debugAttackTimer = 0.2;
    }



    private void handleKeyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_Z, KeyEvent.VK_K -> upPressed = false;
            case KeyEvent.VK_S, KeyEvent.VK_J -> downPressed = false;
            case KeyEvent.VK_Q, KeyEvent.VK_H -> leftPressed = false;
            case KeyEvent.VK_D, KeyEvent.VK_L -> rightPressed = false;
        }
    }
    

    // ---------- logique ----------
    private void updateLogic(double dt) {

        if (player.moveCooldownMs > 0) {
            player.moveCooldownMs -= (dt * 1000.0);
            if (player.moveCooldownMs < 0) player.moveCooldownMs = 0;
        }
        if (debugAttackTimer > 0) {
            debugAttackTimer -= dt;
            if (debugAttackTimer <= 0) {
                debugAttackX = -1;
                debugAttackY = -1;
            }
        }
        
        if (player.attackDuration > 0) {
            player.attackDuration -= dt;

            if (player.attackDuration <= 0) {
                player.isAttacking = false;
            }
        }

        if (player.attackCooldownRemaining > 0) {
            player.attackCooldownRemaining -= dt;
        }
        
        for (GameMap.MapObject obj : map.objects) {
            if (obj.hpBarTimer > 0) {
                obj.hpBarTimer -= dt;
            }
        }

        if (player.moveCooldownMs <= 0) {
            int nx = player.x, ny = player.y;
            if (upPressed) ny--;
            if (downPressed) ny++;
            if (leftPressed) nx--;
            if (rightPressed) nx++;

            if (nx != player.x || ny != player.y)
                tryMovePlayerTo(nx, ny);
        }
        
        if (Boolean.TRUE.equals(player.state.get("attackFlash"))) {
            int t = (int) player.state.getOrDefault("flashTimer", 0);
            t++;
            player.state.put("flashTimer", t);

            if (t > 5) {  // 5 frames ≈ 250 ms
                player.state.remove("attackFlash");
                player.state.remove("flashTimer");
            }
        }
        
        for (GameMap.MapObject obj : map.objects) {
            if (Boolean.TRUE.equals(obj.state.get("flash"))) {
                obj.state.put("flashTimer",
                    ((int) obj.state.getOrDefault("flashTimer", 0)) + 1);

                if ((int) obj.state.get("flashTimer") > 5) {
                    obj.state.remove("flash");
                    obj.state.remove("flashTimer");
                }
            }
        }

        applyTileDamage(dt);
        updateAnimations(dt);

        // 👉 SEULEMENT le gameplay combat ici
        gameMode.update(dt);
    }
    
    
    private void updateAnimations(double dt) {

        if (activeAnimations.isEmpty())
            return;

        double elapsedMs = dt * 1000.0;

        List<GameMap.MapObject> toRemove = new ArrayList<>();

        for (Map.Entry<GameMap.MapObject, ActiveAnimation> entry : activeAnimations.entrySet()) {
            GameMap.MapObject obj = entry.getKey();
            ActiveAnimation anim = entry.getValue();

            if (!anim.playing) {
            	toRemove.add(obj);
                continue;
            }

            anim.timer += elapsedMs;

            int frameDuration = anim.def.duration_ms;
            if (frameDuration <= 0) frameDuration = 100;

            while (anim.timer >= frameDuration) {
                anim.timer -= frameDuration;
                anim.currentFrame++;

                if (anim.currentFrame >= anim.def.frames) {
                    anim.currentFrame = anim.def.frames - 1;
                    anim.playing = false;
                    toRemove.add(obj);
                    break;
                }
            }
        }

        for (GameMap.MapObject obj : toRemove) {
            activeAnimations.remove(obj);
        }
    }




    private void applyTileDamage(double dt) {
    	if (player.x < 0 || player.y < 0 ||
    		    player.x >= map.width || player.y >= map.height)
    		    return;
        String tileId = map.tiles[player.y][player.x];
        TerrainDefinition def = tileColorManager.getTerrainDef(tileId);
        if (def != null && def.damagePerSecond > 0) {
        	player.damage((int)(def.damagePerSecond * dt));
        }
    }
    
    private void playAnimation(GameMap.MapObject obj, String animName) {

        GameObjectDefinition def = objectDefs.get(obj.id);
        if (def == null) return;

        if (def.animations == null ||
            !def.animations.list.containsKey(animName)) {

            obj.state.put("flash", true);
            return;
        }

        AnimationDef ad = def.animations.list.get(animName);
        if (ad == null) {
            System.out.println("[ANIM] Animation introuvable : " + animName);
            return;
        }

        Image sheet = tryLoadImage(ad.sprite_sheet);
        if (sheet == null) {
            System.out.println("[ANIM] Sprite sheet introuvable : " + ad.sprite_sheet);
            return;
        }

        ActiveAnimation state = new ActiveAnimation();
        state.def = ad;
        state.sheet = sheet;          // ✅ LIGNE CRITIQUE
        state.currentFrame = 0;
        state.timer = 0;
        state.playing = true;

        activeAnimations.put(obj, state);

        System.out.println("[ANIM] Lancement animation : " + animName);
        
        if (def.animations == null ||
        	    !def.animations.list.containsKey(animName)) {

        	    System.out.println("FLASH déclenché sur " + obj.id);
        	    obj.state.put("flash", true);
        	    return;
        	}

    }



    private void centerCameraOnPlayer() {
        cameraX = player.x - VIEW_TILES_W / 2;
        cameraY = player.y - VIEW_TILES_H / 2;
        if (cameraX < 0) cameraX = 0;
        if (cameraY < 0) cameraY = 0;
        if (cameraX > map.width  - VIEW_TILES_W) cameraX = map.width  - VIEW_TILES_W;
        if (cameraY > map.height - VIEW_TILES_H) cameraY = map.height - VIEW_TILES_H;
    }

    // collisions footprint objets
    private boolean isBlockedByObjectFootprint(int x, int y) {
        for (GameMap.MapObject obj : map.objects) {
            GameObjectDefinition def = objectDefs.get(obj.id);
            boolean collidable = (def == null || def.collidable == null) ? true : def.collidable;
            if (!collidable) continue;
            int fw = (def != null && def.footprint != null) ? def.footprint.w : 1;
            int fh = (def != null && def.footprint != null) ? def.footprint.h : 1;

            // obj.x,obj.y = coin haut-gauche de l’emprise
            if (x >= obj.x && x < obj.x + fw &&
                y >= obj.y && y < obj.y + fh) {
                return true;
            }
        }
        return false;
    }

    private void tryMovePlayerTo(int nx, int ny) {
        if (nx < 0 || ny < 0 || nx >= map.width || ny >= map.height) return;

        // collision objets footprint
        if (isBlockedByObjectFootprint(nx, ny)) return;

        // terrain
        String tileId = map.tiles[ny][nx];
        TerrainDefinition def = tileColorManager.getTerrainDef(tileId);
        if (def == null) return;
        if (!def.walkable) return;

        // OK
        player.x = nx;
        player.y = ny;

        double base = 150.0;
        double sp = def.slowFactor <= 0 ? 0.0001 : def.slowFactor;
        player.moveCooldownMs = base / sp;

        centerCameraOnPlayer();
    }

    // ---------- rendu ----------
    private class MapPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // ========== 1) AFFICHAGE DU TERRAIN ==========
            for (int ty = 0; ty < VIEW_TILES_H; ty++) {
                for (int tx = 0; tx < VIEW_TILES_W; tx++) {

                    int mapX = cameraX + tx;
                    int mapY = cameraY + ty;

                    if (mapX < 0 || mapY < 0 || mapX >= map.width || mapY >= map.height)
                        continue;

                    String tileId = map.tiles[mapY][mapX];
                    Image tileImg = getTerrainImage(tileId);

                    if (tileImg != null) {
                        g2.drawImage(tileImg,
                                tx * TILE_SIZE, ty * TILE_SIZE,
                                TILE_SIZE, TILE_SIZE,
                                this);
                    }
                }
            }

            // ========== 2) DÉTERMINER LES OBJETS VISIBLES ==========
            List<GameMap.MapObject> visibles = new ArrayList<>();

            for (GameMap.MapObject obj : map.objects) {
                if (obj.x >= cameraX && obj.x < cameraX + VIEW_TILES_W &&
                    obj.y >= cameraY && obj.y < cameraY + VIEW_TILES_H) {
                    visibles.add(obj);
                }
            }

            // Ajouter le joueur comme un "objet"
            if (player.x >= cameraX && player.x < cameraX + VIEW_TILES_W &&
                player.y >= cameraY && player.y < cameraY + VIEW_TILES_H) {
                GameMap.MapObject fakePlayer =
                        new GameMap.MapObject(PLAYER_RENDER_ID, player.x, player.y);
                visibles.add(fakePlayer);
            }

            // ========== 3) ORDRE DE RENDU PAR PROFONDEUR (Z-ORDER) ==========
            visibles.sort((o1, o2) -> Integer.compare(o1.y, o2.y));

            // ========== 4) AFFICHAGE DES OBJETS ET ANIMATIONS ==========
            for (GameMap.MapObject obj : visibles) {

                // --- JEUER ---
            	if (PLAYER_RENDER_ID.equals(obj.id)) {

            	    int px = (obj.x - cameraX) * TILE_SIZE;
            	    int py = (obj.y - cameraY) * TILE_SIZE;

            	    // Joueur
            	    g2.setColor(Color.RED);
            	    g2.fillRect(px + 4, py + 4, TILE_SIZE - 8, TILE_SIZE - 8);

            	    g2.setColor(Color.BLACK);
            	    g2.drawRect(px + 4, py + 4, TILE_SIZE - 8, TILE_SIZE - 8);

            	    // =====================
            	    // 🔥 COOLDOWN BAR
            	    // =====================
            	    if (player.attackCooldownRemaining > 0 && player.maxAttackCooldown > 0) {

            	        double ratio = player.attackCooldownRemaining / player.maxAttackCooldown;

            	        int barWidth = (int)(TILE_SIZE * ratio);
            	        int barHeight = 3;

            	        g2.setColor(new Color(255, 140, 0, 200));
            	        g2.fillRect(px, py + TILE_SIZE - barHeight, barWidth, barHeight);
            	    }
            	    continue;
            	}

                // --- OBJET NORMAL ---
                GameObjectDefinition def = objectDefs.get(obj.id);
                if (def == null) continue;

                int fw = (def.footprint != null ? def.footprint.w : 1);
                int fh = (def.footprint != null ? def.footprint.h : 1);

                int baseX = (obj.x - cameraX) * TILE_SIZE;
                int baseY = (obj.y - cameraY) * TILE_SIZE;

                int drawW = (def.spriteSize != null ? def.spriteSize.w : fw * TILE_SIZE);
                int drawH = (def.spriteSize != null ? def.spriteSize.h : fh * TILE_SIZE);

                int offX = (def.offset != null ? def.offset.x : 0);
                int offY = (def.offset != null ? def.offset.y : 0);

                String anchor = (def.anchor != null ? def.anchor : "bottom");

                int drawX = baseX + offX;
                int drawY = baseY + offY;

                // Ancrage de l’image
                if ("bottom".equalsIgnoreCase(anchor)) {
                    drawY = baseY + (fh * TILE_SIZE) - drawH + offY;
                } else if ("center".equalsIgnoreCase(anchor)) {
                    drawX = baseX + (fw * TILE_SIZE - drawW) / 2 + offX;
                    drawY = baseY + (fh * TILE_SIZE - drawH) / 2 + offY;
                }
                
                if (def.hp > 0 && obj.hpBarTimer > 0) {

                    int maxHp = def.hp;
                    int hp = Math.max(0, obj.currentHp);

                    int barWidth = drawW;
                    int barHeight = 4;

                    int hpWidth = (int)((hp / (double) maxHp) * barWidth);

                    int barY = drawY - 6;

                    // Fond sombre semi-transparent
                    g2.setColor(new Color(0, 0, 0, 180));
                    g2.fillRoundRect(drawX, barY, barWidth, barHeight, 4, 4);

                    // Vie verte (plus joli que rouge)
                    g2.setColor(new Color(80, 220, 80));
                    g2.fillRoundRect(drawX, barY, hpWidth, barHeight, 4, 4);
                    
                    
                    if (debugAttackX >= cameraX && debugAttackX < cameraX + VIEW_TILES_W &&
                    	    debugAttackY >= cameraY && debugAttackY < cameraY + VIEW_TILES_H) {

                    	    int sx = debugAttackX - cameraX;
                    	    int sy = debugAttackY - cameraY;

                    	    int px = sx * TILE_SIZE;
                    	    int py = sy * TILE_SIZE;

                    	    g.setColor(new Color(255, 0, 0, 150));
                    	    g.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                    	}
                    
                }

                // ========== ANIMATION EN COURS ? ==========
                ActiveAnimation anim = activeAnimations.get(obj);

                if (anim != null && anim.playing && anim.sheet != null) {

                    int fwAnim = anim.def.frameWidth;
                    int fhAnim = anim.def.frameHeight;

                    int sx = anim.currentFrame * fwAnim;
                    int sy = 0; // une seule ligne

                    g2.drawImage(
                    	    anim.sheet,
                    	    drawX, drawY, drawX + drawW, drawY + drawH,
                    	    sx, sy, sx + fwAnim, sy + fhAnim,
                    	    this
                    	);

                    continue;
                }


                // ========== SINON : SPRITE NORMAL ==========

                String spritePath = null;
               

	             // 1️⃣ Sprite selon l’état
                if (obj.state != null && def.stateSprites != null) {
                    for (Map.Entry<String, Object> entry : obj.state.entrySet()) {
                        if (Boolean.TRUE.equals(entry.getValue())) {
                            String candidate = def.stateSprites.get(entry.getKey());
                            if (candidate != null) {
                                spritePath = candidate;
                                break;
                            }
                        }
                    }
                }
	
	             // 2️⃣ Fallback sprite par défaut
	             if (spritePath == null) {
	                 spritePath = def.sprite;
	             }
	
	             Image sprite = tryLoadImage(spritePath);

	             if (sprite != null) {
	            	    g2.drawImage(sprite, drawX, drawY, drawW, drawH, this);
	            	}

	            	// 🔥 Flash au-dessus du sprite
	            	if (Boolean.TRUE.equals(obj.state.get("flash"))) {
	            	    g2.setColor(Color.BLACK);
	            	    g2.fillRect(drawX, drawY, drawW, drawH);
	            	    obj.state.remove("flash");
	            	}
	            	
            }
        	// ================== UI JOUEUR ==================

        	int uiWidth = 150;
        	int uiHeight = 14;

        	int margin = 20;

        	int x = margin;
        	int y = margin;

        	int maxHp = 100; // ou player.maxHp si tu l’ajoutes
        	int hp = Math.max(0, player.currentHp);

        	int hpWidth = (int)((hp / (double)maxHp) * uiWidth);

        	// Fond
        	g2.setColor(new Color(0, 0, 0, 180));
        	g2.fillRoundRect(x - 4, y - 4, uiWidth + 8, uiHeight + 8, 8, 8);

        	// Barre rouge
        	g2.setColor(new Color(40, 230, 40));
        	g2.fillRoundRect(x, y, hpWidth, uiHeight, 6, 6);

        	// Bordure
        	g2.setColor(Color.WHITE);
        	g2.drawRoundRect(x, y, uiWidth, uiHeight, 6, 6);   
        }


    } // ← FIN de MapPanel (il faut fermer cette classe ici !)
    
    @Override
    public void play(GameMap.MapObject obj,
                     GameObjectDefinition def,
                     String animationName) {

        playAnimation(obj, animationName);
    }

    // ==== MAIN PROGRAM ====
    public static void main(String[] args) {
        GameMap map = DataLoader.loadMap("assets/data/maps/map_created.json");
        if (map != null)
            SwingUtilities.invokeLater(() -> new TileManager(map));
        else
            System.out.println("Erreur : impossible de charger la map.");
    }
} // ← FIN de TileManager
