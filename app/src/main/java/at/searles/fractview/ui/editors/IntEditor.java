package at.searles.fractview.ui.editors;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import at.searles.fractview.R;

public class IntEditor extends SettingsEditor<Integer> {;

	int value;

	public IntEditor(String title, int value) {
		super(title, R.layout.int_editor);
		this.value = value;
	}

	@Override
	public void init(View view) {
		Log.d("IE", "init int");
		// nothing to do.
		final EditText editText = (EditText) view.findViewById(R.id.intEditText);
		editText.setText(Integer.toString(value));
	}

	//@Override
	//public void set(Integer value) {
	//	this.value = value;
	//}

	@Override
	public Integer get() {
		return value;
	}

	@Override
	public boolean apply(View view) {
		try {
			final EditText intEditText = (EditText) view.findViewById(R.id.intEditText);
			value = Integer.parseInt(intEditText.getText().toString().trim());

			return ((Callback) view.getContext()).applyInt(value);

			//return callback != null && callback.confirm(value);
		} catch(NumberFormatException e) {
			Toast.makeText(view.getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			return false;
		}
	}

	public interface Callback {
		boolean applyInt(int i);
	}

	public IntEditor(Parcel in) {
		super(in);
		this.value = in.readInt();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeInt(value);
	}

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public IntEditor createFromParcel(Parcel in) {
			return new IntEditor(in);
		}

		public IntEditor[] newArray(int size) {
			return new IntEditor[size];
		}
	};


}
