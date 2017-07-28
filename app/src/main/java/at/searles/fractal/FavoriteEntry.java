package at.searles.fractal;


import android.graphics.Bitmap;

/**
 * Use cases:
 * For Favorites. In order to also use this class outside of Android, the icon is stored 
 * as binary png data.
 *
 */
public class FavoriteEntry {

	private Fractal fractal;
	private Bitmap icon;  // icon may be null
	private String description; // description may be null

	/**
	 * The title is set via the key in the mapping
	 */
	private transient String title;

	public FavoriteEntry(Bitmap icon, Fractal fractal, String description) {
		this.fractal = fractal;
		this.icon = icon;
		this.description = description;
	}

	public String title() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
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
