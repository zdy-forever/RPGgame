package au.edu.anu.comp2120.player;

import au.edu.anu.comp2120.combat.Damageable;
import au.edu.anu.comp2120.combat.Health;
import au.edu.anu.comp2120.combat.EquipmentModifiers;
import au.edu.anu.comp2120.inventory.Inventory;
import au.edu.anu.comp2120.inventory.Equipment;
import au.edu.anu.comp2120.inventory.ItemCatalog;
import au.edu.anu.comp2120.world.CollisionBox;
import au.edu.anu.comp2120.world.WalkableArea;
import java.util.Objects;
import java.util.EnumMap;
import au.edu.anu.comp2120.combat.ElementType;

/**
 * Holds player geometry and health, with time-based eight-direction movement independent of graphics.
 * Damage delegates to the shared health model; death consequences are a future gameplay policy.
 *
 * @author Hao Liu (u7998420)
 * @author Daoyan Zhu (u7782042)
 */
public final class Player implements Damageable, EquipmentModifiers {
    /** Persistent item storage, shared with the existing screen UI. */
    private final Inventory inventory = new Inventory();
    /** Equipment survives map transitions with this player. */
    private final Equipment equipment = new Equipment(inventory, this::refreshHealthCapacity);

    /** Returns this player's bag.
     * @return persistent inventory
     */
    public Inventory getInventory() { return inventory; }

    /** Returns worn items backed by this player's bag.
     * @return persistent equipment owner
     */
    public Equipment getEquipment() { return equipment; }

    /** Reads attack amplification from equipped items.
     * @return equipment damage multiplier
     */
    @Override
    public double equipmentAttackMultiplier() { return 1 + equipment.attackPercent() / 100.0; }

    /** Reads damage reduction from equipped items.
     * @return remaining damage multiplier
     */
    @Override
    public double equipmentDefenceMultiplier() { return 1 - equipment.defencePercent() / 100.0; }

    /** Uses a potion only when alive and injured; full health never consumes an item.
     * @return whether one potion was consumed and health restored
     */
    public boolean useHealthPotion() {
        String id = ItemCatalog.POTION;
        if (!isAlive() || health.getCurrentHp() >= health.getMaxHp() || inventory.quantityOf(id) == 0) {
            return false;
        }
        inventory.removeItem(id, 1);
        health.heal(ItemCatalog.POTION_HEAL);
        return true;
    }

    /** Default player HP capacity; callers can override it with the explicit-capacity constructor. */
    public static final int DEFAULT_MAX_HP = 100;
    /** Per-player health retained during movement and map transitions. */
    private final Health health;
    /** Unequipped HP capacity, retained for save compatibility. */
    private final int baseMaxHp;
    /** Current immutable collision rectangle, anchored at the bottom-left corner. */
    private CollisionBox bounds;
    /** Movement speed in world units per second. */
    private final double speed;
    /** Collision environment used to validate spawning and resolve movement. */
    private WalkableArea area;
    /** Selected immutable class definition; movement and health remain owned by this same Player. */
    private ClassDefinition classDefinition;
    /** Persistent selection per profession; untouched by map/health/encounter reset. */
    private final EnumMap<PlayerClass, UltimateMode> ultimateModes = new EnumMap<>(PlayerClass.class);
    /** Mage's independent element choice, also preserved while another class is selected. */
    private ElementType mageElement = ElementType.FIRE;

    /**
     * Reads the current profession's remembered selection, defaulting to A.
     * @return current class ultimate mode
     */
    public UltimateMode getUltimateMode() {
        return ultimateModes.getOrDefault(getPlayerClass(), UltimateMode.A);
    }

    /**
     * Stores mode only after the ability controller approves Hub selection.
     * @param mode validated selection
     */
    void applyUltimateMode(UltimateMode mode) {
        ultimateModes.put(getPlayerClass(), mode);
    }

    /**
     * Reads Mage's independently selected element.
     * @return one of the five gameplay elements, never NONE
     */
    public ElementType getMageElement() {
        return mageElement;
    }

    /**
     * Applies a validated gameplay element after the controller accepts K.
     * @param element selected non-NONE element
     */
    void applyMageElement(ElementType element) {
        mageElement = element;
    }

