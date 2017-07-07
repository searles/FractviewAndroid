package at.searles.fractal;


/**
 * Use cases:
 * For Favorites. In order to also use this class outside of Android, the icon is stored 
 * as binary png data.
 *
 */
public class FractalEntry {

	private Fractal fractal;
	private Bitmap icon;  // icon may be null
	private String title;       // title may be null
	private String description; // description may be null

	public FractalEntry(String title, Bitmap icon, Fractal fractal, String description) {
		this.fractal = fractal;
		this.title = title;
		this.icon = icon;
		this.description = description;
	}

	public String title() {
		return title;
	}

	public Bitmap icon() {
		return icon;
	}

	public String description() {
		return description;
	}

	public Fractal fractal() {
		return fractal;
	}
}
