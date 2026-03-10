package basejeux.model;

public class WeaponDefinition {

    public String id;
    public String name;

    // Stats communes
    public int damage;

    // Action RPG
    public double attackSpeed;     // coups par seconde
    public double animationTime;
    public int range;
    public String shape;           // line / arc / circle

    // RPG tour par tour
    public int actionCost;
    public int initiativeBonus;
}
