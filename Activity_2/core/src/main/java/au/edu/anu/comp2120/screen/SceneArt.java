package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.BattleResult;
import au.edu.anu.comp2120.combat.*;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.enemy.BossState;
import au.edu.anu.comp2120.inventory.MaterialDrop;
import au.edu.anu.comp2120.inventory.ItemCatalog;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.player.*;
import au.edu.anu.comp2120.world.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Read-only art presentation for existing gameplay. Animation never applies damage, moves actors,
 * advances a skill or delays automatic returns. All temporary pictures are bounded and map-scoped.
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 */
public final class SceneArt implements Disposable {
    /** Supplied warning artwork, used only for read-only melee preparation feedback. */
    static final String BOSS_WARNING_IMAGE = "art/boss/common/attack_warning.png";
    /** Normal Mage projectile width in world units, independent of source image resolution. */
    private static final float MAGE_BOLT_WIDTH = 24;
    /** Boss projectile art is twice as wide and twice as tall as the same Mage image. */
    private static final float BOSS_BOLT_SCALE = 2;
    /** Sole owner of lazy GPU images. */
    private final ArtAssets art = new ArtAssets();
    /** Small bounded set of short-lived confirmed-hit pictures. */
    private final List<Effect> effects = new ArrayList<>();
    /** Current map identity for visual cleanup. */
    private GameMap map;
    /** Current practice adapter identity, also detecting an R reset. */
    private Object practice;
    /** Presentation clock, periodically wrapped to avoid float precision loss. */
    private double time;
    /** Most recent player position for locomotion presentation only. */
    private CollisionBox previousPlayer;
    /** Current locomotion observation. */
    private boolean moving;
    /** Current player strip path. */
    private String playerStrip = "";
    /** Elapsed time in the selected player animation. */
    private double playerAge;
    /** Current skill phase, used only to detect presentation transitions. */
    private String phase = "NONE";
    /** Elapsed time since the skill phase changed. */
    private double phaseAge;
    /** Previous slot cooldowns, detecting accepted actions rather than raw button presses. */
    private final double[] cooldowns = new double[AbilitySlot.values().length];
    /** Last accepted slot, retained only for a short launch animation. */
    private AbilitySlot launch;
    /** Remaining launch animation seconds. */
    private double launchRemaining;
    /** Per-portal transition pictures, discarded on map changes. */
    private final Map<String, Gate> gates = new HashMap<>();
    /** Last fixed field bounds, permitting a visual-only end picture. */
    private CollisionBox lastField;
    /** Last field owner, used for the end picture. */
    private PlayerClass fieldClass;
    /** Element captured for the visible Mage field. */
    private ElementType fieldElement = ElementType.NONE;
    /** Boss animation state and clock; no enemy state mutation. */
    private String bossStrip = "";
    /** Seconds in the current Boss strip. */
    private double bossAge;
    /** Brief confirmed player damage animation, independent of input availability. */
    private double playerHurt;
    /** Brief confirmed guardian damage animation. */
    private double bossHurt;
    /** Finite follow-through strip after a defensive/finisher phase has ended. */
    private String followThrough = "";
    /** Remaining follow-through visual time. */
    private double followRemaining;
    /** Immutable last settled result for a short Hub HUD replay, never an encounter actor. */
    private BattleResult outcome;
    /** Player identity used by a failure picture even if the user immediately changes class. */
    private PlayerClass outcomeClass;
    /** Seconds in the non-blocking outcome presentation. */
    private double outcomeAge;
    /** Both victory and failure remain visible for three presentation seconds after returning. */
    static final double OUTCOME_SECONDS = 3;
    /** Feedback lifetime, unrelated to gameplay timing. */
    private static final double EFFECT_SECONDS = 0.4;
    /** Maximum simultaneous feedback pictures. */
    private static final int MAX_EFFECTS = 128;
    /** Reused text measurement for the short potion count; avoids allocating a layout every frame. */
    private final GlyphLayout potionCountText = new GlyphLayout();

    /**
     * One disposable, reference-free visual effect.
     * @author Daoyan Zhu (u7782042)
     */
    private static final class Effect {
        /** Runtime asset path. */ private final String path;
        /** Captured world geometry. */ private final CollisionBox bounds;
        /** Total display time. */ private final double duration;
        /** Presentation age. */ private double age;
        /**
         * Captures an immutable effect description.
         * @param path image path
         * @param bounds world bounds
         * @param duration lifetime
         */
        private Effect(String path, CollisionBox bounds, double duration) {
            this.path = path; this.bounds = bounds; this.duration = duration;
        }
    }

    /**
     * Portal visual transition, separate from travel permissions.
     * @author Daoyan Zhu (u7782042)
     */
    private static final class Gate {
        /** Last observed permission. */ private boolean enabled;
        /** Seconds since the observed permission changed. */ private double age = 1;
        /**
         * Captures initial permission without playing an invented transition.
         * @param enabled initial permission
         */
        private Gate(boolean enabled) { this.enabled = enabled; }
    }

