package at.searles.fractview.ui.editors;

import android.content.Context;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.MutableBoolean;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import at.searles.fractview.R;
import at.searles.math.color.Colors;

public class ColorEditorView {

	// here, store a value that can be easily obtained later.
	/*private int color;

	public ColorEditorView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void init(View view) {

		// fixme: initialize with color

		final ColorView colorView = (ColorView) view.findViewById(R.id.colorView);
		final EditText webcolorEditor = (EditText) view.findViewById(R.id.webcolorEditText);

		colorView.setColor(this.color);
		webcolorEditor.setText(Colors.toColorString(this.color));

		final MutableBoolean updateView = new MutableBoolean(true);

		webcolorEditor.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				String cs = s.toString().trim();

				try {
					ColorEditor.this.color = Color.parseColor(cs);

					// update unless it is called from colorView-listener
					if(updateView.value) colorView.setColor(color);
				} catch (IllegalArgumentException e) {
					// not a valid color
				} catch (StringIndexOutOfBoundsException android_bug) {
					// fixme bug in android - really annoying one, makes
					// me wonder about the quality of google'string android
					// code...
				}
			}
		});



		colorView.setListener(new ColorView.ColorListener() {
			@Override
			public void onColorChanged(int color) {
				ColorEditor.this.color = color;
				updateView.value = false; // to avoid that the edittext listener sets the color
				webcolorEditor.setText(Colors.toColorString(color));
				updateView.value = true;
			}
		});
	}

	/*@Override
	public void set(Integer color) {
		this.color = color;
	}*

	@Override
	public Integer get() {
		return color | 0xff000000; // fixme alpha some other way
	}

	@Override
	public boolean apply(View view) {
		EditText editor = (EditText) view.findViewById(R.id.webcolorEditText);
		String colorString = editor.getText().toString();

		try {
			int color = Color.parseColor(colorString);
			return ((Callback) view.getContext()).applyColor(color);
		} catch(IllegalArgumentException e) {
			Toast.makeText(view.getContext(), "Invalid Color", Toast.LENGTH_LONG).show();
			return false;
		}
	}

	public static interface Callback {
		boolean applyColor(int color);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeInt(this.color);
	}

	ColorEditor(Parcel in) {
		super(in);
		this.color = in.readInt();
	}

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public ColorEditor createFromParcel(Parcel in) {
			return new ColorEditor(in);
		}

		public ColorEditor[] newArray(int size) {
			return new ColorEditor[size];
		}
	};*/
}
