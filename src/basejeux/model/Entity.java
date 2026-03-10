package basejeux.model;

public abstract class Entity {

    protected int x;
    protected int y;
    protected int currentHp;

    public Entity(int x, int y, int hp) {
        this.x = x;
        this.y = y;
        this.currentHp = hp;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void takeDamage(int amount) {
        currentHp -= amount;
        if (currentHp < 0) currentHp = 0;
    }

    public boolean isDead() {
        return currentHp <= 0;
    }

    public int getCurrentHp() {
        return currentHp;
    }
}