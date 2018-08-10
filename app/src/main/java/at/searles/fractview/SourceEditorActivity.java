package at.searles.fractview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import at.searles.fractal.Fractal;
import at.searles.fractal.data.Parameters;
import at.searles.fractview.ui.DialogHelper;

public class SourceEditorActivity extends Activity {

	/**
	 * Shared preferences that contains programs
	 */
	static public final String PREFS_NAME = "SavedPrograms";

    static public final String SOURCE_LABEL = "acceptedSource";

	static private final int LOAD_PROGRAM = -1;
	static private final int SAVE_PROGRAM = -2;
	public static final int SOURCE_EDITOR_ACTIVITY_RETURN = 98;

	private EditText editor;
    
	private String acceptedSource;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.program_layout);

		// fetch program from intent
		this.acceptedSource = getIntent().getStringExtra(SOURCE_LABEL);

		editor = (EditText) findViewById(R.id.programEditText);
		editor.setHorizontallyScrolling(true); // fixme Android Bug: the attribute in the xml is ignored

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

		Button okButton = (Button) findViewById(R.id.okButton);
		Button cancelButton = (Button) findViewById(R.id.cancelButton);

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
					setResult(1, data);
					finish();
				}
			}
		});
	}

	private boolean checkCompile() {
		String source = currentSource();

		try {
			Fractal fractal = Fractal.fromData(source, new Parameters());
			fractal.compile(); // Also checks default-values for external data.

			acceptedSource = source;

			return true;
		} catch(Throwable e) {
			// TODO
			DialogHelper.error(this, "Compiler Error: " + e.getMessage());
			e.printStackTrace();

			return false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_source_editor, menu);
		return super.onCreateOptionsMenu(menu);
	}

//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
//		switch (item.getItemId()) {
//			case R.id.action_load_program: {
//				EditableDialogFragment ft = EditableDialogFragment.newInstance(LOAD_PROGRAM,
//						"Load Program", false, EditableDialogFragment.Type.LoadSharedPref);
//				ft.show(getFragmentManager(), "dialog");
//				ft.getArguments().putString("prefs_name", PREFS_NAME);
//			} return true;
//			case R.id.action_save_program: {
//				EditableDialogFragment ft = EditableDialogFragment.newInstance(SAVE_PROGRAM,
//						"Save Program", false, EditableDialogFragment.Type.SaveSharedPref);
//				ft.show(getFragmentManager(), "dialog");
//				ft.getArguments().putString("prefs_name", PREFS_NAME);
//			} return true;
//			case R.id.action_compile: {
//				if(checkCompile()) {
//					DialogHelper.info(this, "Program compiled without errors");
//				}
//			} return true;
//			default:
//				return super.onOptionsItemSelected(item);
// todo		}
//	}

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
