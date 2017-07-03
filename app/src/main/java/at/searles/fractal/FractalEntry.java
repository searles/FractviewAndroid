package at.searles.fractal;


/**
 * Use cases:
 * * Presets
 *     + Set source but keep data
 *
 */
public class FractalEntry {

	private Fractal fractal;
	private byte[] iconBinary;  // icon may be null
	private String title;       // title may be null
	private String description; // description may be null

	public FractalEntry(String title, byte[] iconBinary, Fractal fractal, String description) {
		this.fractal = fractal;
		this.title = title;
		this.iconBinary = iconBinary;
		this.description = description;
	}

	public String title() {
		return title;
	}

	public byte[] icon() {
		return iconBinary;
	}

	public String description() {
		return description;
	}

	public Fractal fractal() {
		return fractal;
	}
}
