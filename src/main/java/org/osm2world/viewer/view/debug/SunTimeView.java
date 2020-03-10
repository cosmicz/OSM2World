package org.osm2world.viewer.view.debug;

import java.awt.Color;
import javax.media.opengl.GL;
import org.osm2world.core.GlobalValues;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.viewer.view.TextRenderer;
import org.osm2world.viewer.view.TextRendererFixedFunction;
import org.osm2world.viewer.view.TextRendererShader;

public class SunTimeView extends DebugView {
	private TextRenderer textRenderer;
	private int width = 0, height = 0;
	private float scale = 1;

	@Override
	public void reset() {
		super.reset();
		if (textRenderer != null) {
			textRenderer.destroy();
		}
		textRenderer = null;
	}

	@Override
	public boolean canBeUsed() {
		return map != null;
	}

	@Override
	public void renderTo(GL gl, Camera camera, Projection projection) {

		if (!canBeUsed()) {
			return;
		}
		if (textRenderer == null) {
			if ("shader".equals(config.getString("joglImplementation"))) {
				System.out.println("SunTimeView using jogl");
				textRenderer = new TextRendererShader(gl.getGL2ES2());
			} else {
				System.out.println("SunTimeView using fixedFunction");
				textRenderer = new TextRendererFixedFunction();
			}
			textRenderer.reshape(width, height);
			textRenderer.setScale(scale);
		}

		// textRenderer.drawTextTop(
		// 	"sunlightFromDirection: " +
		// 		GlobalLightingParameters.DEFAULT.lightFromDirection.toString(),
		// 	50, 100, Color.GREEN);
	}

	/**
	 * Calculate the scale factor for text rendering. Ensures that the help text
	 * is always visible. Has to be adjusted when text size changes.
	 */
	private float calculateScale() {
		float scale;
		if (width > 800) {
			scale = width / 800f;
		} else if (width < 550) {
			scale = width / 550f;
		} else {
			scale = 1;
		}
		if (height > 600) {
			scale = Math.min(scale, height / 600f);
		} else if (height < 200) {
			scale = Math.min(scale, height / 200f);
		} else {
			scale = Math.min(scale, 1f);
		}
		return scale;
	}

	public void reshape(int width, int height) {
		this.width = width;
		this.height = height;
		this.scale = calculateScale();
		if (textRenderer != null) {
			textRenderer.reshape(width, height);
			textRenderer.setScale(scale);
		}
	}

	@Override
	protected void fillTarget(JOGLTarget target) {
		// do nothing, has its own renderTo implementation
	}
}