    /** Creates an empty presentation; loading begins only when a visible asset is needed. */
    public SceneArt() { }

    /**
     * Starts non-blocking return feedback after gameplay has already completed its return.
     * This neither delays travel nor consumes the Inventory/Quest reward payload.
     * @param result immutable completed battle result
     * @param playerClass class at settlement
     * @throws NullPointerException if either snapshot is null
     */
    public void showOutcome(BattleResult result, PlayerClass playerClass) {
        java.util.Objects.requireNonNull(result, "result");
        java.util.Objects.requireNonNull(playerClass, "player class");
        outcome = result; outcomeClass = playerClass; outcomeAge = 0;
    }

    /**
     * Samples settled gameplay and advances only presentation clocks.
     * @param next current map
     * @param player persistent player
     * @param demo existing gameplay coordinator
     * @param delta non-negative finite visual seconds, zero when paused
     */
    public void update(GameMap next, Player player, CombatDemo demo, double delta) {
        if (map != next || practice != demo.getTrainingDummy().orElse(null)) {
            effects.clear(); gates.clear(); previousPlayer = null; lastField = null;
            playerStrip = ""; bossStrip = ""; launch = null; launchRemaining = 0;
            for (AbilitySlot slot : AbilitySlot.values()) {
                cooldowns[slot.ordinal()] = demo.getAbilities().getCooldownRemaining(slot);
            }
            phase = "NONE"; phaseAge = 0; playerHurt = 0; bossHurt = 0; followRemaining = 0;
            map = next; practice = demo.getTrainingDummy().orElse(null);
            effect("art/common_vfx/spawn.png", expanded(player.getBounds(), 18), 0.6);
        }
        time = (time + delta) % 3600;
        effects.forEach(effect -> effect.age += delta);
        effects.removeIf(effect -> effect.age >= effect.duration);
        gates.values().forEach(gate -> gate.age += delta);
        moving = previousPlayer != null && (previousPlayer.x() != player.getX() || previousPlayer.y() != player.getY());
        previousPlayer = player.getBounds();
        PlayerAbilities abilities = demo.getAbilities();
        String nextPhase = abilities.getSkillStatus();
        String previousPhase = phase;
        phaseAge = nextPhase.equals(phase) ? phaseAge + delta : 0;
        phase = nextPhase;
        playerHurt = Math.max(0, playerHurt-delta);
        bossHurt = Math.max(0, bossHurt-delta);
        followRemaining = Math.max(0, followRemaining-delta);
        if (previousPhase.equals("FINISHER") && phase.equals("NONE")) {
            followThrough = "o_helmbreaker/recovery/recovery_4f.png"; followRemaining = 0.4;
        }
        launchRemaining = Math.max(0, launchRemaining - delta);
        for (AbilitySlot slot : AbilitySlot.values()) {
            double value = abilities.getCooldownRemaining(slot);
            if (value > cooldowns[slot.ordinal()] + 0.00001) {
                launch = slot; launchRemaining = 0.35;
                playerStrip = "";
            }
            cooldowns[slot.ordinal()] = value;
        }
        for (HitFeedback hit : demo.drainHitFeedback()) {
            if (hit.blocked() && previousPhase.equals("STANCE")) {
                followThrough = "o_watermoon/counter/counter_6f.png"; followRemaining = 0.6;
            } else if (!hit.blocked()) {
                if (hit.bounds().overlaps(player.getBounds())) { playerHurt = 0.3; }
                else { bossHurt = 0.3; }
            }
            String path = hit.blocked() ? "art/skills/katana/parry_flash.png"
                    : !hit.reaction().isEmpty() ? "art/reactions/" + hit.reaction().replace('-', '_') + ".png"
                    : hit.element() != ElementType.NONE ? "art/elements/" + lower(hit.element()) + "/hit.png"
                    : "art/common_vfx/physical_hit.png";
            effect(path, expanded(hit.bounds(), 16), EFFECT_SECONDS);
            effect("art/common_vfx/hit_flash.png", hit.bounds(), 0.2);
        }
        CollisionBox field = abilities.getSkillFieldBounds().orElse(null);
        if (field != null && lastField == null) {
            fieldClass = player.getPlayerClass(); fieldElement = player.getMageElement();
        }
        if (field == null && lastField != null) {
            effect(fieldClass == PlayerClass.ARCHER ? "art/skills/archer/arrow_rain_end.png"
                    : "art/skills/mage/magic_circle/disappear.png", lastField, 0.45);
        }
        lastField = field;
        String selected = playerAnimation(player, abilities);
        playerAge = selected.equals(playerStrip) ? playerAge + delta : 0;
        playerStrip = selected;
        String bossSelected = bossHurt > 0 ? "hit/hit_3f.png" : bossAnimation(demo);
        bossAge = bossSelected.equals(bossStrip) ? bossAge + delta : 0;
        bossStrip = bossSelected;
    }