    /**
     * Creates a player entirely inside a walkable region with default full health.
     *
     * @param bounds initial position and collision dimensions
     * @param speed positive, finite speed in world units per second
     * @param area collision environment
     * @throws NullPointerException if bounds or area is null
     * @throws IllegalArgumentException if speed is invalid or the spawn is blocked
     */
    public Player(CollisionBox bounds, double speed, WalkableArea area) {
        this(bounds, speed, area, DEFAULT_MAX_HP);
    }

    /**
     * Creates a player with configurable full health without sharing mutable HP with other entities.
     *
     * @param bounds initial position and collision dimensions
     * @param speed positive, finite speed in world units per second
     * @param area collision environment
     * @param maxHp positive initial and maximum hit points
     * @throws NullPointerException if bounds or area is null
     * @throws IllegalArgumentException if speed, spawn, or maxHp is invalid
     */
    public Player(CollisionBox bounds, double speed, WalkableArea area, int maxHp) {
        this(bounds, speed, area, maxHp, PlayerClasses.get(PlayerClasses.DEFAULT));
    }

    /**
     * Initializes a chosen class using the same shared movement, collision and health implementation.
     * @param bounds initial collision rectangle
     * @param speed positive finite ordinary movement speed
     * @param area collision environment
     * @param maxHp positive health capacity, independent of class tuning
     * @param classDefinition validated selected class and common abilities
     * @throws NullPointerException if bounds, area or classDefinition is null
     * @throws IllegalArgumentException if speed, spawn or health capacity is invalid
     */
    public Player(CollisionBox bounds, double speed, WalkableArea area, int maxHp, ClassDefinition classDefinition) {
        this.classDefinition = Objects.requireNonNull(classDefinition, "class definition");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.area = Objects.requireNonNull(area, "area");
        if (!Double.isFinite(speed) || speed <= 0 || !area.isWalkable(bounds)) {
            throw new IllegalArgumentException("Speed must be positive and finite; spawn must be walkable");
        }
        this.speed = speed;
        baseMaxHp = maxHp;
        health = new Health(maxHp);
    }

    /**
     * Returns stable class identity without inspecting a sprite, weapon or Java subclass.
     * @return current class identity
     */
    public PlayerClass getPlayerClass() {
        return classDefinition.type();
    }

    /**
     * Returns current immutable class tuning and attack strategy references.
     * @return selected definition, containing no mutable health/cooldown state
     */
    public ClassDefinition getClassDefinition() {
        return classDefinition;
    }

    /**
     * Applies a definition after the common ability controller validates switching safety.
     * Package access prevents bypass through ordinary screen/session consumers.
     * @param definition complete class definition
     * @throws NullPointerException if definition is null
     */
    void applyClassDefinition(ClassDefinition definition) {
        classDefinition = Objects.requireNonNull(definition, "class definition");
    }

    /**
     * Returns this player's health for healing, explicit restoration, or read-only display queries.
     * Mutate it only through its validated operations on the game-loop thread.
     *
     * @return this player's persistent health model
     */
    public Health getHealth() {
        equipment.reconcile();
        return health;
    }

    /** Updates maximum HP after equipment changes without giving free healing. */
    private void refreshHealthCapacity() {
        health.setMaxHp((int) Math.min(Integer.MAX_VALUE, (long) baseMaxHp + equipment.healthBonus()));
    }

    /** Reads health capacity before equipment.
     * @return base maximum HP
     */
    public int getBaseMaxHp() { return baseMaxHp; }

    /**
     * Delegates damage to the shared model without changing position or triggering visual effects.
     *
     * @param damage non-negative hit points to remove
     * @throws IllegalArgumentException if damage is negative
     */
    @Override
    public void takeDamage(int damage) {
        equipment.reconcile();
        health.takeDamage(damage);
    }

    /**
     * Reports whether the shared model has remaining HP.
     *
     * @return true while this player has positive HP
     */
    @Override
    public boolean isAlive() {
        equipment.reconcile();
        return health.isAlive();
    }

