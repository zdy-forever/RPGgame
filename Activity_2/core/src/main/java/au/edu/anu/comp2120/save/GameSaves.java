package au.edu.anu.comp2120.save;

import au.edu.anu.comp2120.*;
import au.edu.anu.comp2120.combat.ElementType;
import au.edu.anu.comp2120.enemy.BossChallenge;
import au.edu.anu.comp2120.inventory.*;
import au.edu.anu.comp2120.npc.NPC;
import au.edu.anu.comp2120.player.*;
import au.edu.anu.comp2120.quest.BossQuests;
import au.edu.anu.comp2120.screen.CombatDemo;
import au.edu.anu.comp2120.world.*;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

/** Game-loop coordinator for independent save slots and complete validated restoration.
 * No live object is changed until every file field and cross-module reference is checked.
 * @author Hao Liu (u7998420)
 * @author Jiaqi Chen (u7851266)
 */
public final class GameSaves {
    /** Sydney display time with seasonal zone label; persisted timestamps remain UTC. */
    private static final java.time.format.DateTimeFormatter DISPLAY_TIME =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", java.util.Locale.ENGLISH)
                    .withZone(java.time.ZoneId.of("Australia/Sydney"));

    /** Persistent map and player owner. */
    private final GameSession session;
    /** Route owner. */
    private final TravelSystem travel;
    /** Reward and win-count owner. */
    private final EncounterFlow flow;
    /** Quest owner. */
    private final BossQuests quests;
    /** Combat scope and timer owner. */
    private final CombatDemo combat;
    /** Separate chest storage. */
    private final Inventory chest;
    /** Stable NPC catalogue including non-current maps. */
    private final List<NPC> npcs;
    /** Independent disk slots. */
    private final SaveStore store;
    /** Last settled result inspected; never restored from disk. */
    private BattleResult observed;
    /** Visible feedback including recoverable IO errors. */
    private String status = "F5: manual save | F9: load saves (Hub / Memorial)";

    /** Monotonic operation event number; repeated identical messages remain new notifications. */
    private long statusVersion;

    /** Binds one coordinator to a game session.
     * @param session map/player owner
     * @param travel route owner
     * @param flow result owner
     * @param quests quest owner
     * @param combat combat scope
     * @param chest chest storage
     * @param npcs full NPC catalogue
     * @param store disk storage
     */
    public GameSaves(GameSession session, TravelSystem travel, EncounterFlow flow, BossQuests quests,
            CombatDemo combat, Inventory chest, List<NPC> npcs, SaveStore store) {
        this.session = session; this.travel = travel; this.flow = flow; this.quests = quests;
        this.combat = combat; this.chest = chest; this.npcs = List.copyOf(npcs); this.store = store;
    }

    /** Checks world safety independently from UI modal state.
     * @return true in a living safe map with no executing action or projectile
     */
    public boolean isSafe() {
        var type = session.getCurrentMap().getType();
        return (type == MapType.HUB || type == MapType.MEMORIAL)
                && session.getPlayer().isAlive() && combat.getAbilities().isIdleForSave();
    }
    /** Reads the latest operation message.
     * @return displayable status
     */
    public String status() { return status; }
    /** Returns the last operation event number.
     * @return monotonically increasing notification sequence
     */
    public long statusVersion() { return statusVersion; }
    /** Publishes feedback once per attempted operation.
     * @param message result for the presentation layer
     */
    private void report(String message) { status = message; statusVersion++; }

    /** Checks whether overwriting a manual file needs confirmation, including corrupt files.
     * @return whether a manual file exists
     */
    public boolean hasManual() { return Files.exists(store.path(SaveSlot.MANUAL)); }

    /** Writes only the manual slot after the screen obtains any required confirmation.
     * @return whether writing succeeded
     */
    public boolean saveManual() { return save(SaveSlot.MANUAL); }

    /** Observes a fully settled victory once, after reward collection and quest synchronization.
     * Failed saves report an error without undoing the victory or overwriting the manual slot.
     */
    public void afterSettlement() {
        BattleResult result = flow.getLastResult().orElse(null);
        if (result == null || result.equals(observed)) { return; }
        observed = result;
        if (result.outcome() == BattleResult.Outcome.SUCCESS) { save(SaveSlot.AUTO); }
    }

    /** Reads and validates one slot for a selection-menu summary without changing game or disk.
     * @param slot selected slot
     * @return timestamp, map and distinct guardian count, or an unavailable message
     */
    public String summary(SaveSlot slot) {
        try {
            var fields = store.load(slot);
            prepare(fields, slot);
            var meta = SaveMetadata.fromFields(fields, slot);
            var location = LocationSnapshot.fromFields(fields);
            int wins = 0;
            for (var boss : BossChallenge.values()) {
                if (Boolean.parseBoolean(fields.get("quest.done." + boss.name()))) { wins++; }
            }
            return slot + ": " + DISPLAY_TIME.format(meta.savedAt()) + " | " + location.mapId() + " | " + wins + "/5 guardians";
        } catch (IOException | IllegalArgumentException error) {
            return slot + ": unavailable (" + error.getMessage() + ")";
        }
    }

