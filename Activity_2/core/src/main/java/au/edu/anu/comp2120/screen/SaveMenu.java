package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.save.GameSaves;
import au.edu.anu.comp2120.save.SaveSlot;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
        if (!open) {
            boolean manual = Gdx.input.isKeyJustPressed(Input.Keys.F5);
            boolean load = Gdx.input.isKeyJustPressed(Input.Keys.F9);
            if (!manual && !load) { return false; }
            if (otherPanel) { hint = "Close the dialogue, bag, chest or map before saving/loading."; return true; }
            if (!saves.isSafe()) { hint = "Save/load requires a living player in Hub / Memorial, with no active skills."; return true; }
            hint = "";
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

    /** Draws hints and the modal using resources owned by GameScreen.
     * @param batch shared screen-space batch
     * @param font shared font
     * @param shapes shared shape renderer
     */
    void draw(SpriteBatch batch, BitmapFont font, ShapeRenderer shapes) {
        float width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        if (width <= 0 || height <= 0) { return; }
        var projection = new Matrix4().setToOrtho2D(0, 0, width, height);
        batch.setProjectionMatrix(projection);
        float scale = Math.max(0.65f, Math.min(1f, width / 900f));
        if (open) {
            shapes.setProjectionMatrix(projection);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.06f, 0.08f, 0.12f, 1);
            shapes.rect(12, height * 0.18f, width - 24, height * 0.64f);
            shapes.end();
        }
        batch.begin();
        font.setColor(Color.WHITE); font.getData().setScale(scale);
        font.draw(batch, hint.isEmpty() ? saves.status() + "  [F5 Save / F9 Load]" : hint,
                16, 40, width - 32, Align.left, true);
        if (open) {
            String prompt = overwrite ? "Replace the MANUAL save? Automatic save stays unchanged.\nEnter: confirm | Esc: cancel"
                    : selected == null ? "1: Automatic save | 2: Manual save | Esc: cancel"
                    : "Load " + selected + "? Unsaved progress will be replaced.\nNeither save file will change.\nEnter: confirm | Esc: cancel";
            font.draw(batch, "SAVE / LOAD\n\n" + summaries + "\n\n" + prompt,
                    30, height * 0.76f, width - 60, Align.left, true);
        }
        font.getData().setScale(1); font.setColor(Color.WHITE);
        batch.end();
    }
}
