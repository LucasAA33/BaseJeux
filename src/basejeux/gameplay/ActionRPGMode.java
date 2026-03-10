package basejeux.gameplay;

import basejeux.model.*;
import java.util.Iterator;
import java.util.Map;

public class ActionRPGMode implements GameMode {

    private final GameMap map;
    private final Player player;
    private final Map<String, GameObjectDefinition> objectDefs;
    private final AnimationTrigger animationTrigger;
    private final Map<String, WeaponDefinition> weaponDefs;
    
    private boolean isLiving(GameMap.MapObject obj) {
        GameObjectDefinition def = objectDefs.get(obj.id);
        if (def == null) return false;

        return "enemy".equals(def.category)
            || "npc".equals(def.category);
    }

    public ActionRPGMode(GameMap map, Player player, Map<String, GameObjectDefinition> objectDefs,Map<String, WeaponDefinition> weaponDefs,
            AnimationTrigger animationTrigger) {
		
		this.map = map;
		this.player = player;
		this.objectDefs = objectDefs;
		this.animationTrigger = animationTrigger;
	    this.weaponDefs = weaponDefs;
		}
    
    private void updateEnemy(GameMap.MapObject obj, double dt) {

        int dx = player.x - obj.x;
        int dy = player.y - obj.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            obj.x += Integer.signum(dx);
        } else {
            obj.y += Integer.signum(dy);
        }

        // Si contact joueur → dégâts
        if (obj.x == player.x && obj.y == player.y) {
            player.damage(5);
            System.out.println("Le joueur prend des dégâts !");
        }
    }
    
    private void updateNpc(GameMap.MapObject obj, double dt) {

        if (Boolean.TRUE.equals(obj.state.get("hostile"))) {
            obj.overrides.put("type", "enemy");
            return;
        }

        // comportement passif
    }
    
    private GameMap.MapObject findTarget(GameMap.MapObject attacker) {

        for (GameMap.MapObject obj : map.objects) {

            if (obj == attacker) continue;

            if (!isLiving(obj)) continue;

            int dx = Math.abs(obj.x - attacker.x);
            int dy = Math.abs(obj.y - attacker.y);

            if (dx + dy == 1) {
                return obj;
            }
        }

        // Player ?
        if (Math.abs(player.x - attacker.x)
          + Math.abs(player.y - attacker.y) == 1) {
            return null; // Player traité séparément
        }

        return null;
    }
    
    private void triggerAttackVisual(GameMap.MapObject attacker,GameObjectDefinition def) {
			if (def.animations != null && def.animations.list.containsKey("attack")) {
				animationTrigger.play(attacker, def, "attack");
				return;
			}
			// Fallback visuel
			attacker.state.put("hitFlash", true);
		}
    
			    
    private void performAttack(GameMap.MapObject attacker,GameMap.MapObject target,GameObjectDefinition def) {
		target.currentHp -= def.attack.damage;
		triggerAttackVisual(attacker, def);
		
		if (target.currentHp <= 0) {
			onObjectDamaged(target, 0);
		}
		System.out.println("Attaque déclenchée");
	}    
    
    private void updateCombatEntity(GameMap.MapObject attacker,GameObjectDefinition def,double dt) {
			if (attacker.attackCooldownRemaining > 0) {
				attacker.attackCooldownRemaining -= dt;
				return;
			}
			GameMap.MapObject target = findTarget(attacker);
			
			if (target != null) {
				performAttack(attacker, target, def);
				attacker.attackCooldownRemaining = def.attack.cooldown;
			}
		}

    @Override
    public void update(double dt) {

        for (GameMap.MapObject obj : map.objects) {

            GameObjectDefinition def = objectDefs.get(obj.id);
            if (def == null) continue;

            if (def.attack != null) {
                updateCombatEntity(obj, def, dt);
            }
        }
    }

    @Override
    public void onObjectDamaged(GameMap.MapObject obj, int damage) {

        obj.currentHp -= damage;
        obj.hpBarTimer = 2.0;

        GameObjectDefinition def = objectDefs.get(obj.id);

        if (obj.currentHp <= 0) {

            if (def != null && def.drops != null) {
                for (Drop d : def.drops) {
                    System.out.println("Drop: " + d.item);
                }
            }

            map.objects.remove(obj);

            System.out.println(obj.id + " détruit !");
        }
    }
    
    private String getEffectiveType(GameMap.MapObject obj) {
        GameObjectDefinition def = objectDefs.get(obj.id);
        if (def == null) return null;

        if (obj.overrides != null && obj.overrides.containsKey("type")) {
            return (String) obj.overrides.get("type");
        }

        return def.type;
    }
    @Override
    public void playerAttack() {

        if (player.isAttacking) return;


        WeaponDefinition weapon =
            weaponDefs.getOrDefault(
                player.currentWeapon,
                weaponDefs.get("fist")
            );

        if (weapon == null) {
            System.out.println("Aucune arme trouvée !");
            return;
        }

        player.isAttacking = true;

        double cooldown = 1.0 / weapon.attackSpeed;
        

        player.attackDuration = weapon.animationTime;
        player.attackCooldownRemaining = cooldown;
        player.maxAttackCooldown = cooldown;

        System.out.println(">>> playerAttack() appelée");

        // === 1️⃣ Calcul de la case devant le joueur ===
        int targetX = player.x;
        int targetY = player.y;

        switch (player.getDirection()) {
            case UP    -> targetY--;
            case DOWN  -> targetY++;
            case LEFT  -> targetX--;
            case RIGHT -> targetX++;
        }

        System.out.println("Attack target tile: " + targetX + "," + targetY);
        System.out.println("Damage value: " + weapon.damage);

        // === 2️⃣ Vérifier tous les objets ===
        for (GameMap.MapObject obj : map.objects) {

            GameObjectDefinition def = objectDefs.get(obj.id);

            int fw = (def != null && def.footprint != null) ? def.footprint.w : 1;
            int fh = (def != null && def.footprint != null) ? def.footprint.h : 1;

            // === 3️⃣ Vérification footprint rectangle ===
            if (targetX >= obj.x && targetX < obj.x + fw &&
                targetY >= obj.y && targetY < obj.y + fh) {

                obj.currentHp -= weapon.damage;
                obj.hpBarTimer = 2.0;

                System.out.println("Hit " + obj.id);
                System.out.println(obj.id + " footprint = " + fw + "x" + fh + " at " + obj.x + "," + obj.y);
            }
        }
    }
}