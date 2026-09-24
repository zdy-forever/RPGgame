package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.save.GameSaves;
import au.edu.anu.comp2120.save.SaveSlot;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;

/** Keyboard save/load overlay with explicit overwrite and timeline replacement confirmation.
 * @author Hao Liu (u7998420)
 */
final class SaveMenu {
    /** Save coordinator; owns no graphical resources. */
    private final GameSaves saves;
    /** Whether the modal is visible. */
    private boolean open;
    /** Whether Enter is confirming a manual overwrite. */
    private boolean overwrite;
    /** Chosen load slot awaiting confirmation, or null for slot selection. */
    private SaveSlot selected;
    /** Cached summaries prevent disk reads each render frame. */
    private String summaries = "";
    /** UI-local hint when another panel must be closed. */
    private String hint = "";
    /** Remaining UI seconds for a temporary blocked-action hint. */
    private float hintRemaining;
    /** Distinguishes a panel blocker from an unsafe gameplay state. */
    private boolean panelHint;
    /** Duration of a blocked-action notification, independent of gameplay simulation. */
    private static final float HINT_SECONDS = 4f;
    /** Reusable text measurement for the notification background and modal fitting. */
    private final GlyphLayout textLayout = new GlyphLayout();

    /** Binds a menu without creating graphical resources.
     * @param saves save coordinator
     */
    SaveMenu(GameSaves saves) { this.saves = saves; }
    /** Reads modal state for pausing world visuals.
     * @return whether gameplay input must pause
     */
    boolean isOpen() { return open; }

    /** Processes menu keys before gameplay; world actions never share a confirmation frame.
     * @param otherPanel whether a dialogue, bag, map or chest is open
     * @return whether this frame's gameplay input is consumed
     */
    boolean update(boolean otherPanel) {
        if (!hint.isEmpty() && (panelHint ? !otherPanel : saves.isSafe())) { clearHint(); }
        if (!open) {
            boolean manual = Gdx.input.isKeyJustPressed(Input.Keys.F5);
            boolean load = Gdx.input.isKeyJustPressed(Input.Keys.F9);
            if (!manual && !load) { return false; }
            if (otherPanel) { showHint("Close the dialogue, bag, chest or map before saving/loading.", true); return true; }
            if (!saves.isSafe()) { showHint("Save/load requires a living player in Hub / Memorial, with no active skills.", false); return true; }
            clearHint();
            if (manual && !saves.hasManual()) { saves.saveManual(); return true; }
            open = true; overwrite = manual; selected = null;
            summaries = saves.summary(SaveSlot.AUTO) + "\n\n" + saves.summary(SaveSlot.MANUAL);
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { open = false; return true; }
        if (!overwrite && selected == null) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) { selected = SaveSlot.AUTO; }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) { selected = SaveSlot.MANUAL; }
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (overwrite) { saves.saveManual(); }
            else { saves.load(selected); }
            open = false;
        }
        return true;
    }

    /** Starts or refreshes a temporary blocked-action hint.
     * @param message explanation shown to the player
     * @param panel whether closing another panel resolves this hint
     */
    private void showHint(String message, boolean panel) {
        hint = message; panelHint = panel; hintRemaining = HINT_SECONDS;
    }

    /** Discards a notification once it expires or its blocking condition disappears. */
    private void clearHint() { hint = ""; hintRemaining = 0; }

    /** Ages hints using UI time, including frames when gameplay is paused.
     * @param delta elapsed UI seconds; non-finite or negative values are ignored
     */
    void advanceHint(float delta) {
        if (Float.isFinite(delta) && delta >= 0 && !hint.isEmpty()) {
            hintRemaining = Math.max(0, hintRemaining - delta);
            if (hintRemaining == 0) { clearHint(); }
        }
    }

    /** Resolves the current notification without duplicating the default shortcut text.
     * @return temporary hint or the coordinator's latest status
     */
    String notification() { return hint.isEmpty() ? saves.status() : hint; }

    /** Graphics-free top-left notification geometry in logical window units.
     * @param scale font scale limited by both window dimensions
     * @param x left inset
     * @param top top text baseline below visible health and coordinate HUDs
     * @param width available wrapping width
     */
    record HintLayout(float scale, float x, float top, float width) { }

    /** Reserves space below existing HUD elements and scales even in short, wide windows.
     * @param width logical window width
     * @param height logical window height
     * @return geometry, or zero geometry for a minimized window
     */
    static HintLayout hintLayout(int width, int height) {
        var hud = HudLayout.forWindow(width, height);
        if (hud.scale() == 0) { return new HintLayout(0, 0, 0, 0); }
        float scale = hud.scale() / GameConfig.HUD_MAX_SCALE;
        float x = 12 * hud.scale();
        float offset = hud.detailsVisible() ? 64 : hud.healthVisible() ? 38 : 12;
        return new HintLayout(scale, x, height - offset * hud.scale(),
                Math.min(520 * scale, width - 2 * x));
    }

    /** Draws hints and the modal using resources owned by GameScreen.
     * @param batch shared screen-space batch
     * @param font shared font
     * @param shapes shared shape renderer
     */
    void draw(SpriteBatch batch, BitmapFont font, ShapeRenderer shapes) {
        advanceHint(Gdx.graphics.getDeltaTime());
        int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        if (width <= 0 || height <= 0) { return; }
        var projection = new Matrix4().setToOrtho2D(0, 0, width, height);
        batch.setProjectionMatrix(projection);
        var layout = hintLayout(width, height);
        float scale = layout.scale();
        float padding = 8 * scale;
        font.getData().setScale(scale);
        textLayout.setText(font, notification(), Color.WHITE, layout.width(), Align.left, true);
        if (open) {
            shapes.setProjectionMatrix(projection);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.06f, 0.08f, 0.12f, 1);
            shapes.rect(12 * scale, height * 0.18f, width - 24 * scale, height * 0.64f);
            shapes.end();
        } else {
            shapes.setProjectionMatrix(projection);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.06f, 0.08f, 0.12f, 1);
            shapes.rect(layout.x() - padding, layout.top() - textLayout.height - padding,
                    textLayout.width + 2 * padding, textLayout.height + 2 * padding);
            shapes.end();
        }
        batch.begin();
        font.setColor(Color.WHITE); font.getData().setScale(scale);
        if (!open) { font.draw(batch, textLayout, layout.x(), layout.top()); }
        if (open) {
            String prompt = overwrite ? "Replace the MANUAL save? Automatic save stays unchanged.\nEnter: confirm | Esc: cancel"
                    : selected == null ? "1: Automatic save | 2: Manual save | Esc: cancel"
                    : "Load " + selected + "? Unsaved progress will be replaced.\nNeither save file will change.\nEnter: confirm | Esc: cancel";
            String content = "SAVE / LOAD\n\n" + summaries + "\n\n" + prompt;
            float textWidth = width - 60 * scale;
            textLayout.setText(font, content, Color.WHITE, textWidth, Align.left, true);
            // Fit unusually long file-error summaries without spilling outside the modal.
            for (int i = 0; i < 12 && textLayout.height > height * 0.52f; i++) {
                font.getData().setScale(font.getData().scaleX * 0.85f);
                textLayout.setText(font, content, Color.WHITE, textWidth, Align.left, true);
            }
            font.draw(batch, textLayout, 30 * scale, height * 0.76f);
        }
        font.getData().setScale(1); font.setColor(Color.WHITE);
        batch.end();
    }
}