    /**
     * Selects a deterministic elemental theme for each battle, or the visit's random practice theme.
     * @param map map identity
     * @param demo gameplay observation
     * @return lowercase elemental background name
     */
    private static String arenaTheme(GameMap map, CombatDemo demo) {
        return lower(demo.getTrainingAppearance().map(TrainingAppearance::arena)
                .orElseGet(() -> BossChallenge.forMap(map.getId()).orElse(BossChallenge.FIRE)));
    }

    /**
     * Draws an elemental background; peaceful Hub and Memorial Hall retain procedural scenery.
     * Extending the border beyond playable bounds keeps the decorative wall outside collision space.
     * @param batch active world batch
     * @param map current map
     * @param demo current gameplay
     */
    public void background(SpriteBatch batch, GameMap map, CombatDemo demo) {
        if (map.getType() == MapType.BATTLE || map.getType() == MapType.TRAINING) {
            CollisionBox bounds = map.getBounds();
            art.draw(batch, "art/maps/battle/" + arenaTheme(map, demo) + "_arena.png",
                    new CollisionBox(bounds.x()-bounds.width()/3, bounds.y()-bounds.height()/3,
                            bounds.width()*5/3, bounds.height()*5/3), 0, false, false);
        }
    }

    /**
     * Draws the fixed memorial circle and five stones; only confirmed guardian victories light them.
     * This view consumes a progress snapshot and never awards inventory items or starts the ending.
     * @param batch active world batch
     * @param completed immutable or caller-owned read-only guardian completion snapshot
     */
    public void memorial(SpriteBatch batch, Set<BossChallenge> completed) {
        if (map.getType() != MapType.MEMORIAL) { return; }
        draw(batch, "art/memorial/magic_circle.png", GameConfig.MEMORIAL_CIRCLE);
        for (BossChallenge target : BossChallenge.values()) {
            boolean lit = completed.contains(target);
            if (lit) { batch.setColor(Color.WHITE); }
            else { batch.setColor(0.20f, 0.20f, 0.24f, 0.65f); }
            draw(batch, "art/memorial/" + lower(target) + "_stone.png", GameConfig.MEMORIAL_STONES.get(target));
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * Draws fixed ground effects and Hub props before actors.
     * @param batch active world batch
     * @param map current map
     * @param demo current gameplay
     */
    public void ground(SpriteBatch batch, GameMap map, CombatDemo demo) {
        if (map.getType() == MapType.HUB) {
            String[] props = {"props/forge", "props/class_altar", "props/training_dummy",
                    "decorations/banner", "decorations/lamp", "decorations/plant", "decorations/furniture"};
            double[][] positions = {{GameConfig.FORGE_BOUNDS.x(),GameConfig.FORGE_BOUNDS.y()},
                    {345,155},{520,110},{390,240},{520,230},{80,210},{510,310}};
            for (int i = 0; i < props.length; i++) {
                draw(batch, "art/hub/" + props[i] + ".png", new CollisionBox(positions[i][0],positions[i][1],40,40));
            }
        }
        for (ElementField field : demo.getElementFields()) {
            draw(batch, "art/elements/" + lower(field.getDefinition().element()) + "/ground_field.png", field.getBounds());
        }
        if (lastField != null && !phase.equals("CAST")) {
            String path = fieldClass == PlayerClass.ARCHER ? "art/skills/archer/arrow_rain_"
                    + (phaseAge < 0.5 ? "start" : "loop") + ".png"
                    : "art/skills/mage/magic_circle/" + (phaseAge < 0.6 ? "appear" : "loop") + ".png";
            art.draw(batch, path, lastField, phaseAge, true, false);
            if (fieldClass == PlayerClass.MAGE && fieldElement != ElementType.NONE) {
                draw(batch, "art/elements/" + lower(fieldElement) + "/ground_field.png", lastField);
            }
        }
    }

    /**
     * Draws existing actors, portals, projectiles and bounded effects without mutating them.
     * @param batch active world batch
     * @param player persistent player
     * @param npcs current NPCs, possibly null
     * @param portals current map's portal snapshots
     * @param demo gameplay coordinator
     * @param drops current reward snapshots
     */
    public void actors(SpriteBatch batch, Player player, List<NPC> npcs, List<Portal> portals,
                       CombatDemo demo, List<MaterialDrop> drops) {
        for (Portal portal : portals) {
            if (!portal.sourceMapId().equals(map.getId())) { continue; }
            Gate gate = gates.computeIfAbsent(portal.id(), ignored -> new Gate(portal.enabled()));
            if (gate.enabled != portal.enabled()) { gate.enabled = portal.enabled(); gate.age = 0; }
            String state = gate.age < 0.6 ? gate.enabled ? "activate" : "close" : gate.enabled ? "loop" : "idle";
            art.draw(batch, "art/portal/portal_" + state + ".png", portal.trigger(), gate.age, gate.age >= 0.6, false);
        }
        if (npcs != null) {
            for (NPC npc : npcs) {
                String image = switch (npc.getId()) {
                    case "notice_board" -> "props/quest_board";
                    case "storage_chest" -> "props/chest";
                    case "class_master" -> "npc/class_master";
                    case "weapon_merchant" -> "npc/blacksmith";
                    default -> "npc/quest_giver";
                };
                draw(batch, "art/hub/" + image + ".png",
                        visualBody(new CollisionBox(npc.getX(),npc.getY(),npc.getWidth(),npc.getHeight()), 42, 54));
            }
        }
        art.draw(batch, playerStrip, visualBody(player.getBounds(), 42, 54), playerAge, true,
                demo.getAbilities().getFacingX() < 0);
        playerEffects(batch, player, demo.getAbilities());
        for (Combatant actor : demo.getCombatants()) {
            if (actor.getFaction() == Faction.ENEMY) {
                String theme = lower(demo.getTrainingAppearance().map(TrainingAppearance::guardian)
                        .orElseGet(() -> BossChallenge.forMap(map.getId()).orElse(BossChallenge.FIRE)));
                art.draw(batch, "art/boss/" + theme + "/" + bossStrip,
                        visualBody(actor.getHurtbox(), 92, 92), bossAge, true, false);
            }
            statuses(batch, actor, demo.getAttachedElement(actor));
        }
        for (Attack attack : demo.getActiveAttacks()) {
            if (!attack.isActive()) { continue; }
            if (attack.getProjectile() != null) { projectile(batch, attack); }
            else if (!attack.isDerived()) { areaAttack(batch, attack); }
        }
        for (MaterialDrop drop : drops) {
            draw(batch, "art/items/boss_materials/" + arenaTheme(map, demo) + "_material.png", drop.bounds());
        }
        for (Effect effect : effects) {
            art.draw(batch, effect.path, effect.bounds, effect.age, false, false);
        }
        demo.getBoss().filter(boss -> boss.getMeleeWarning().isPresent()
                && warningVisible(boss.getWindupProgress())).ifPresent(boss -> {
            CollisionBox body = boss.getBounds();
            draw(batch, BOSS_WARNING_IMAGE,
                    new CollisionBox(body.x() + body.width() / 2 - 24, body.top() + 20, 48, 48));
        });
    }

    /**
     * Flashes using simulation progress so pauses do not advance the warning. The phase derivative
     * rises continuously from two to ten cycles per complete windup, without frame-rate dependence.
     * @param progress elapsed windup fraction, clamped to the visible window
     * @return whether the exclamation should be drawn in this frame
     */
    static boolean warningVisible(double progress) {
        double t = Math.max(0, Math.min(1, progress));
        double cycles = 2 * t + 4 * t * t;
        return cycles - Math.floor(cycles) < 0.5;
    }

    /**
     * Selects a player strip from accepted actions and existing skill phases.
     * @param player player state
     * @param abilities common controller
     * @return existing horizontal strip path
     */
    private String playerAnimation(Player player, PlayerAbilities abilities) {
        String root = "art/player/" + player.getPlayerClass().getId() + "/";
        String direction = abilities.getFacingY() > 0 ? "up" : abilities.getFacingY() < 0 ? "down" : "side";
        if (!player.isAlive()) { return root + "death/death_8f.png"; }
        if (playerHurt > 0) { return root + "hit/hit_3f.png"; }
        if (followRemaining > 0) { return root + followThrough; }
        if (abilities.getState(AbilitySlot.DASH) == AbilityState.ACTIVE) { return root + "l_dash/dash_" + direction + "_4f.png"; }
        String skill = switch (player.getPlayerClass()) {
            case ARCHER -> phase.equals("BUFF") && phaseAge < 0.6 ? "o_untargetable/untargetable_cast_6f.png"
                    : phase.equals("FIELD") && phaseAge < 0.8 ? "o_arrow_rain/arrow_rain_cast_8f.png" : "";
            case MAGE -> switch (phase) {
                case "CAST" -> "o_magic_circle/casting/casting_10f.png";
                case "FIELD" -> phaseAge < 0.4 ? "o_magic_circle/complete/cast_complete_4f.png" : "";
                case "BUFF" -> phaseAge < 0.6 ? "o_rapid_barrage/activate/activate_6f.png" : "o_rapid_barrage/buff_loop/buff_loop_4f.png";
                default -> "";
            };
            case KATANA -> switch (phase) {
                case "PARRY" -> "k_parry/start/parry_start_4f.png";
                case "RECOVERY" -> "k_parry/recovery/parry_recovery_5f.png";
                case "COUNTER_DASH" -> phaseAge < 0.1 ? "k_parry/success/parry_success_4f.png" : "k_parry/counter_dash/counter_dash_6f.png";
                case "STANCE" -> "o_watermoon/stance/stance_6f.png";
                case "THRUST" -> "o_helmbreaker/approach/approach_4f.png";
                case "FINISHER" -> phaseAge < 0.12 ? "o_helmbreaker/launch/launch_4f.png" : "o_helmbreaker/multislash/multislash_6f.png";
                default -> "";
            };
        };
        if (launchRemaining > 0 && launch == AbilitySlot.LIGHT) {
            return root + switch (player.getPlayerClass()) {
                case ARCHER -> "j_light_attack/light_attack_" + direction + "_6f.png";
                case MAGE -> "j_magic_bolt/magic_bolt_" + direction + "_6f.png";
                case KATANA -> "j_slash/slash_" + direction + "_5f.png";
            };
        }
        if (!skill.isEmpty()) { return root + skill; }
        if (launchRemaining > 0 && launch == AbilitySlot.SPECIAL) {
            if (player.getPlayerClass() == PlayerClass.ARCHER) { return root + "k_spread_shot/spread_shot_" + direction + "_8f.png"; }
            if (player.getPlayerClass() == PlayerClass.MAGE) { return root + "k_element_switch/element_switch_4f.png"; }
        }
        return root + (moving ? "walk/walk_" + direction + "_6f.png" : "idle/idle_" + direction + "_4f.png");
    }

    /**
     * Uses the current Boss state and shared attack IDs only for selecting pictures.
     * @param demo encounter observation
     * @return Boss-relative strip path
     */
    private static String bossAnimation(CombatDemo demo) {
        if (demo.getBoss().isEmpty()) { return "idle/idle_6f.png"; }
        var boss = demo.getBoss().orElseThrow();
        if (!boss.isAlive()) { return "death/death_10f.png"; }
        if (boss.getState() == BossState.PHASE_TRANSITION) { return "phase_change/phase_change_10f.png"; }
        for (Attack attack : demo.getActiveAttacks()) {
            if (attack.getSource() != boss.getCombatant() || !attack.isActive()) { continue; }
            String type = attack.getSpecification().type();
            return type.contains("heavy") ? "attack_03/attack_03_12f.png"
                    : type.contains("slam") ? "attack_02/attack_02_10f.png" : "attack_01/attack_01_8f.png";
        }
        return boss.getState() == BossState.CHASE ? "move/move_6f.png" : "idle/idle_6f.png";
    }

    /**
     * Draws passive visual status overlays from generic combat state.
     * @param batch active batch
     * @param actor current adapter
     * @param element current attached element
     */
    private void statuses(SpriteBatch batch, Combatant actor, ElementType element) {
        CollisionBox box = expanded(actor.getHurtbox(), 10);
        if (element != ElementType.NONE) { draw(batch, "art/elements/" + lower(element) + "/aura.png", box); }
        CombatStatus status = actor.getStatus();
        if (status.isFrozen()) { draw(batch, "art/status/frozen.png", box); }
        if (status.getChargedRemaining() > 0) { draw(batch, "art/status/electro_charged.png", box); }
        if (status.getResistanceRemaining() > 0) { draw(batch, "art/status/superconduct_debuff.png", box); }
        if (!actor.isTargetable() && actor.getEntity().isAlive()) { draw(batch, "art/status/untargetable.png", box); }
        String icon = status.isFrozen() ? "frozen" : status.getChargedRemaining() > 0 ? "electro_charged"
                : status.getResistanceRemaining() > 0 ? "superconduct" : !actor.isTargetable() ? "untargetable" : "";
        if (!icon.isEmpty()) {
            draw(batch,"art/ui/status_icons/"+icon+".png",new CollisionBox(box.x(),box.top()+20,16,16));
        }
    }

    /**
     * Draws class-specific visual overlays, leaving generic combat unaware of classes.
     * Archer's right-facing spread artwork mirrors horizontally with the player's facing,
     * using draw geometry rather than mutating a cached texture region.
     * @param batch active batch
     * @param player persistent player
     * @param abilities shared controller
     */
    private void playerEffects(SpriteBatch batch, Player player, PlayerAbilities abilities) {
        CollisionBox box = expanded(player.getBounds(), 20);
        String path = switch (phase) {
            case "PARRY" -> "art/status/parry_ready.png";
            case "STANCE" -> "art/skills/katana/watermoon_stance.png";
            case "COUNTER_DASH" -> "art/skills/katana/counter_dash_effect.png";
            case "BUFF" -> player.getPlayerClass() == PlayerClass.ARCHER ? "art/skills/archer/untargetable_effect.png"
                    : "art/skills/mage/rapid_barrage_" + (phaseAge < 0.5 ? "start" : "loop") + ".png";
            default -> "";
        };
        if (!path.isEmpty()) { art.draw(batch, path, box, phaseAge, true, false); }
        if (phase.equals("CAST")) {
            draw(batch,"art/ui/status_icons/casting.png",new CollisionBox(box.x(),box.top()+4,16,16));
        }
        if (phase.equals("BUFF") && player.getPlayerClass() == PlayerClass.MAGE) { draw(batch, "art/status/rapid_cast.png", box); }
        if (abilities.getState(AbilitySlot.DASH) == AbilityState.ACTIVE) { draw(batch, "art/common_vfx/dash_trail.png", box); }
        if (launchRemaining > 0 && launch == AbilitySlot.SPECIAL && player.getPlayerClass() == PlayerClass.ARCHER) {
            art.draw(batch, "art/skills/archer/spread_shot_effect.png", box, time, true,
                    abilities.getFacingX() < 0);
        }
    }

    /**
     * Rotates a visual arrow/bolt to the shared flight direction without changing its hitbox.
     * @param batch active batch
     * @param attack existing projectile invocation
     */
    private void projectile(SpriteBatch batch, Attack attack) {
        ElementType element = attack.getSpecification().element();
        String path = attack.getSource().getFaction() == Faction.ENEMY ? "art/projectiles/boss/"
                + (element == ElementType.NONE ? "fire" : lower(element)) + "_bolt.png"
                : element == ElementType.NONE ? "art/projectiles/archer/arrow.png"
                : "art/projectiles/mage/" + lower(element) + "_bolt.png";
        var frame = art.frame(path, time, true);
        CollisionBox box = attack.getHitbox();
        float width = attack.getSource().getFaction() == Faction.ENEMY ? MAGE_BOLT_WIDTH * BOSS_BOLT_SCALE
                : element == ElementType.NONE ? 28 : MAGE_BOLT_WIDTH;
        float height = width * frame.getRegionHeight() / frame.getRegionWidth();
        float angle = (float) Math.toDegrees(Math.atan2(attack.getProjectile().getDirectionY(), attack.getProjectile().getDirectionX()));
        batch.draw(frame, (float)(box.x()+box.width()/2)-width/2, (float)(box.y()+box.height()/2)-height/2,
                width/2,height/2,width,height,1,1,angle);
    }

    /**
     * Presents finite melee/Boss geometry; persistent fields are drawn once between their ticks.
     * @param batch active batch
     * @param attack active shared invocation
     */
    private void areaAttack(SpriteBatch batch, Attack attack) {
        String type = attack.getSpecification().type();
        String path = type.startsWith("katana") ? type.contains("stance") ? "art/skills/katana/watermoon_counter.png"
                : type.contains("dragon") ? "art/skills/katana/helmbreaker_slash.png" : "art/skills/katana/slash_effect.png"
                : type.startsWith("boss") ? "art/common_vfx/physical_hit.png" : "";
        if (!path.isEmpty()) { draw(batch, path, attack.getHitbox()); }
    }

    /**
     * Draws capped, hideable class/ability/element icons and live bag potion quantity.
     * @param batch active screen-space batch
     * @param font existing HUD font
     * @param shapes idle geometry renderer for the procedural potion icon
     * @param player current player
     * @param abilities common cooldowns
     * @param hud current responsive policy
     * @param windowWidth window width
     */
    public void hud(SpriteBatch batch, BitmapFont font, ShapeRenderer shapes, Player player, PlayerAbilities abilities, HudLayout hud,
                    int windowWidth) {
        if (!hud.detailsVisible()) { return; }
        double size = hud.skillIconSize();
        boolean elementVisible = player.getPlayerClass() == PlayerClass.MAGE;
        double x = hud.skillSlotLeft(windowWidth, 0, elementVisible);
        String cls = player.getPlayerClass().getId();
        draw(batch, "art/ui/class_icons/" + cls + ".png", new CollisionBox(x, 8, size, size));
        String mode = switch (player.getPlayerClass()) {
            case ARCHER -> player.getUltimateMode() == UltimateMode.A ? "arrow_rain" : "untargetable";
            case MAGE -> player.getUltimateMode() == UltimateMode.A ? "magic_circle" : "barrage";
            case KATANA -> player.getUltimateMode() == UltimateMode.A ? "watermoon" : "helmbreaker";
        };
        String[] keys = {"j", "k", "l", "o_" + mode};
        AbilitySlot[] slots = {AbilitySlot.LIGHT,AbilitySlot.SPECIAL,AbilitySlot.DASH,AbilitySlot.ULTIMATE};
        for (int i = 0; i < keys.length; i++) {
            double left = hud.skillSlotLeft(windowWidth, i + 1, elementVisible);
            boolean ready = abilities.getState(slots[i]) == AbilityState.READY;
            batch.setColor(ready ? Color.WHITE : Color.GRAY);
            draw(batch, "art/ui/ability_icons/" + cls + "_" + keys[i] + ".png",new CollisionBox(left,8,size,size));
            batch.setColor(Color.WHITE);
            font.getData().setScale(0.65f * hud.skillScale()); font.setColor(Color.WHITE);
            String label = "JKLO".substring(i,i+1);
            double cooldown = abilities.getCooldownRemaining(slots[i]);
            font.draw(batch, cooldown > 0 ? label + " " + (int)Math.ceil(cooldown) : label, (float)left, (float)(size+22));
        }
        if (elementVisible) {
            draw(batch,"art/ui/element_icons/"+lower(player.getMageElement())+".png",
                    new CollisionBox(hud.skillSlotLeft(windowWidth, 5, true),8,size,size));
        }
        drawPotionSlot(batch, font, shapes, hud.skillSlotLeft(windowWidth, elementVisible ? 6 : 5, elementVisible),
                hud.skillIconSize(), hud.skillScale(), player.getInventory().quantityOf(ItemCatalog.POTION));
    }

    /**
     * Draws a pixel-style bottle and compact remaining count without loading another art resource.
     * Reads the bag each frame; presentation never consumes a potion or changes equipment.
     * Temporarily suspends the active batch for geometry and restores the borrowed render state.
     * @param batch active screen-space batch, still active on return
     * @param font reusable font
     * @param shapes idle reusable geometry renderer
     * @param x slot left edge
     * @param size square icon size
     * @param scale skill bar scale for this window
     * @param quantity current bag count, excluding chest storage
     */
    private void drawPotionSlot(SpriteBatch batch, BitmapFont font, ShapeRenderer shapes,
                                float x, float size, float scale, int quantity) {
        var projection = shapes.getProjectionMatrix().cpy();
        Color shapeColor = new Color(shapes.getColor());
        batch.end();
        shapes.setProjectionMatrix(batch.getProjectionMatrix());
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(quantity > 0 ? Color.TEAL : Color.DARK_GRAY);
        shapes.rect(x, 8, size, size);
        shapes.setColor(0.025f, 0.055f, 0.085f, 1);
        shapes.rect(x + scale, 8 + scale, size - 2 * scale, size - 2 * scale);
        // Bottle body, neck and stopper occupy the upper part; the lower band is reserved for the count.
        shapes.setColor(quantity > 0 ? Color.LIGHT_GRAY : Color.GRAY);
        shapes.rect(x + size * 0.28f, 8 + size * 0.32f, size * 0.44f, size * 0.4f);
        shapes.rect(x + size * 0.4f, 8 + size * 0.7f, size * 0.2f, size * 0.16f);
        shapes.setColor(quantity > 0 ? Color.SCARLET : Color.DARK_GRAY);
        shapes.rect(x + size * 0.32f, 8 + size * 0.36f, size * 0.36f, size * 0.23f);
        shapes.setColor(quantity > 0 ? Color.TAN : Color.GRAY);
        shapes.rect(x + size * 0.37f, 8 + size * 0.84f, size * 0.26f, size * 0.08f);
        shapes.end();
        shapes.setProjectionMatrix(projection);
        shapes.setColor(shapeColor);
        batch.begin();
        float oldX = font.getData().scaleX, oldY = font.getData().scaleY;
        Color oldColor = new Color(font.getColor());
        font.getData().setScale(0.65f * scale);
        font.setColor(quantity > 0 ? Color.WHITE : Color.GRAY);
        font.draw(batch, "P", x, size + 22);
        String label = HudLayout.potionCountLabel(quantity);
        font.getData().setScale(0.6f * scale);
        potionCountText.setText(font, label);
        font.draw(batch, label, x + (size - potionCountText.width) / 2, 8 + size * 0.32f);
        font.getData().setScale(oldX, oldY);
        font.setColor(oldColor);
    }

    /**
     * Advances return feedback independently of paused gameplay, dialogue and hidden skill icons.
     * Expiry discards only presentation snapshots, never gameplay or reward state.
     * @param delta finite non-negative elapsed UI seconds
     * @throws IllegalArgumentException if time is negative or non-finite
     */
    void advanceOutcome(double delta) {
        if (!Double.isFinite(delta) || delta < 0) { throw new IllegalArgumentException("Invalid UI time"); }
        if (outcome == null) { return; }
        outcomeAge = Math.min(OUTCOME_SECONDS, outcomeAge + delta);
        if (outcomeAge >= OUTCOME_SECONDS) { outcome = null; outcomeClass = null; }
    }

    /**
     * Reports whether a result is still in its three-second display window.
     * @return true before expiry, independently of persistent HUD visibility
     */
    boolean hasOutcome() { return outcome != null; }

    /**
     * Fits the result panel to either window axis, capping growth at the reference window size.
     * @param width logical window width
     * @param height logical window height
     * @return uniform scale, zero for a minimized window
     */
    static float outcomeScale(int width, int height) {
        return Math.max(0, Math.min(1, Math.min(width / (float) GameConfig.HUD_REFERENCE_WIDTH,
                height / (float) GameConfig.HUD_REFERENCE_HEIGHT)));
    }

    /**
     * Draws a large, high-contrast return banner above other UI without capturing input.
     * Uses existing screen-owned geometry and font resources; both batches must initially be idle.
     * @param batch screen-space batch, with projection already set
     * @param font shared UI font, restored after drawing
     * @param shapes existing screen geometry renderer
     * @param width logical window width
     * @param height logical window height
     */
    public void drawOutcome(SpriteBatch batch, BitmapFont font, ShapeRenderer shapes, int width, int height) {
        float scale = outcomeScale(width, height);
        if (!hasOutcome() || map == null || map.getType() != MapType.HUB || scale == 0) { return; }
        boolean victory = outcome.outcome() == BattleResult.Outcome.SUCCESS;
        Color accent = victory ? Color.GOLD : Color.SALMON;
        float panelWidth = 600 * scale, panelHeight = 180 * scale;
        float left = (width - panelWidth) / 2, bottom = height * 0.62f - panelHeight / 2;
        float edge = 3 * scale;
        shapes.setProjectionMatrix(batch.getProjectionMatrix());
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(accent);
        shapes.rect(left, bottom, panelWidth, panelHeight);
        shapes.setColor(0.025f, 0.035f, 0.055f, 1);
        shapes.rect(left + edge, bottom + edge, panelWidth - edge * 2, panelHeight - edge * 2);
        shapes.end();
        float oldX = font.getData().scaleX, oldY = font.getData().scaleY;
        Color oldFontColor = new Color(font.getColor()), oldBatchColor = new Color(batch.getColor());
        batch.begin();
        try {
            batch.setColor(Color.WHITE);
            String theme = lower(BossChallenge.forMap(outcome.encounter().mapId()).orElse(BossChallenge.FIRE));
            CollisionBox portrait = new CollisionBox(left + 16 * scale, bottom + 28 * scale, 120 * scale, 120 * scale);
            art.draw(batch, victory ? "art/boss/" + theme + "/death/death_10f.png"
                    : "art/player/" + outcomeClass.getId() + "/death/death_8f.png", portrait, outcomeAge, false, false);
            art.draw(batch, victory ? "art/common_vfx/boss_defeat.png" : "art/common_vfx/disappear.png",
                    portrait, Math.max(0, outcomeAge - 0.5), false, false);
            float textX = left + 152 * scale;
            font.setColor(accent);
            font.getData().setScale(2.6f * scale);
            font.draw(batch, victory ? "BOSS DEFEATED" : "CHALLENGE FAILED", textX, bottom + 132 * scale);
            font.setColor(Color.WHITE);
            font.getData().setScale(1.15f * scale);
            font.draw(batch, "Returned to Hub - HP restored", textX, bottom + 79 * scale);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, victory ? "Victory!" : "You can challenge the Boss again.", textX, bottom + 48 * scale);
        } finally {
            font.getData().setScale(oldX, oldY);
            font.setColor(oldFontColor);
            batch.setColor(oldBatchColor);
            batch.end();
        }
    }

    /**
     * Adds disposable feedback without retaining combatants or attacks.
     * @param path existing PNG
     * @param bounds captured world box
     * @param duration finite visual duration
     */
    private void effect(String path, CollisionBox bounds, double duration) {
        if (effects.size() == MAX_EFFECTS) { effects.remove(0); }
        effects.add(new Effect(path,bounds,duration));
    }

    /**
     * Draws a repeating/static visual at the global presentation clock.
     * @param batch active batch
     * @param path asset path
     * @param box visual bounds
     */
    void draw(SpriteBatch batch, String path, CollisionBox box) { art.draw(batch,path,box,time,true,false); }

    /**
     * Keeps visual feet at the collision body's bottom while centering wider artwork.
     * @param body collision bounds to observe
     * @param width visual width
     * @param height visual height
     * @return independent visual rectangle
     */
    private static CollisionBox visualBody(CollisionBox body, double width, double height) {
        return new CollisionBox(body.x()+(body.width()-width)/2,body.y(),width,height);
    }

    /**
     * Expands only presentation geometry.
     * @param box original bounds
     * @param margin amount on each side
     * @return expanded rectangle
     */
    private static CollisionBox expanded(CollisionBox box, double margin) {
        return new CollisionBox(box.x()-margin,box.y()-margin,box.width()+2*margin,box.height()+2*margin);
    }

    /**
     * Uses locale-independent catalogue names at the art boundary only.
     * @param value enum identity
     * @return lowercase enum name
     */
    private static String lower(Enum<?> value) { return value.name().toLowerCase(Locale.ROOT); }

    /** Disposes cached graphics and all retained presentation-only snapshots. */
    @Override public void dispose() { art.dispose(); effects.clear(); gates.clear(); }
}
