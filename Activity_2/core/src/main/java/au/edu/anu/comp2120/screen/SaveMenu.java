package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.save.GameSaves;
import au.edu.anu.comp2120.save.SaveSlot;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Paused Escape menu with independent saves, confirmed restart and transient notifications.
 * @author Hao Liu (u7998420)
 */
final class SaveMenu {
    /** Persistence owner; retains all safe-state checks. */
    private final GameSaves saves;
    /** Current music percentage, independent of gameplay snapshots. */
    private final IntSupplier volume;
    /** Applies slider changes immediately. */
    private final IntConsumer setVolume;
    /** Requests a deferred fresh game without removing files. */
    private final Runnable restart;
    /** Reusable wrapped text measurement. */
    private final GlyphLayout text = new GlyphLayout();
    /** Current page; CLOSED leaves gameplay input available. */
    private Page page = Page.CLOSED;
    /** Focused menu row, including the volume slider. */
    private int focus;
    /** Explicit action pending confirmation, or null. */
    private Action pending;
    /** Cached slot summaries, refreshed only when entering a menu. */
    private final String[] summaries = {"", ""};
    /** Latest transient message, never a permanent control legend. */
    private String toast = "";
    /** Remaining real UI seconds for the toast. */
    private float remaining;
    /** Last consumed result event, so identical later operations can notify again. */
    private long observed;
    /** Whether the pointer owns a slider drag. */
    private boolean dragging;

    /** Menu pages.
     * @author Hao Liu (u7998420)
     */
    private enum Page {
        /** Gameplay owns input. */ CLOSED,
        /** Action list and volume control. */ HOME,
        /** Slot loading and deletion. */ SLOTS,
        /** Explicit destructive-to-live-state decision. */ CONFIRM
    }
    /** Actions requiring an explicit decision.
     * @author Hao Liu (u7998420)
     */
    private enum Action {
        /** Replace the manual file. */ SAVE,
        /** Restore the automatic file. */ AUTO,
        /** Restore the manual file. */ MANUAL,
        /** Delete the automatic file. */ DELETE_AUTO,
        /** Delete the manual file. */ DELETE_MANUAL,
        /** Replace only the running session. */ RESTART
    }

    /** Binds model callbacks without allocating graphics resources.
     * @param saves persistence coordinator
     * @param volume current percentage supplier
     * @param setVolume percentage consumer
     * @param restart deferred fresh-game request
     */
    SaveMenu(GameSaves saves, IntSupplier volume, IntConsumer setVolume, Runnable restart) {
        this.saves = saves; this.volume = volume; this.setVolume = setVolume; this.restart = restart;
        observed = saves.statusVersion();
    }

    /** Reports whether simulation and world animation must pause.
     * @return true for any open menu page
     */
    boolean isOpen() { return page != Page.CLOSED; }

