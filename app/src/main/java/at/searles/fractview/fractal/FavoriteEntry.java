package at.searles.fractview.fractal;

import android.graphics.*;
import android.util.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import at.searles.fractview.FractalEntry;

/**
 *
 */
public class FavoriteEntry implements FractalEntry {

	static final int ICON_LEN = 64;

	private Fractal fractal;
	private Bitmap icon; // icon may be null!
	private String title;

	/**
	 * creates a new bitmap with size 64x64 containing the center of the current image
	 * @return
	 */
	private static Bitmap createIcon(Bitmap original) {
		// create a square icon. Should  only contain central square.
		Bitmap icon = Bitmap.createBitmap(ICON_LEN, ICON_LEN, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(icon);

		float scale = ((float) ICON_LEN) / Math.min(original.getWidth(), original.getHeight());

		float w = original.getWidth();
		float h = original.getHeight();

		float tx = (original.getWidth() * scale - ICON_LEN) / 2.f;
		float ty = (original.getHeight() * scale - ICON_LEN) / 2.f;

		Matrix transformation = new Matrix();
		transformation.setValues(new float[]{
				scale, 0, (ICON_LEN - scale * w) / 2.f,
				0, scale, (ICON_LEN - scale * h) / 2.f,
				0, 0, 1,
		});

		Paint paint = new Paint();
		paint.setFilterBitmap(true);

		canvas.drawBitmap(original, transformation, paint);

		return icon;
	}

	private static String getStringFromBitmap(Bitmap bitmapPicture) {
		// Thanks to http://mobile.cs.fsu.edu/converting-images-to-json-objects/
		String encodedImage;
		ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
		bitmapPicture.compress(Bitmap.CompressFormat.PNG, 100, byteArrayBitmapStream);
		byte[] b = byteArrayBitmapStream.toByteArray();
		encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
		return encodedImage;
	}

	/**
	 * Convert a Base64 encoded icon to a bitmap
	 * @param str The base64 encoded value
	 * @return
     */
	private static Bitmap getBitmapFromString(String str) {
		// Thanks to http://mobile.cs.fsu.edu/converting-images-to-json-objects/
		// FIXME check error case
		byte[] decodedString = Base64.decode(str, Base64.DEFAULT);
		return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
	}

	public static FavoriteEntry create(String title, Fractal fractal, Bitmap bitmap) {
		return new FavoriteEntry(title, fractal, createIcon(bitmap));
	}

	public FavoriteEntry(String title, Fractal fractal, Bitmap icon) {
		this.title = title;
		this.fractal = fractal;
		this.icon = icon;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("fractal", fractal.toJSON());
		obj.put("icon", getStringFromBitmap(icon));
		return obj;
	}

	public static FavoriteEntry fromJSON(String title, Object o) throws JSONException {
		if(o instanceof JSONObject) {
			JSONObject obj = (JSONObject) o;
			Fractal f = Fractal.fromJSON(obj.getJSONObject("fractal"));
			Bitmap bm = getBitmapFromString(obj.getString("icon"));

			return new FavoriteEntry(title, f, bm);
		}

		throw new JSONException("not a JSON-Object???");
	}

	@Override
	public String title() {
		return title;
	}

	@Override
	public Bitmap icon() {
		return icon;
	}

	@Override
	public String description() {
		// FIXME add some description like date.
		return "";
	}

	public Fractal fractal() {
		return fractal;
	}
}
