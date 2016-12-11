package at.searles.fractview.ui.editors;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import at.searles.fractview.R;
import at.searles.math.Cplx;

public class CplxEditor extends SettingsEditor<Cplx> {

	Cplx value;

	public CplxEditor(String title, Cplx value) {
		super(title, R.layout.cplx_layout);
		this.value = value;
	}

	@Override
	public void init(View view) {
		final EditText xEditText = (EditText) view.findViewById(R.id.xEditText);
		final EditText yEditText = (EditText) view.findViewById(R.id.yEditText);

		xEditText.setText(Double.toString(value.re()));
		yEditText.setText(Double.toString(value.im()));
	}

	@Override
	public Cplx get() {
		return value;
	}

	@Override
	public boolean apply(View view) {
		EditText xEdit = (EditText) view.findViewById(R.id.xEditText);
		EditText yEdit = (EditText) view.findViewById(R.id.yEditText);

		try {
			double re = Double.parseDouble(xEdit.getText().toString());
			double im = Double.parseDouble(yEdit.getText().toString());

			return ((Callback) (view.getContext())).applyCplx(new Cplx(re, im));
		} catch(NumberFormatException e) {
			Toast.makeText(view.getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			return false;
		}
	}

	public interface Callback {
		boolean applyCplx(Cplx c);
	}

	public CplxEditor(Parcel in) {
		super(in);
		double re = in.readDouble();
		double im = in.readDouble();

		this.value = new Cplx(re, im);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeDouble(value.re());
		dest.writeDouble(value.im());
	}

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public CplxEditor createFromParcel(Parcel in) {
			return new CplxEditor(in);
		}

		public CplxEditor[] newArray(int size) {
			return new CplxEditor[size];
		}
	};
}