    /** Removes one slot without changing the live session or the other slot.
     * @param slot slot explicitly confirmed by the player
     * @return whether deletion completed, including an already empty slot
     */
    public boolean deleteSlot(SaveSlot slot) {
        if (!isSafe()) { report("Delete blocked: use Hub / Memorial after active skills finish."); return false; }
        try {
            boolean removed = store.delete(slot);
            report(removed ? "Deleted " + slot + ". Current game and other save unchanged."
                    : slot + " is already empty. Current game unchanged.");
            return true;
        } catch (IOException error) {
            report("Delete failed: " + error.getMessage());
            return false;
        }
    }

    /** Loads exactly the chosen slot after UI confirmation; never saves or merges timelines.
     * @param slot selected source slot
     * @return whether a complete validated snapshot was restored
     */
    public boolean load(SaveSlot slot) {
        if (!isSafe()) { report("Load blocked: leave combat and finish active skills first."); return false; }
        try {
            Runnable restore = prepare(store.load(slot), slot);
            restore.run();
            observed = null;
            report("Loaded " + slot + ". Neither save slot was changed.");
            return true;
        } catch (IOException | IllegalArgumentException error) {
            report("Load failed: " + error.getMessage());
            return false;
        }
    }

    /** Writes a detached fully validated snapshot to one file.
     * @param slot destination
     * @return write success
     */
    private boolean save(SaveSlot slot) {
        if (session.getPlayer().getDebugOptions().blocksSaving()) {
            report("Save blocked: debug used in this game. Start a new game/restart to save again."); return false;
        }
        if (!isSafe()) { report("Save blocked: use Hub / Memorial after active skills finish."); return false; }
        try {
            var fields = capture(slot);
            prepare(fields, slot);
            store.save(slot, fields);
            report(slot == SaveSlot.AUTO ? "Boss defeated - automatic save complete." : "Manual save complete.");
            return true;
        } catch (IOException | IllegalArgumentException error) {
            report("Save failed; previous file retained: " + error.getMessage());
            return false;
        }
    }

    /** Captures every persistent gameplay component on the game-loop thread.
     * @param slot originating slot
     * @return detached fields
     */
    private Map<String, String> capture(SaveSlot slot) {
        Map<String, String> f = new LinkedHashMap<>();
        f.putAll(new SaveMetadata(slot, Instant.now()).toFields());
        f.putAll(LocationSnapshot.capture(session).toFields());
        f.putAll(InventorySnapshot.capture(session.getPlayer(), chest).toFields());
        var player = session.getPlayer();
        f.put("player.class", player.getPlayerClass().name());
        f.put("player.element", player.getMageElement().name());
        player.savedModes().forEach((type, mode) -> f.put("player.mode." + type.name(), mode.name()));
        for (var ability : AbilitySlot.values()) {
            f.put("player.cooldown." + ability.name(), Double.toString(combat.getAbilities().getCooldownRemaining(ability)));
        }
        for (var boss : BossChallenge.values()) {
            f.put("quest.done." + boss.name(), Boolean.toString(quests.getCompleted().contains(boss)));
            f.put("quest.wins." + boss.name(), Integer.toString(flow.getSuccessCount(boss.mapId())));
        }
        f.put("quest.active", quests.getActive().map(Enum::name).orElse(""));
        f.put("quest.ending", Boolean.toString(quests.hasEndingStarted()));
        f.put("quest.memorialRevealed", Boolean.toString(quests.isMemorialPathRevealed()));
        var permission = flow.getActiveEncounter().orElse(null);
        f.put("quest.permission", Boolean.toString(permission != null));
        if (permission != null) { encodePermission(f, "quest.permission.", permission); }
        var completed = flow.getCompletedEncounterIds().stream().sorted().toList();
        f.put("encounter.count", Integer.toString(completed.size()));
        for (int i = 0; i < completed.size(); i++) { f.put("encounter." + i, completed.get(i)); }
        var rewards = flow.getPendingRewards();
        f.put("reward.count", Integer.toString(rewards.size()));
        int index = 0;
        for (var entry : rewards.entrySet()) {
            String p = "reward." + index++ + ".";
            encodePermission(f, p, entry.getKey().encounter());
            f.put(p + "version", Long.toString(entry.getKey().encounterVersion()));
            var drop = entry.getValue();
            f.put(p + "drop", drop.id()); f.put(p + "material", drop.materialId());
            f.put(p + "quantity", Integer.toString(drop.quantity()));
            f.put(p + "x", Double.toString(drop.bounds().x())); f.put(p + "y", Double.toString(drop.bounds().y()));
            f.put(p + "width", Double.toString(drop.bounds().width())); f.put(p + "height", Double.toString(drop.bounds().height()));
        }
        for (var route : routes()) {
            String p = "portal." + route.id() + ".";
            f.put(p + "map", route.destinationMapId()); f.put(p + "spawn", route.destinationSpawnId());
            f.put(p + "enabled", Boolean.toString(route.enabled()));
        }
        travel.getDestinations().forEach(d -> f.put("travel." + d.id(), Boolean.toString(d.unlocked())));
        npcs.forEach(npc -> f.put("npc." + npc.getId(), Boolean.toString(npc.hasInteracted())));
        return Map.copyOf(f);
    }

