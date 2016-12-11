package at.searles.fractview.ui.editors;

import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.util.Size;
import android.view.View;
import android.widget.*;
import at.searles.fractview.MainActivity;
import at.searles.fractview.R;

/**
 * Preference for setting size of image.
 * It consists of two edit-texts that can hold
 * the widthBox and the heightBox.
 *
 * If the size is too big (hence causing an OutOfMem)
 * it will be reduced. Yet, testing for this is needed!
 */
public class ImageSizeEditor extends SettingsEditor<Size> {

	int width;
	int height;
	boolean setAsDefault = false;

	public ImageSizeEditor(String title, int width, int height) {
		super(title, R.layout.image_size_editor);
		this.width = width;
		this.height = height;
	}

	@Override
	public void init(View view) {
		final EditText widthEdit = (EditText) view.findViewById(R.id.widthEditText);
		final EditText heightEdit = (EditText) view.findViewById(R.id.heightEditText);
		CheckBox defaultCheckBox = (CheckBox) view.findViewById(R.id.defaultCheckBox);
		Button resetButton = (Button) view.findViewById(R.id.resetSizeButton);

		widthEdit.setText(Integer.toString(width));
		heightEdit.setText(Integer.toString(height));

		// listener to button
		resetButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(view.getContext());

				Point dim = new Point();

				dim.set(prefs.getInt("width", -1), prefs.getInt("height", -1));

				if(dim.x <= 0 || dim.y <= 0) {
					dim = MainActivity.screenDimensions(view.getContext());
				}

				widthEdit.setText(Integer.toString(dim.x));
				heightEdit.setText(Integer.toString(dim.y));
			}
		});

		defaultCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean state) {
				setAsDefault = state;
			}
		});
	}

	@Override
	public Size get() {
		return new Size(width, height);
	}

	public boolean setAsDefault() {
		return setAsDefault;
	}

	/*@Override
	public void set(Size size) {
		this.width = size.getWidth();
		this.height = size.getHeight();
	}*/

	@Override
	public boolean apply(View view) {
		// fixme put this partly into an adapter inside activity.

		final EditText widthEdit = (EditText) view.findViewById(R.id.widthEditText);
		final EditText heightEdit = (EditText) view.findViewById(R.id.heightEditText);
		CheckBox defaultCheckBox = (CheckBox) view.findViewById(R.id.defaultCheckBox);
		// Button resetButton = (Button) view.findViewById(R.id.resetSizeButton);

		//final BitmapFragment bf = (BitmapFragment) fm.findFragmentByTag(activity.getString(R.string.bitmap_main));

		try {
			// Read data from text-edits
			int width = Integer.parseInt(widthEdit.getText().toString());
			int height = Integer.parseInt(heightEdit.getText().toString());

			if (width < 1 || height < 1) throw new NumberFormatException("value must be > 0");

			return ((Callback) view.getContext()).applyImageSize(width, height, defaultCheckBox.isChecked());
		} catch(NumberFormatException e) {
			Toast.makeText(view.getContext(), "Not a number", Toast.LENGTH_LONG).show();
			return false;
		}
	}

	public static interface Callback {
		boolean applyImageSize(int width, int height, boolean setAsDefault);
	}
}
