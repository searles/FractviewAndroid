package at.searles.fractview.ui.editors;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;

/**
 * General-purpose interface to edit certain values
 */
public abstract class SettingsEditor<A> implements Parcelable {

	String title;
	int layout;

	protected SettingsEditor(String title, int layout) {
		this.title = title;
		this.layout = layout;
	}

	// the following two are abstract
	// because eg in Adapter I refer to another editor
	// for these values.
	//public abstract void set(A value);
	public abstract A get();

	public abstract void init(View view); // initalize value and function to be called when editor is done.


	// dialog will only be closed if true is returned
	public abstract boolean apply(View view);

	public String getTitle() {
		return title;
	}

	public int getLayout() {
		return layout;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	protected SettingsEditor(Parcel in) {
		this.title = in.readString();
		this.layout = in.readInt();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(title);
		dest.writeInt(layout);
	}

	public Dialog createDialog(Activity activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle(title);

		LayoutInflater inflater = activity.getLayoutInflater();

		final View view = inflater.inflate(this.layout, null);

		init(view);

		// Add action buttons
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				if (SettingsEditor.this.apply(view)) dialog.dismiss();
			}
		}) // will be modified later
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		builder.setView(view);

		return builder.show();
	}
}

// fixme:
/*
Some thoughts:
1. When the dialog is ok-closed, data fields should be updated in the calling program.
In fact, it should be done whenever apply is called with an ok-return value.

For this purpose, there are multiple possibilities.
Nice possibility: Write an adapter that wraps a settingseditor
 */