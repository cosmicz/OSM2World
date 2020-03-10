package org.osm2world.core.target.common.lighting;

import com.optivoltlabs.sunflower.SunPosition;
import java.awt.Color;
import org.osm2world.core.math.VectorXYZ;

/**
 * parameters that describe lighting affecting the entire scene
 */
public class GlobalLightingParameters {

	public final Color globalAmbientColor;

	/**
	 * source of the scene's directional lighting;
	 * null disables it and leaves only ambient lighting
	 */
	public VectorXYZ lightFromDirection;

	public final Color lightColorDiffuse;
	public final Color lightColorSpecular;

	private GlobalLightingParameters(Color globalAmbientLight,
									 VectorXYZ lightFromDirection,
									 Color lightColorDiffuse,
									 Color lightColorSpecular) {

		this.globalAmbientColor = globalAmbientLight;
		this.lightFromDirection = lightFromDirection;
		this.lightColorDiffuse = lightColorDiffuse;
		this.lightColorSpecular = lightColorSpecular;
	}

	public void setDirectionFromAzEl(double az, double el) {
		this.lightFromDirection = azElToXYZ(az, el);
	}

	public static VectorXYZ azElToXYZ(double az, double el) {
		double rAz = Math.toRadians(az);
		double rEl = Math.toRadians(el);
		double hyp = Math.cos(rEl);
		return new VectorXYZ(/* x */ hyp * Math.sin(rAz),
							 /* y */ Math.sin(rEl),
							 /* z */ hyp * Math.cos(rAz));
	}

	public static final VectorXYZ sunVectorSF201909231045 =
		new VectorXYZ(0.32964, 0.58051, -0.74454);
	public static final VectorXYZ sunVectorSF201909231200 =
		new VectorXYZ(0.00869, 0.61505, -0.78844);
	public static final VectorXYZ sunVectorDefault =
		new VectorXYZ(1.0, 1.5, -1.0);

	public static final GlobalLightingParameters DEFAULT =
		new GlobalLightingParameters(new Color(1.0f, 1.0f, 1.0f),
									 // new Color(0.8f, 0.8f, 0.8f),
									 sunVectorSF201909231200, Color.WHITE,
									 Color.WHITE);
}
