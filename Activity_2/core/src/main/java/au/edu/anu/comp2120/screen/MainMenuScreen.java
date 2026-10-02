package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.MainGame;
import au.edu.anu.comp2120.save.SaveSelection;
import au.edu.anu.comp2120.save.SaveSlot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import java.nio.file.Path;

/**
 * Title screen shown before any game session, mirroring the in-game pause menu's look and navigation.
 * Offers New Game, Continue (disabled until a valid save exists), Settings and Quit.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class MainMenuScreen extends ScreenAdapter {

    /** Save directory shared with the running game so Continue sees the same slots. */
    private static final Path SAVE_DIR = Path.of(System.getProperty("user.home"),
            ".comp2120-game", "saves");
    /** Panel background fill. */
    private static final Color PANEL = new Color(0.055f, 0.085f, 0.13f, 0.96f);
    /** Unfocused menu row. */
    private static final Color ROW = new Color(0.12f, 0.17f, 0.23f, 1);
    /** Focused menu row. */
    private static final Color ROW_FOCUS = new Color(0.12f, 0.35f, 0.42f, 1);
    /** Disabled menu row. */
    private static final Color ROW_DISABLED = new Color(0.10f, 0.12f, 0.16f, 1);
    /** Visible menu rows in keyboard/visual order. */
    private static final String[] ROWS = {"New Game", "Continue", "Settings", "Quit"};

    /** Row index belonging to Continue, gated by save availability. */
    private static final int CONTINUE_ROW = 1;

    /** Shared screen batch. */
    private final SpriteBatch batch = new SpriteBatch();
    /** Shared bitmap font for menu text. */
    private final BitmapFont font = new BitmapFont();
    /** Shared shape renderer for the panel and rows. */
    private final ShapeRenderer shapes = new ShapeRenderer();
    /** Whether at least one valid save slot exists. */
    private final boolean hasSave;
    /** Current keyboard/mouse focus row. */
    private int focus = 0;
    /** Guards against double resource disposal across hide() and dispose(). */
    private boolean disposed = false;

    /** Builds the menu and probes save availability once. */
    public MainMenuScreen() {
        hasSave = SaveSelection.latestValid(SAVE_DIR).isPresent();
    }

    @Override
    public void render(float delta) {
        update();
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        Gdx.gl.glClearColor(0.02f, 0.03f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        var layout = SaveMenu.layout(width, height);
        if (layout.scale() == 0) { return; }
        var projection = new Matrix4().setToOrtho2D(0, 0, width, height);
        batch.setProjectionMatrix(projection);
        shapes.setProjectionMatrix(projection);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        rect(shapes, layout, 0, 0, 500, 460, PANEL);
        for (int row = 0; row < ROWS.length; row++) {
            boolean enabled = row != CONTINUE_ROW || hasSave;
            rect(shapes, layout, 24, 352 - row * 54, 452, 46,
                    !enabled ? ROW_DISABLED : row == focus ? ROW_FOCUS : ROW);
        }
        shapes.end();

        batch.begin();
        label(layout, "COMP2120 GAME", 24, 434, 452, 28, 1.55f, Color.WHITE);
        for (int row = 0; row < ROWS.length; row++) {
            boolean enabled = row != CONTINUE_ROW || hasSave;
            label(layout, ROWS[row], 36, 384 - row * 54, 428, 20, 1.1f, enabled ? Color.WHITE : Color.GRAY);
        }
        label(layout, "W/S or Up/Down: select | E/Enter: confirm | Mouse: click",
                24, 46, 452, 34, 0.85f, Color.LIGHT_GRAY);
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Handles keyboard and pointer navigation plus the chosen action. */
    private void update() {
        if (MenuKeys.down()) { focus = nextFocus(focus, 1); }
        if (MenuKeys.up()) { focus = nextFocus(focus, -1); }
        if (MenuKeys.confirm()) { activate(focus); }
        pointer();
    }

    /** Steps the focus in a direction, skipping the disabled Continue row.
     * @param from current row
     * @param step plus or minus one
     * @return next enabled row index
     */
    private int nextFocus(int from, int step) {
        int next = from;
        for (int guard = 0; guard < ROWS.length; guard++) {
            next = (next + step + ROWS.length) % ROWS.length;
            if (next != CONTINUE_ROW || hasSave) { return next; }
        }
        return from;
    }

    /** Runs the selected row.
     * @param row chosen row index
     */
    private void activate(int row) {
        switch (row) {
            case 0 -> startGame(null);
            case CONTINUE_ROW -> { if (hasSave) { startGame(SaveSelection.latestValid(SAVE_DIR).orElse(null)); } }
            case 2 -> openSettings();
            case 3 -> Gdx.app.exit();
            default -> { }
        }
    }

    /** Starts a fresh or continued game on the next frame, releasing this menu.
     * @param slot save slot to load, or null for a new game
     */
    private void startGame(SaveSlot slot) {
        float volume = GameSettings.musicVolume() / 100f;
        Gdx.app.postRunnable(() -> {
            GameScreen game = new GameScreen(volume, false);
            if (slot != null) { game.loadFrom(slot); }
            ((MainGame) Gdx.app.getApplicationListener()).setScreen(game);
        });
    }

    /** Opens the settings screen on the next frame, releasing this menu. */
    private void openSettings() {
        Gdx.app.postRunnable(() ->
                ((MainGame) Gdx.app.getApplicationListener()).setScreen(new SettingsScreen()));
    }

    /** Maps mouse clicks to rows using the same panel geometry used for drawing. */
    private void pointer() {
        var layout = SaveMenu.layout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (layout.scale() == 0) { return; }
        float x = (Gdx.input.getX() - layout.x()) / layout.scale();
        float y = (Gdx.graphics.getHeight() - Gdx.input.getY() - layout.y()) / layout.scale();
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) { return; }
        if (x < 24 || x > 476) { return; }
        for (int row = 0; row < ROWS.length; row++) {
            float bottom = 352 - row * 54;
            if (y >= bottom && y <= bottom + 46) { focus = row; activate(row); return; }
        }
    }

    /** Draws one reference-coordinate filled rectangle.
     * @param shapes shape renderer
     * @param layout panel layout
     * @param x left edge in reference coordinates
     * @param y bottom edge in reference coordinates
     * @param w width in reference coordinates
     * @param h height in reference coordinates
     * @param color fill color
     */
    private static void rect(ShapeRenderer shapes, SaveMenu.Layout layout, float x, float y,
                             float w, float h, Color color) {
        shapes.setColor(color);
        shapes.rect(layout.x() + x * layout.scale(), layout.y() + y * layout.scale(),
                w * layout.scale(), h * layout.scale());
    }

    /** Draws scaled left-aligned text.
     * @param layout panel layout
     * @param value text to draw
     * @param x left edge in reference coordinates
     * @param top baseline y in reference coordinates
     * @param width unused horizontal bound
     * @param height unused vertical bound
     * @param size font scale
     * @param color text color
     */
    private void label(SaveMenu.Layout layout, String value, float x, float top,
                       float width, float height, float size, Color color) {
        font.getData().setScale(size * layout.scale());
        font.setColor(color);
        font.draw(batch, value, layout.x() + x * layout.scale(), layout.y() + top * layout.scale());
        font.setColor(Color.WHITE);
        font.getData().setScale(1);
    }

    /** Releases resources when the screen is replaced. */
    @Override
    public void hide() { disposeResources(); }

    /** Releases resources when the application exits. */
    @Override
    public void dispose() { disposeResources(); }

    /** Idempotent resource release. */
    private void disposeResources() {
        if (disposed) { return; }
        disposed = true;
        batch.dispose();
        font.dispose();
        shapes.dispose();
    }
}
