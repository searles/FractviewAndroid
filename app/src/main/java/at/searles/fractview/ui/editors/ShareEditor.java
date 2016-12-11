package at.searles.fractview.ui.editors;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import at.searles.fractview.R;

public class ShareEditor extends SettingsEditor<String> {

	String filename;

	boolean allowSettingWallpaper = false;

	EditText filenameEditor;

	public ShareEditor(String title, String filename) {
		super(title, R.layout.share_editor);
		this.filename = filename;
	}

	public void allowSettingWallpaper(boolean allowSettingWallpaper) {
		this.allowSettingWallpaper = allowSettingWallpaper;
	}


	/** If we picked a bad name, a new one will be proposed
	 * @param value
	 */
	public void set(String value) {
		this.filename = value;
		updateFilename();
	}

	@Override
	public String get() {
		return filename;
	}

	void updateFilename() {
		if(filenameEditor != null) {
			filenameEditor.setText(filename);
		}
	}

	@Override
	public void init(View view) {
		filenameEditor = (EditText) view.findViewById(R.id.filenameEditText);
		filenameEditor.setText(filename);

		CheckBox wallpaperCheckBox = (CheckBox) view.findViewById(R.id.setWallpaperCheckBox);

		if(!allowSettingWallpaper) {
			wallpaperCheckBox.setEnabled(false);
			wallpaperCheckBox.setChecked(false);
		}
	}

	private static final String RESERVED_CHARS = "";

	@Override
	public boolean apply(View view) {
		EditText filenameEditor = (EditText) view.findViewById(R.id.filenameEditText);

		this.filename = filenameEditor.getText().toString();

		if(filename.isEmpty()) {
			Toast.makeText(view.getContext(), "Filename must not be empty", Toast.LENGTH_LONG).show();
			return false;
		} else {
			// sanitize
			filename = filename.replaceAll("[\\|\\\\?*<\":>/';]", "_");

			CheckBox shareCheckBox = (CheckBox) view.findViewById(R.id.shareCheckBox);
			CheckBox wallpaperCheckBox = (CheckBox) view.findViewById(R.id.setWallpaperCheckBox);

			// filename check should be done outside of this class using SettingsEditorAdapter.
			return ((Callback) view.getContext()).applySave(filename,
					shareCheckBox.isChecked(), wallpaperCheckBox.isChecked());
		}
	}

	public interface Callback {
		boolean applySave(String filename, boolean share, boolean setAsWallpaper);
	}

	public ShareEditor(Parcel in) {
		super(in);
		this.filename = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(filename);
	}

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public ShareEditor createFromParcel(Parcel in) {
			return new ShareEditor(in);
		}

		public ShareEditor[] newArray(int size) {
			return new ShareEditor[size];
		}
	};
}
