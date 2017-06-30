package at.searles.fractview.fractal;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;

import at.searles.fractview.FractalLabel;

/**
 *
 */
public class FavoriteEntry implements FractalLabel {

	private static final int ICON_LEN = 64;

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

		Matrix transformation = new Matrix();
		transformation.setValues(new float[]{
				scale, 0, (ICON_LEN - scale * w) * .5f,
				0, scale, (ICON_LEN - scale * h) * .5f,
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
		byte[] decodedString = Base64.decode(str, Base64.DEFAULT);
		return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
	}

	public static FavoriteEntry create(String title, Fractal fractal, Bitmap bitmap) {
		return new FavoriteEntry(title, createIcon(bitmap), fractal);
	}

	public FavoriteEntry(String title, Bitmap icon, Fractal fractal) {
		this.title = title;
		this.fractal = fractal;
		this.icon = icon;
	}

	public static final String FRACTAL_LABEL = "fractal";
	public static final String ICON_LABEL = "icon";
	public static final String TITLE_LABEL = "title";

	public JsonObject serialize() {
		JsonObject obj = new JsonObject();

		obj.addProperty(TITLE_LABEL, title);
		obj.addProperty(ICON_LABEL, getStringFromBitmap(icon));
		obj.add(FRACTAL_LABEL, fractal.serialize());

		return obj;
	}

	/**
	 *
	 * @param title In an old version, the title was not stored, therefore this one is here to avoid null.
	 * @param e The Json
	 * @return
	 */
	public static FavoriteEntry deserialize(String title, JsonElement e) {
		JsonObject obj = (JsonObject) e;
		Fractal fractal = Fractal.deserialize(obj.get(FRACTAL_LABEL));
		Bitmap icon = getBitmapFromString(obj.get(ICON_LABEL).getAsString());

		JsonElement titleJson = obj.get(TITLE_LABEL);

		if(titleJson == null) {
			Log.d(FavoriteEntry.class.getName(), "no title was stored in Json");
		} else {
			title = titleJson.getAsString();
		}

		return new FavoriteEntry(title, icon, fractal);
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
		return "";
	}

	public Fractal fractal() {
		return fractal;
	}
}