    /** Enumerates catalogue routes by registered source map.
     * @return all routes
     */
    private List<Portal> routes() {
        return MapCatalog.createMaps().stream().flatMap(map -> travel.getPortals(map.getId()).stream()).toList();
    }

    /** Encodes admission identity without storing live entities.
     * @param f destination fields
     * @param p key prefix
     * @param permission accepted challenge
     */
    private static void encodePermission(Map<String, String> f, String p, ActiveEncounter permission) {
        f.put(p + "id", permission.id()); f.put(p + "map", permission.mapId());
        f.put(p + "spawn", permission.spawnId()); f.put(p + "portal", permission.portalId());
    }

    /** Decodes and checks catalogue-backed battle permission without mutating routes.
     * @param f strict reader
     * @param p key prefix
     * @return valid battle permission
     * @throws IllegalArgumentException if fields, challenge identity, portal or spawn are invalid
     */
    private ActiveEncounter permission(SaveFields f, String p) {
        var value = new ActiveEncounter(f.text(p + "id"), f.text(p + "map"), f.text(p + "spawn"), f.text(p + "portal"));
        if (BossChallenge.forMap(value.mapId()).isEmpty() || !value.portalId().equals(TravelCatalog.QUEST_PORTAL_ID)) {
            throw new IllegalArgumentException("Invalid Boss permission");
        }
        validateSpawn(value.mapId(), value.spawnId());
        return value;
    }

    /** Checks an entire body at a named spawn without entering it.
     * @param mapId registered map
     * @param spawnId registered spawn
     * @throws NullPointerException if either identity is null
     * @throws IllegalArgumentException if the map or spawn is missing or the full body cannot fit
     */
    private void validateSpawn(String mapId, String spawnId) {
        var map = session.getMap(mapId);
        var point = map.getSpawnPoint(spawnId);
        var body = session.getPlayer().getBounds();
        if (!map.isWalkable(new CollisionBox(point.x(), point.y(), body.width(), body.height()))) {
            throw new IllegalArgumentException("Blocked saved portal destination");
        }
    }