    /**
     * Applies normalized movement. Opposing keys should cancel before this call; when both
     * axes are nonzero, normalization keeps diagonal speed equal to cardinal speed.
     * Zero delta/input does nothing. Collision may allow sliding along a wall.
     * This low-level movement operation does not enforce death-related gameplay restrictions.
     * Invalid arguments are rejected before changing state.
     *
     * @param horizontal horizontal intent: -1 left, 0 idle, 1 right
     * @param vertical vertical intent: -1 down, 0 idle, 1 up
     * @param delta finite, non-negative simulation time in seconds
     * @throws IllegalArgumentException if input, delta, or resulting distance is invalid
     */
    public void updateMovement(int horizontal, int vertical, double delta) {
        if (horizontal < -1 || horizontal > 1 || vertical < -1 || vertical > 1
                || !Double.isFinite(delta) || delta < 0 || !Double.isFinite(getSpeed() * delta)) {
            throw new IllegalArgumentException("Expected axis inputs in [-1, 1] and finite non-negative distance");
        }
        double distance = getSpeed() * delta;
        double length = Math.hypot(horizontal, vertical);
        if (length != 0 && distance != 0) {
            bounds = area.move(bounds, horizontal / length * distance, vertical / length * distance);
        }
    }

    /**
     * Applies a finite displacement through the same swept collision resolver as ordinary movement.
     * Ability controllers own timing and permission; this method grants neither teleportation nor immunity.
     * @param deltaX horizontal world displacement
     * @param deltaY vertical world displacement
     * @throws IllegalArgumentException if either displacement is not finite
     */
    public void moveBy(double deltaX, double deltaY) {
        bounds = area.move(bounds, deltaX, deltaY);
    }

    /**
     * Enters a collision environment at a new bottom-left position, preserving size, speed, and HP.
     * Session coordination should use this method when changing maps. All validation occurs
     * before either the position or collision environment changes.
     *
     * @param destination target collision environment
     * @param x destination horizontal coordinate
     * @param y destination vertical coordinate
     * @throws NullPointerException if destination is null
     * @throws IllegalArgumentException if the full body cannot occupy the destination
     */
    public void enterArea(WalkableArea destination, double x, double y) {
        Objects.requireNonNull(destination, "destination");
        CollisionBox nextBounds = new CollisionBox(x, y, bounds.width(), bounds.height());
        if (!destination.isWalkable(nextBounds)) {
            throw new IllegalArgumentException("Player does not fit at destination (" + x + ", " + y + ")");
        }
        area = destination;
        bounds = nextBounds;
    }

    /**
     * Returns immutable geometry for read-only consumers such as rendering.
     *
     * @return current collision rectangle
     */
    public CollisionBox getBounds() {
        return bounds;
    }

    /**
     * Returns the bottom-left horizontal position.
     *
     * @return horizontal world coordinate
     */
    public double getX() {
        return bounds.x();
    }

    /**
     * Returns the bottom-left vertical position.
     *
     * @return vertical world coordinate
     */
    public double getY() {
        return bounds.y();
    }

    /**
     * Returns the configured movement speed.
     *
     * @return world units travelled per second when unobstructed
     */
    public double getSpeed() {
        return speed * (1 + equipment.speedPercent() / 100.0);
    }

    /**
     * Supplies the current collision contract to launches without exposing map switching authority.
     * @return current collision environment, captured by each projectile at launch
     */
    public WalkableArea getCollisionArea() {
        return area;
    }

    /** Reads every profession's remembered mode for persistence.
     * @return immutable complete selection map
     */
    public java.util.Map<PlayerClass, UltimateMode> savedModes() {
        var result = new EnumMap<PlayerClass, UltimateMode>(PlayerClass.class);
        for (var type : PlayerClass.values()) { result.put(type, ultimateModes.getOrDefault(type, UltimateMode.A)); }
        return java.util.Map.copyOf(result);
    }

    /** Restores validated selections without invoking interactive Hub controls.
     * @param type saved profession
     * @param element saved Mage element
     * @param modes complete profession selections
     * @throws IllegalArgumentException if a selection is incomplete or invalid
     * @throws NullPointerException if a required value is null
     */
    public void restoreSelections(PlayerClass type, ElementType element,
            java.util.Map<PlayerClass, UltimateMode> modes) {
        Objects.requireNonNull(type); Objects.requireNonNull(element);
        var copy = java.util.Map.copyOf(modes);
        if (element == ElementType.NONE || copy.size() != PlayerClass.values().length) {
            throw new IllegalArgumentException("Invalid saved player selections");
        }
        classDefinition = PlayerClasses.get(type);
        mageElement = element;
        ultimateModes.clear(); ultimateModes.putAll(copy);
    }

}
