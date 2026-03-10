package basejeux.gameplay;

import basejeux.model.GameMap;

public class RPGMode implements GameMode {

    private boolean inCombat = false;

    @Override
    public void update(double dt) {
        if (inCombat) {
            // gérer tour
        }
    }
    public void onObjectDamaged(GameMap.MapObject obj, int damage) {
    	
    }
	@Override
	public void playerAttack() {
		// TODO Auto-generated method stub
		
	}
}


