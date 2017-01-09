package at.searles.fractview.ui.editors;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import org.jetbrains.annotations.NotNull;

/*public class DialogEditFragment extends DialogFragment {

	View view;
	AlertDialog dialog;

	SettingsEditor<?> editor = null;

	public static void createDialog(Activity activity, final SettingsEditor<?> editor) {
		final DialogEditFragment fragment = new DialogEditFragment();

		Bundle bundle = new Bundle();
		bundle.putParcelable("editor", editor);
		fragment.setArguments(bundle);

		fragment.show(activity.getFragmentManager(), editor.getTitle()); // FIXME string (?)
	}

	public DialogEditFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);

		if(savedInstanceState != null) {
			this.editor = savedInstanceState.getParcelable("editor");
		} else {
			this.editor = getArguments().getParcelable("editor");
		}
	}

	@Override
	public void onSaveInstanceState(@NotNull Bundle outState) {
		outState.putParcelable("editor", editor);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
	 	super.onStart(); // calls "show" of dialog

		// change positive button so that it only dismisses dialog
		// if apply returns true.

		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (editor.apply(view)) dialog.dismiss();
			}
		});
	}

	@Override
	public void onDestroyView() {
		// fixme this is due to a bug in android
		if (getDialog() != null && getRetainInstance())
			getDialog().setDismissMessage(null);
		super.onDestroyView();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Log.d("DEF", "onCreateDialog");
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		LayoutInflater inflater = getActivity().getLayoutInflater();
		view = inflater.inflate(editor.getLayout(), null);

		editor.init(view);

		builder.setView(view)
				// Add action buttons
				.setPositiveButton("ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// empty because modified later.
					}
				}) // will be modified later
				.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						DialogEditFragment.this.getDialog().cancel();
					}
				});

		dialog = builder.create();
		dialog.setTitle(editor.getTitle());

		return dialog;
	}
}*/