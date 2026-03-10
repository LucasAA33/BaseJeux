package basejeux.model;

public class TerrainDefinition {
    public String id;
    public String name;
    public String image; // chemin vers le .png
    public boolean walkable;
    public int damagePerSecond;
    public double slowFactor;
    public boolean collidable;
	public Object color;
}