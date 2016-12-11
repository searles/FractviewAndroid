package at.searles.fractview.ui.editors;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import at.searles.fractview.R;

public class RealEditor extends SettingsEditor<Double> {

	// this one is just used for init.
	double value;

	public RealEditor(String title, double value) {
		super(title, R.layout.real_editor);
		this.value = value;
	}

	@Override
	public void init(View view) {
		Log.d("IE", "init real");
		final EditText editText = (EditText) view.findViewById(R.id.realEditText);
		editText.setText(Double.toString(value));
	}

	/*@Override
	public void set(Double d) {
		this.d = d;
	}*/

	@Override
	public Double get() {
		return value;
	}

	@Override
	public boolean apply(View view) {
		EditText realEdit = (EditText) view.findViewById(R.id.realEditText);

		try {
			double d = Double.parseDouble(realEdit.getText().toString());
			return ((Callback) view.getContext()).applyReal(d);
		} catch(NumberFormatException e) {
			Toast.makeText(view.getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			return false;
		}
	}

	public static interface Callback {
		boolean applyReal(double d);
	}

	public RealEditor(Parcel in) {
		super(in);
		this.value = in.readDouble();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeDouble(value);
	}

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public RealEditor createFromParcel(Parcel in) {
			return new RealEditor(in);
		}

		public RealEditor[] newArray(int size) {
			return new RealEditor[size];
		}
	};
}
