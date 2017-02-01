package at.searles.math.color;

import at.searles.math.Commons;

/**
 * Colors can come in two flavors: as integers in AARRGGBB-Format, or
 * as float[4]-arrays in rgba-format or laba-format.
 */
public class Colors {

	public static double brightness(int argb) {
		int r = (argb & 0x00ff0000) >> 16;
		int g = (argb & 0x0000ff00) >> 8;
		int b = (argb & 0x000000ff);

		return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255;
	}

	public static String toColorString(int color) {
		if((color & 0xff000000) == 0xff000000) {
			// alpha is 100%
			return String.format("#%06x", color & 0xffffff);
		} else {
			return String.format("#%08x", color);
		}
	}

	static int toHexDigit(char digit) {
		if(digit >= '0' && digit <= '9') return digit - '0';
		else if(digit >= 'A' && digit <= 'F') return digit - 'A' + 10;
		else if(digit >= 'a' && digit <= 'f') return digit - 'a' + 10;
		return -1;
	}

	public static int fromColorString(String color) {
		if(color.startsWith("#")) {
			color = color.substring(1);

			int c = 0xffffffff;

			if(color.length() == 3 || color.length() == 4) {
				for(int i = 0; i < color.length(); ++i) {
					int d = toHexDigit(color.charAt(i));
					if(d == -1) throw new ColorFormatException("bad format: " + color);
					c <<= 4;
					c |= d;
					c <<= 4;
					c |= d;
				}

				return c;
			} else if(color.length() == 6 || color.length() == 8) {
				for(int i = 0; i < color.length(); ++i) {
					int d = toHexDigit(color.charAt(i));
					if(d == -1) throw new ColorFormatException("bad format: " + color);
					c <<= 4;
					c |= d;
				}

				return c;
			}
		}

		// fixme: some colors?

		throw new ColorFormatException("bad format: " + color);
	}


	public static int rgb2int(float[] rgba) {
		int r = (int) Commons.clamp(rgba[0] * 256.0f, 0.0f, 255.0f);
		int g = (int) Commons.clamp(rgba[1] * 256.0f, 0.0f, 255.0f);
		int b = (int) Commons.clamp(rgba[2] * 256.0f, 0.0f, 255.0f);
		int a = (int) Commons.clamp(rgba[3] * 256.0f, 0.0f, 255.0f);

		return a << 24 | r << 16 | g << 8 | b;
	}

	public static float[] int2rgb(int argb) {
		return int2rgb(argb, new float[4]);
	}

	public static float[] int2rgb(int argb, float[] rgba) {
		rgba[0] = ((argb >> 16) & 0x0ff) / 255.0f;
		rgba[1] = ((argb >> 8) & 0x0ff) / 255.0f;
		rgba[2] = (argb & 0x0ff) / 255.0f;
		rgba[3] = ((argb >> 24) & 0x0ff) / 255.0f;

		return rgba;
	}

	public static float[] rgb2lab(float[] rgba) {
		return rgb2lab(rgba, new float[4]);
	}

	public static float[] la2rgb(float[] laba) {
		return lab2rgb(laba, new float[4]);
	}

	static float finv(float t) {
		return ((t > (216.f / 24389.f)) ?
				t * t * t : (108.f / 841.f * (t - 4.f / 29.f)));
	}

	static float K(float g) {
		if(g > 0.0031308) {
			return 1.055f * (float) Math.pow(g, 1.f / 2.4f) - 0.055f;
		} else {
			return 12.92f * g;
		}
	}

	public static float[] lab2rgb(float[] lab, float[] rgb) {
		float fx = (lab[0] + 16.f) / 116.f + lab[1] / 500.f;
		float fy = (lab[0] + 16.f) / 116.f;
		float fz = (lab[0] + 16.f) / 116.f - lab[2] / 200.f;

		float X = 0.9505f * finv(fx);
		float Y = 1.f * finv(fy);
		float Z = 1.0890f * finv(fz);

		float r0 = X * 3.2404542f - Y * 1.5371385f - Z * 0.4985314f; // red
		float g0 = -X * 0.9692660f + Y * 1.8760108f + Z * 0.0415560f; // green
		float b0 = X * 0.0556434f - Y * 0.2040259f + Z * 1.0572252f; // blue

		rgb[0] = K(r0); rgb[1] = K(g0); rgb[2] = K(b0); rgb[3] = lab[3];

		return rgb;
	}



	static float f(float t) {
		return ((t > (216.f / 24389.f)) ?
				(float) Math.cbrt(t) : (841.f / 108.f * t + 4.f / 29.f));
	}

	static float g(float K) {
		if(K > 0.04045f) {
			return (float) Math.pow((K + 0.055f) / 1.055f, 2.4f);
		} else {
			return K / 12.92f;
		}
	}

	public static float[] rgb2lab(float[] rgb, float[] lab) {
		// fixme
		// clamp values


		// Convert to sRGB
		float r0 = g(rgb[0]);
		float g0 = g(rgb[1]);
		float b0 = g(rgb[2]);

		// see http://www.brucelindbloom.com/index.html?Eqn_RGB_XYZ_Matrix.html
		float X = 0.4124564f * r0 + 0.3575761f * g0 + 0.1804375f * b0;
		float Y = 0.2126729f * r0 + 0.7151522f * g0 + 0.0721750f * b0;
		float Z = 0.0193339f * r0 + 0.1191920f * g0 + 0.9503041f * b0;

		float fx = f(X / 0.9505f);
		float fy = f(Y / 1.f);
		float fz = f(Z / 1.0890f);

		lab[0] = 116.f * fy - 16.f;
		lab[1] = 500.f * (fx - fy);
		lab[2] = 200.f * (fy - fz);
		lab[3] = rgb[3];

		return lab;
	}

	/**
	 * Returns the distance of these two colurs. It uses the lab colorspace
	 * @param color1
	 * @param color2
     * @return
     */
	public static float dist(int color1, int color2) {
		float[] rgb1 = int2rgb(color1);
		float[] rgb2 = int2rgb(color2);
		float[] lab1 = rgb2lab(rgb1, rgb1);
		float[] lab2 = rgb2lab(rgb2, rgb2);

		float dl = lab1[0] - lab2[0];
		float da = lab1[1] - lab2[1];
		float db = lab1[2] - lab2[2];

		return (float) Math.sqrt(dl * dl + da * da + db * db);
	}

	public static class ColorFormatException extends RuntimeException {
		public ColorFormatException(String msg) {
			super(msg);
		}
	}
}
