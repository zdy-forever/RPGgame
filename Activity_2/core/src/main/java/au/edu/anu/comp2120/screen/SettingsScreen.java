package au.edu.anu.comp2120.screen;

import au.edu.anu.comp2120.MainGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

/**
 * Boot-time settings, styled like the in-game pause menu but without game-bound actions.
 * Exposes the shared music volume slider and a fullscreen toggle, both persisted via {@link GameSettings}.
 *
 * @author Daoyan Zhu (u7782042)
 * @author Jiaqi Chen (u7851266)
 */
public final class SettingsScreen extends ScreenAdapter {

    /** Panel background fill. */
    private static final Color PANEL = new Color(0.055f, 0.085f, 0.13f, 0.96f);
    /** Unfocused menu row. */
    private static final Color ROW = new Color(0.12f, 0.17f, 0.23f, 1);
    /** Focused menu row. */
    private static final Color ROW_FOCUS = new Color(0.12f, 0.35f, 0.42f, 1);

    /** Shared screen batch. */
    private final SpriteBatch batch = new SpriteBatch();
    /** Shared bitmap font for text. */
    private final BitmapFont font = new BitmapFont();
    /** Shared shape renderer. */
    private final ShapeRenderer shapes = new ShapeRenderer();
    /** Current keyboard/mouse focus row. */
    private int focus = 0;
    /** Whether the volume slider owns a pointer drag. */
    private boolean dragging = false;
    /** Guards against double resource disposal. */
    private boolean disposed = false;

    /** Builds the settings panel. */
    public SettingsScreen() { }

    /**
     * Processes preference controls and draws the volume, fullscreen and back options.
     * @param delta elapsed frame seconds; menu input and layout do not use this value
     */
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
        for (int row = 0; row < 3; row++) {
            rect(shapes, layout, 24, 352 - row * 54, 452, 46, row == focus ? ROW_FOCUS : ROW);
        }
        // Volume slider track, fill and handle.
        rect(shapes, layout, 32, 356, 436, 6, Color.DARK_GRAY);
        rect(shapes, layout, 32, 356, 436 * GameSettings.musicVolume() / 100f, 6, Color.CYAN);
        rect(shapes, layout, 28 + 436 * GameSettings.musicVolume() / 100f, 350, 8, 18, Color.WHITE);
        shapes.end();

        batch.begin();
        label(layout, "SETTINGS", 24, 434, 1.55f, Color.WHITE);
        label(layout, (focus == 0 ? "> " : "") + "Music volume  " + GameSettings.musicVolume() + " / 100",
                36, 392, 1.1f, Color.WHITE);
        label(layout, (focus == 1 ? "> " : "") + "Fullscreen: " + (GameSettings.fullscreen() ? "On" : "Off"),
                36, 330, 1.1f, Color.WHITE);
        label(layout, (focus == 2 ? "> " : "") + "Back",
                36, 276, 1.1f, Color.WHITE);
        label(layout, "W/S or Up/Down: select | E/Enter: confirm | Mouse: click", 24, 46, 0.85f, Color.LIGHT_GRAY);
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Handles keyboard and pointer input. */
    private void update() {
        if (MenuKeys.down()) { focus = (focus + 1) % 3; dragging = false; }
        if (MenuKeys.up()) { focus = (focus + 2) % 3; dragging = false; }
        if (focus == 0) {
            int step = (MenuKeys.right() ? 1 : 0) - (MenuKeys.left() ? 1 : 0);
            if (step != 0) { GameSettings.setMusicVolume(GameSettings.musicVolume() + step * 2); }
        } else if (focus == 1) {
            if (MenuKeys.confirm() || MenuKeys.left() || MenuKeys.right()) { toggleFullscreen(); }
        } else if (MenuKeys.confirm()) { back(); }
        pointer();
    }

    /** Toggles and persists the fullscreen preference, applying it immediately. */
    private void toggleFullscreen() {
        boolean next = !GameSettings.fullscreen();
        GameSettings.setFullscreen(next);
        applyFullscreen(next);
    }

    /** Applies a fullscreen state without changing the persisted preference.
     * @param on true to enter fullscreen, false to return to windowed mode
     */
    static void applyFullscreen(boolean on) {
        if (on) {
            if (!Gdx.graphics.isFullscreen()) { Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode()); }
        } else if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(960, 540);
        }
    }

    /** Returns to the main menu on the next frame. */
    private void back() {
        Gdx.app.postRunnable(() ->
                ((MainGame) Gdx.app.getApplicationListener()).setScreen(new MainMenuScreen()));
    }

    /** Maps mouse clicks to rows and drags the volume slider. */
    private void pointer() {
        var layout = SaveMenu.layout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (layout.scale() == 0) { return; }
        float x = (Gdx.input.getX() - layout.x()) / layout.scale();
        float y = (Gdx.graphics.getHeight() - Gdx.input.getY() - layout.y()) / layout.scale();
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            int row = pointerRow(x, y);
            if (row >= 0) {
                focus = row;
                if (row == 0) { dragging = true; }
                else if (row == 1) { toggleFullscreen(); return; }
                else { back(); return; }
            }
        }
        if (dragging && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            GameSettings.setMusicVolume(SaveMenu.sliderPercent(x));
        } else { dragging = false; }
    }

    /** Resolves a local settings-panel pointer to its selectable row.
     * @param x pointer x in reference coordinates
     * @param y pointer y in reference coordinates
     * @return zero through two for volume/fullscreen/back, or minus one outside a row
     */
    static int pointerRow(float x, float y) {
        if (x < 24 || x > 476) { return -1; }
        for (int row = 0; row < 3; row++) {
            float bottom = 352 - row * 54;
            if (y >= bottom && y <= bottom + 46) { return row; }
        }
        return -1;
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
     * @param size font scale
     * @param color text color
     */
    private void label(SaveMenu.Layout layout, String value, float x, float top,
                       float size, Color color) {
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
