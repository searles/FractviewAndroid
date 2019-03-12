package at.searles.fractview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import at.searles.fractal.Fractal;
import at.searles.fractview.parameters.dialogs.LoadSharedPreferenceDialogFragment;
import at.searles.fractview.parameters.dialogs.SaveSharedPreferenceDialogFragment;
import at.searles.fractview.ui.DialogHelper;
import at.searles.meelan.MeelanException;

public class SourceEditorActivity extends Activity {

	/**
	 * Shared preferences that contains programs
	 */
	static public final String PREFS_NAME = "SavedPrograms";

    static public final String SOURCE_LABEL = "acceptedSource";

	static private final int LOAD_PROGRAM = -1;
	static private final int SAVE_PROGRAM = -2;
	public static final int SOURCE_EDITOR_ACTIVITY_RETURN = 98;
    public static final String ID_LABEL = "id";
	private static final String DIALOG_TAG = "dialogTag";

	private EditText editor;
    
	private String acceptedSource;

	private int id;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.source_editor);

		// fetch program from intent
		this.acceptedSource = getIntent().getStringExtra(SOURCE_LABEL);
		this.id = getIntent().getIntExtra(ID_LABEL, -1);

		editor = findViewById(R.id.sourceEditText);
		editor.setHorizontallyScrolling(true); // XXX Android Bug: the attribute in the xml is ignored

		// set text in editor
		editor.setText(this.acceptedSource);

		editor.addTextChangedListener(new TextWatcher() {
			// if the text is changed we set program to null to see whether we need fresh compiling
			@Override
			public void afterTextChanged(Editable s) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				acceptedSource = null;
			}
		});

		Button okButton = findViewById(R.id.okButton);
		Button cancelButton = findViewById(R.id.cancelButton);

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// don't do anything.
				finish();
			}
		});

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(checkCompile()) {
					Intent data = new Intent();
                    
					data.putExtra(SOURCE_LABEL, acceptedSource);
					data.putExtra(ID_LABEL, id);
					setResult(1, data);
					finish();
				}
			}
		});
	}

	private boolean checkCompile() {
		String source = currentSource();

		try {
			Fractal.fromSource(source);
			acceptedSource = source;

			return true;
		} catch(MeelanException e) {
			DialogHelper.error(this, "Compiler Error: " + e.toString());
			// TODO improve meelan exception
		} catch(Throwable e) {
			// TODO
			DialogHelper.error(this, "Compiler Error: " + e.getMessage());
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_source_editor, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_load_program: {
				// Load from shared prefs, only one textfile
				// export can only export one file.
				LoadSharedPreferenceDialogFragment ft = LoadSharedPreferenceDialogFragment.newInstance(PREFS_NAME);
				ft.show(getFragmentManager(), DIALOG_TAG);
			} return true;
			case R.id.action_save_program: {
				SaveSharedPreferenceDialogFragment ft = SaveSharedPreferenceDialogFragment.newInstance(PREFS_NAME);
				ft.show(getFragmentManager(), DIALOG_TAG);
			} return true;
			case R.id.action_compile: {
				if(checkCompile()) {
					DialogHelper.info(this, "Program compiled without errors");
				}
			} return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private String currentSource() {
		// unchecked acceptedSource code
		return editor.getText().toString();
	}

//	@Override
//	public void apply(int requestCode, Object o) {
//		switch(requestCode) {
//			case SAVE_PROGRAM: {
//				Log.d(getClass().getName(), "Saving program " + o);
//				String name = (String) o;
//				SharedPrefsHelper.storeInSharedPreferences(this, name, currentSource(), PREFS_NAME);
//			} break;
//			case LOAD_PROGRAM: {
//				String name = (String) o;
//
//				acceptedSource = SharedPrefsHelper.loadFromSharedPreferences(this, name, PREFS_NAME);
//				editor.setText(acceptedSource);
//			} break;
//			default:
//				throw new IllegalArgumentException("Bad call to checkCompile, code: " + requestCode);
//		}
// todo	}
}
