package at.searles.fractview.ui.editors;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import at.searles.fractview.R;
import at.searles.fractview.fractal.Adapters;
import at.searles.math.Scale;

public class ScaleEditor extends SettingsEditor<Scale> {

	Scale scale;

	public ScaleEditor(String title, Scale scale) {
		super(title, R.layout.scale_editor);
		this.scale = scale;
	}

	/*@Override
	public void set(Scale scale) {
		this.scale = scale;
	}*/

	@Override
	public Scale get() {
		return scale;
	}

	@Override
	public void init(final View view) {
		final EditText xxEditText = (EditText) view.findViewById(R.id.xxEditText);
		final EditText xyEditText = (EditText) view.findViewById(R.id.xyEditText);
		final EditText yxEditText = (EditText) view.findViewById(R.id.yxEditText);
		final EditText yyEditText = (EditText) view.findViewById(R.id.yyEditText);
		final EditText cxEditText = (EditText) view.findViewById(R.id.cxEditText);
		final EditText cyEditText = (EditText) view.findViewById(R.id.cyEditText);

		xxEditText.setText(Double.toString(scale.xx));
		xyEditText.setText(Double.toString(scale.xy));
		yxEditText.setText(Double.toString(scale.yx));
		yyEditText.setText(Double.toString(scale.yy));
		cxEditText.setText(Double.toString(scale.cx));
		cyEditText.setText(Double.toString(scale.cy));
	}

	@Override
	public boolean apply(View view) {
		EditText xxEditText = (EditText) view.findViewById(R.id.xxEditText);
		EditText xyEditText = (EditText) view.findViewById(R.id.xyEditText);
		EditText yxEditText = (EditText) view.findViewById(R.id.yxEditText);
		EditText yyEditText = (EditText) view.findViewById(R.id.yyEditText);
		EditText cxEditText = (EditText) view.findViewById(R.id.cxEditText);
		EditText cyEditText = (EditText) view.findViewById(R.id.cyEditText);

		try {
			final double xx = Double.parseDouble(xxEditText.getText().toString());
			final double xy = Double.parseDouble(xyEditText.getText().toString());
			final double yx = Double.parseDouble(yxEditText.getText().toString());
			final double yy = Double.parseDouble(yyEditText.getText().toString());
			final double cx = Double.parseDouble(cxEditText.getText().toString());
			final double cy = Double.parseDouble(cyEditText.getText().toString());

			Scale scale = new Scale(xx, xy, yx, yy, cx, cy);

			return ((Callback) view.getContext()).applyScale(scale);
		} catch(NumberFormatException e) {
			Toast.makeText(view.getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			return false;
		}
	}

	public static interface Callback {
		boolean applyScale(Scale sc);
	}

	public ScaleEditor(Parcel in) {
		super(in);
		this.scale = Adapters.scaleAdapter.fromParcel(in);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		Adapters.scaleAdapter.toParcel(scale, dest, flags);
	}

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public ScaleEditor createFromParcel(Parcel in) {
			return new ScaleEditor(in);
		}

		public ScaleEditor[] newArray(int size) {
			return new ScaleEditor[size];
		}
	};
}