    /** Fully validates and stages restoration; the returned operation contains no IO or parsing.
     * @param fields decoded snapshot
     * @param slot selected slot
     * @return game-loop restoration operation, to run only after validation succeeds
     * @throws NullPointerException if fields or slot is null
     * @throws IllegalArgumentException if fields are malformed, unknown or inconsistent with the session
     */
    private Runnable prepare(Map<String, String> fields, SaveSlot slot) {
        var f = new SaveFields(fields);
        var metadata = SaveMetadata.fromFields(fields, slot); f.accept(metadata.toFields().keySet());
        var location = LocationSnapshot.fromFields(fields); f.accept(location.toFields().keySet());
        var inventory = InventorySnapshot.fromFields(fields); f.accept(inventory.toFields().keySet());
        int capacity = (int) Math.min(Integer.MAX_VALUE, (long) session.getPlayer().getBaseMaxHp()
                + inventory.worn().values().stream().map(id -> au.edu.anu.comp2120.inventory.ItemCatalog.gear(id).orElseThrow())
                .mapToInt(au.edu.anu.comp2120.inventory.ItemCatalog.Gear::healthBonus).sum());
        // Pre-attribute saves recorded base HP even with a hood; preserve their actual HP.
        var restoredLocation = location.maxHp() == session.getPlayer().getBaseMaxHp()
                ? new LocationSnapshot(location.mapId(), location.x(), location.y(), location.hp(), capacity) : location;
        restoredLocation.validateFor(session, capacity);
        var type = PlayerClass.valueOf(f.text("player.class"));
        var element = ElementType.valueOf(f.text("player.element"));
        if (element == ElementType.NONE) { throw new IllegalArgumentException("Invalid Mage element"); }
        Map<PlayerClass, UltimateMode> modes = new EnumMap<>(PlayerClass.class);
        for (var c : PlayerClass.values()) { modes.put(c, UltimateMode.valueOf(f.text("player.mode." + c.name()))); }
        Map<AbilitySlot, Double> timers = new EnumMap<>(AbilitySlot.class);
        for (var a : AbilitySlot.values()) { timers.put(a, f.number("player.cooldown." + a.name())); }
        Set<BossChallenge> done = EnumSet.noneOf(BossChallenge.class);
        Map<String, Integer> wins = new LinkedHashMap<>();
        for (var boss : BossChallenge.values()) {
            boolean completed = f.flag("quest.done." + boss.name());
            int count = f.count("quest.wins." + boss.name(), Integer.MAX_VALUE);
            if (completed != (count > 0)) { throw new IllegalArgumentException("Quest/win count mismatch"); }
            if (completed) { done.add(boss); }
            wins.put(boss.mapId(), count);
        }
        String activeName = f.text("quest.active");
        BossChallenge active = activeName.isEmpty() ? null : BossChallenge.valueOf(activeName);
        boolean ending = f.flag("quest.ending");
        String memorialKey = "quest.memorialRevealed";
        String memorialValue = fields.get(memorialKey);
        boolean memorialRevealed = "true".equalsIgnoreCase(memorialValue);
        if (memorialValue != null) { f.accept(Set.of(memorialKey)); }
        if (ending && done.size() != BossChallenge.values().length) { throw new IllegalArgumentException("Invalid ending progress"); }
        ActiveEncounter permission = f.flag("quest.permission") ? permission(f, "quest.permission.") : null;
        if ((active == null) != (permission == null)
                || (active != null && !active.mapId().equals(permission.mapId()))) {
            throw new IllegalArgumentException("Quest permission mismatch");
        }
        Set<String> completedIds = new HashSet<>();
        int completedSize = f.count("encounter.count", 100000);
        for (int i = 0; i < completedSize; i++) {
            String id = f.text("encounter." + i);
            if (id.isBlank() || !completedIds.add(id) || (permission != null && permission.id().equals(id))) {
                throw new IllegalArgumentException("Invalid completed encounter identity");
            }
        }
        Map<BattleResult, MaterialDrop> rewards = new LinkedHashMap<>();
        Set<String> rewardIds = new HashSet<>();
        int size = f.count("reward.count", 4096);
        for (int i = 0; i < size; i++) {
            String p = "reward." + i + ".";
            var accepted = permission(f, p);
            long version = Long.parseLong(f.text(p + "version"));
            if (version < 0 || !completedIds.contains(accepted.id()) || !rewardIds.add(accepted.id()) || wins.get(accepted.mapId()) == 0
                    || (permission != null && permission.id().equals(accepted.id()))) {
                throw new IllegalArgumentException("Invalid pending reward identity");
            }
            var result = new BattleResult(accepted, version, BattleResult.Outcome.SUCCESS);
            var drop = new MaterialDrop(f.text(p + "drop"), f.text(p + "material"), f.count(p + "quantity", Integer.MAX_VALUE),
                    accepted.mapId(), new CollisionBox(f.number(p + "x"), f.number(p + "y"), f.number(p + "width"), f.number(p + "height")));
            rewards.put(result, drop);
        }
        List<Portal> portals = new ArrayList<>();
        for (var original : routes()) {
            String p = "portal." + original.id() + ".";
            String map = f.text(p + "map"), spawn = f.text(p + "spawn");
            boolean enabled = f.flag(p + "enabled");
            validateSpawn(map, spawn);
            if (enabled && session.getMap(map).getType() == MapType.BATTLE
                    && (permission == null || !permission.mapId().equals(map)
                        || !permission.spawnId().equals(spawn) || !permission.portalId().equals(original.id()))) {
                throw new IllegalArgumentException("Open battle portal has no matching quest");
            }
            portals.add(new Portal(original.id(), original.sourceMapId(), original.trigger(), map, spawn, enabled));
        }
        Map<String, Boolean> unlocks = new LinkedHashMap<>();
        travel.getDestinations().forEach(d -> unlocks.put(d.id(), f.flag("travel." + d.id())));
        Map<NPC, Boolean> flags = new LinkedHashMap<>();
        npcs.forEach(npc -> flags.put(npc, f.flag("npc." + npc.getId())));
        f.finish();
        return () -> {
            inventory.restore(session.getPlayer(), chest);
            restoredLocation.restore(session);
            session.getPlayer().restoreSelections(type, element, modes);
            flow.restoreProgress(permission, wins, completedIds, rewards);
            quests.restoreProgress(done, active, ending, memorialRevealed);
            travel.restoreProgress(portals, unlocks);
            flags.forEach(NPC::restoreInteracted);
            combat.synchronizeAfterLoad();
            combat.getAbilities().restoreCooldowns(timers);
        };
    }
}
