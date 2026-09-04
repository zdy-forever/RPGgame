package au.edu.anu.comp2120.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Displays the initial placeholder screen used to verify that the game starts.
 *
 * @author Daoyan Zhu (u7782042)
 */
public final class GameScreen extends ScreenAdapter {

    private static final String TITLE = "COMP2120 Game Skeleton";
    private static final float TEXT_MARGIN = 32.0f;

    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();

    /**
     * Creates the placeholder game screen and its rendering resources.
     */
    public GameScreen() {
        // Rendering resources are initialized by their field declarations.
    }

    /**
     * Renders a clear background and a small placeholder title.
     *
     * @param delta seconds elapsed since the previous frame
     */
    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.08f, 0.10f, 0.16f, 1.0f);

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, TITLE, TEXT_MARGIN, Gdx.graphics.getHeight() - TEXT_MARGIN);
        batch.end();
    }

    /**
     * Updates the drawing projection when the window size changes.
     *
     * @param width new window width in pixels
     * @param height new window height in pixels
     */
    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0.0f, 0.0f, width, height);
    }

    /**
     * Releases the temporary rendering resources owned by this screen.
     */
    @Override
    public void dispose() {
        font.dispose();
        batch.dispose();
    }
}