    /** Processes menu input before gameplay, consuming opening and closing frames.
     * Escape first belongs to another open gameplay panel. F5/F9 remain optional shortcuts.
     * @param otherPanel whether dialogue, bag, chest, map or ending already owns input
     * @return whether this frame belongs exclusively to the menu
     */
    boolean update(boolean otherPanel) {
        boolean escape = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
        if (!isOpen()) {
            boolean manual = Gdx.input.isKeyJustPressed(Input.Keys.F5);
            boolean load = Gdx.input.isKeyJustPressed(Input.Keys.F9);
            if (otherPanel) {
                if (manual || load) { notify("Close the current panel before saving or loading."); return true; }
                return false;
            }
            if (!escape && !manual && !load) { return false; }
            if (manual) { request(Action.SAVE); }
            else { open(); focus = 0; if (load) { openSlots(); } }
            return true;
        }
        if (escape) {
            back();
            dragging = false;
            return true;
        }
        if (page == Page.HOME || page == Page.SLOTS) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) { focus = (focus + 4) % 5; }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) { focus = (focus + 1) % 5; }
            if (page == Page.SLOTS && Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) { request(Action.AUTO); return true; }
            if (page == Page.SLOTS && Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) { request(Action.MANUAL); return true; }
            if (page == Page.HOME && focus == 4) {
                int step = (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) ? 1 : 0)
                        - (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) ? 1 : 0);
                if (step != 0) { setVolume.accept(Math.max(0, Math.min(100, volume.getAsInt() + step))); }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) { activate(focus); return true; }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) { confirm(); return true; }
        pointer();
        return true;
    }

    /** Opens the menu and caches file summaries once. */
    private void open() {
        page = Page.HOME; pending = null; dragging = false;
        summaries[0] = saves.summary(SaveSlot.AUTO);
        summaries[1] = saves.summary(SaveSlot.MANUAL);
    }

    /** Opens slot management and refreshes summaries after deletions. */
    private void openSlots() {
        open(); page = Page.SLOTS; focus = 0;
    }

    /** Returns one level without confirming an operation. */
    private void back() {
        if (page == Page.CONFIRM) {
            page = pending == Action.SAVE || pending == Action.RESTART ? Page.HOME : Page.SLOTS;
            pending = null;
        } else if (page == Page.SLOTS) { page = Page.HOME; focus = 2; }
        else { page = Page.CLOSED; }
        dragging = false;
    }

    /** Whether a button can be used under the current gameplay guards.
     * @param row button index
     * @return true for navigation or a safe persistence action
     */
    private boolean enabled(int row) {
        return (page == Page.HOME ? row != 1 : row == 4) || saves.isSafe();
    }

    /** Routes a selected row to its action.
     * @param row row index in visual/keyboard order
     */
    private void activate(int row) {
        if (page == Page.SLOTS) {
            switch (row) {
                case 0 -> request(Action.AUTO);
                case 1 -> request(Action.DELETE_AUTO);
                case 2 -> request(Action.MANUAL);
                case 3 -> request(Action.DELETE_MANUAL);
                case 4 -> back();
                default -> { }
            }
            return;
        }
        switch (row) {
            case 0 -> page = Page.CLOSED;
            case 1 -> request(Action.SAVE);
            case 2 -> openSlots();
            case 3 -> request(Action.RESTART);
            default -> { }
        }
    }

    /** Applies current safety before confirmation, never treating pause as safety.
     * @param action requested operation
     */
    private void request(Action action) {
        if (action != Action.RESTART && !saves.isSafe()) {
            notify("Save/load unavailable: use Hub or Memorial Hall after active skills finish.");
            return;
        }
        if (action == Action.SAVE && !saves.hasManual()) {
            saves.saveManual(); observeStatus(); page = Page.CLOSED; return;
        }
        if (!isOpen()) { open(); }
        pending = action; page = Page.CONFIRM; dragging = false;
    }

    /** Executes only the explicitly confirmed operation; current safety is rechecked by GameSaves. */
    private void confirm() {
        if (pending == Action.DELETE_AUTO || pending == Action.DELETE_MANUAL) {
            saves.deleteSlot(pending == Action.DELETE_AUTO ? SaveSlot.AUTO : SaveSlot.MANUAL);
            observeStatus(); openSlots(); return;
        }
        if (pending == Action.RESTART) { restart.run(); }
        else if (pending == Action.SAVE) { saves.saveManual(); }
        else { saves.load(pending == Action.AUTO ? SaveSlot.AUTO : SaveSlot.MANUAL); }
        observeStatus(); pending = null; page = Page.CLOSED; dragging = false;
    }

    /** Consumes mouse clicks and slider drags using the same layout used for drawing. */
    private void pointer() {
        if (Gdx.graphics == null) { return; }
        var box = layout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (box.scale() == 0) { return; }
        float x = (Gdx.input.getX() - box.x()) / box.scale();
        float y = (Gdx.graphics.getHeight() - Gdx.input.getY() - box.y()) / box.scale();
        boolean click = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (page == Page.CONFIRM) {
            if (click && y >= 40 && y <= 84) {
                if (x >= 24 && x <= 244) { back(); }
                else if (x >= 256 && x <= 476) { confirm(); }
            }
            return;
        }
        if (click && x >= 24 && x <= 476) {
            for (int row = 0; row < (page == Page.HOME ? 4 : 5); row++) {
                float bottom = 352 - row * 54;
                if (y >= bottom && y <= bottom + 46) { focus = row; activate(row); return; }
            }
            if (page == Page.HOME && y >= 62 && y <= 108) { focus = 4; dragging = true; }
        }
        if (dragging && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            setVolume.accept(sliderPercent(x));
        } else { dragging = false; }
    }

    /** Maps a local pointer to a clamped percentage, including drags beyond the rail.
     * @param x pointer x in reference panel coordinates
     * @return integer percent in zero through one hundred
     */
    static int sliderPercent(float x) { return Math.round(Math.max(0, Math.min(1, (x - 32) / 436)) * 100); }

    /** Publishes a new result even when its text is identical to a preceding operation. */
    void observeStatus() {
        if (observed != saves.statusVersion()) { observed = saves.statusVersion(); notify(saves.status()); }
    }

    /** Shows a four-second operation message; no message is shown at startup.
     * @param message player-facing feedback
     */
    void notify(String message) { toast = message; remaining = 4; }

    /** Advances the toast using UI time independently of paused gameplay.
     * @param delta elapsed seconds; negative/non-finite values are ignored
     */
    void advanceHint(float delta) {
        if (Float.isFinite(delta) && delta >= 0) {
            remaining = Math.max(0, remaining - delta);
            if (remaining == 0) { toast = ""; }
        }
    }

    /** Exposes only a currently visible toast, never the coordinator's stale status.
     * @return transient message or empty text
     */
    String notification() { return toast; }

    /** Shared rendering/hit-testing geometry.
     * @param x panel left
     * @param y panel bottom
     * @param scale reference-to-window scale
     * @author Hao Liu (u7998420)
     */
    record Layout(float x, float y, float scale) { }

    /** Fits a 500 by 460 panel within both window axes, including narrow/short windows.
     * @param width window width
     * @param height window height
     * @return centered panel bounds, or zero scale for a minimized window
     */
    static Layout layout(int width, int height) {
        if (width <= 0 || height <= 0) { return new Layout(0, 0, 0); }
        float scale = Math.min(1.2f, Math.min(width / 540f, height / 500f));
        return new Layout((width - 500 * scale) / 2, (height - 460 * scale) / 2, scale);
    }

    /** Draws a centered pause menu and temporary translucent toast away from the top-left HUD.
     * @param batch shared screen batch
     * @param font shared bitmap font
     * @param shapes shared shape renderer
     */
    void draw(SpriteBatch batch, BitmapFont font, ShapeRenderer shapes) {
        observeStatus(); advanceHint(Gdx.graphics.getDeltaTime());
        int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        var b = layout(width, height);
        if (b.scale() == 0 || (!isOpen() && toast.isEmpty())) { return; }
        var projection = new Matrix4().setToOrtho2D(0, 0, width, height);
        batch.setProjectionMatrix(projection); shapes.setProjectionMatrix(projection);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (isOpen()) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0, 0, 0, 0.58f); shapes.rect(0, 0, width, height);
            rect(shapes, b, 0, 0, 500, 460, new Color(0.055f, 0.085f, 0.13f, 0.96f));
            if (page == Page.HOME || page == Page.SLOTS) {
                for (int row = 0; row < (page == Page.HOME ? 4 : 5); row++) {
                    boolean enabled = enabled(row);
                    rect(shapes, b, 24, 352 - row * 54, 452, 46,
                            !enabled ? new Color(0.10f, 0.12f, 0.16f, 1) : row == focus
                                    ? new Color(0.12f, 0.35f, 0.42f, 1) : new Color(0.12f, 0.17f, 0.23f, 1));
                }
                if (page == Page.HOME) {
                    rect(shapes, b, 32, 72, 436, 6, Color.DARK_GRAY);
                    rect(shapes, b, 32, 72, 436 * volume.getAsInt() / 100f, 6, Color.CYAN);
                    rect(shapes, b, 28 + 436 * volume.getAsInt() / 100f, 66, 8, 18, Color.WHITE);
                }
            } else {
                rect(shapes, b, 24, 40, 220, 44, Color.DARK_GRAY);
                rect(shapes, b, 256, 40, 220, 44, new Color(0.12f, 0.35f, 0.42f, 1));
            }
            shapes.end(); batch.begin();
            label(batch, font, b, page == Page.SLOTS ? "MANAGE SAVES" : "PAUSED", 24, 434, 452, 28, 1.55f, Color.WHITE);
            if (page == Page.HOME || page == Page.SLOTS) {
                String[] names = page == Page.HOME
                        ? new String[] {"Resume", "Save manually", "Manage saves", "Restart game"}
                        : new String[] {"Load automatic", "Delete automatic", "Load manual", "Delete manual", "Back"};
                for (int row = 0; row < (page == Page.HOME ? 4 : 5); row++) {
                    float top = 384 - row * 54;
                    label(batch, font, b, names[row], 36, top, 428, 20, 1.1f,
                            !enabled(row) ? Color.GRAY : Color.WHITE);
                    if (page == Page.SLOTS && (row == 0 || row == 2)) {
                        label(batch, font, b, summaries[row / 2], 36, top - 18, 428, 12, 0.7f, Color.LIGHT_GRAY);
                    }
                }
                if (page == Page.HOME) {
                    label(batch, font, b, (focus == 4 ? "> " : "") + "Music volume  " + volume.getAsInt() + " / 100",
                        32, 104, 436, 20, 1, Color.CYAN);
                }
                label(batch, font, b, saves.isSafe() ? "Esc: back  |  Arrows / Enter or mouse"
                        : "Save actions locked: Hub / Memorial, no active skills.\nEsc: back  |  Arrows / Enter or mouse",
                        24, 46, 452, 34, 0.85f, Color.LIGHT_GRAY);
            } else {
                String prompt = switch (pending) {
                    case SAVE -> "Replace the manual save?\n\nThe automatic save stays unchanged.";
                    case AUTO, MANUAL -> "Load " + (pending == Action.AUTO ? "automatic" : "manual")
                            + " save?\n\nUnsaved progress will be replaced.\nNeither save file will change.";
                    case DELETE_AUTO, DELETE_MANUAL -> "Permanently delete "
                            + (pending == Action.DELETE_AUTO ? "automatic" : "manual")
                            + " save?\n\nThis cannot be undone.\nYour current game and the other save stay unchanged.";
                    case RESTART -> "Restart from the beginning?\n\nUnsaved progress will be lost.\nBoth save files and music volume are kept.";
                };
                label(batch, font, b, prompt, 32, 340, 436, 220, 1.2f, Color.WHITE);
                label(batch, font, b, "Cancel (Esc)", 36, 68, 196, 22, 1, Color.WHITE);
                label(batch, font, b, "Confirm (Enter)", 268, 68, 196, 22, 1, Color.WHITE);
            }
            batch.end();
        }
        if (!toast.isEmpty()) {
            float scale = b.scale();
            float w = Math.min(560 * scale, width - 24 * scale);
            float alpha = Math.min(1, remaining / 0.6f);
            font.getData().setScale(scale);
            text.setText(font, toast, new Color(1, 1, 1, alpha), w - 24 * scale, Align.center, true);
            for (int i = 0; i < 24 && text.height > 72 * scale; i++) {
                font.getData().setScale(font.getData().scaleX * 0.85f);
                text.setText(font, toast, new Color(1, 1, 1, alpha), w - 24 * scale, Align.center, true);
            }
            float h = text.height + 24 * scale, x = (width - w) / 2;
            float y = isOpen() ? 12 * scale : height * 0.22f;
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.06f, 0.10f, 0.16f, 0.8f * alpha); shapes.rect(x, y, w, h); shapes.end();
            batch.begin(); font.draw(batch, text, x + 12 * scale, y + h - 12 * scale); batch.end();
        }
        font.setColor(Color.WHITE); font.getData().setScale(1);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Draws one reference-coordinate filled rectangle.
     * @param shapes active shape renderer
     * @param b panel geometry
     * @param x local left
     * @param y local bottom
     * @param w width
     * @param h height
     * @param color fill color
     */
    private void rect(ShapeRenderer shapes, Layout b, float x, float y, float w, float h, Color color) {
        shapes.setColor(color); shapes.rect(b.x() + x * b.scale(), b.y() + y * b.scale(), w * b.scale(), h * b.scale());
    }

    /** Fits wrapped text inside a reference-coordinate rectangle.
     * @param batch active sprite batch
     * @param font shared font
     * @param b panel geometry
     * @param value text
     * @param x local left
     * @param top local top
     * @param width local width
     * @param height local height
     * @param size requested relative font size
     * @param color text color
     */
    private void label(SpriteBatch batch, BitmapFont font, Layout b, String value, float x, float top,
            float width, float height, float size, Color color) {
        font.getData().setScale(size * b.scale());
        text.setText(font, value, color, width * b.scale(), Align.left, true);
        for (int i = 0; i < 24 && text.height > height * b.scale(); i++) {
            font.getData().setScale(font.getData().scaleX * 0.85f);
            text.setText(font, value, color, width * b.scale(), Align.left, true);
        }
        font.draw(batch, text, b.x() + x * b.scale(), b.y() + top * b.scale());
    }
}
