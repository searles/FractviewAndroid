package at.searles.fractview.ui.editors;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;
import at.searles.fractview.R;

public class ExprEditor extends SettingsEditor<String> {

	String value;

	public ExprEditor(String title, String value) {
		super(title, R.layout.string_editor);
		this.value = value;
	}

	@Override
	public void init(View view) {
		// nothing to do.
		final EditText editText = (EditText) view.findViewById(R.id.stringEditText);
		editText.setText(value);
	}

	//@Override
	//public void set(Integer value) {
	//	this.value = value;
	//}

	@Override
	public String get() {
		return value;
	}

	@Override
	public boolean apply(View view) {
		EditText editText = (EditText) view.findViewById(R.id.stringEditText);
		String data = editText.getText().toString();

		return ((Callback) view.getContext()).applyExpr(data);
	}

	public static interface Callback {
		boolean applyExpr(String data);
	}

	public ExprEditor(Parcel in) {
		super(in);
		this.value = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(value);
	}

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public ExprEditor createFromParcel(Parcel in) {
			return new ExprEditor(in);
		}

		public ExprEditor[] newArray(int size) {
			return new ExprEditor[size];
		}
	};
}
