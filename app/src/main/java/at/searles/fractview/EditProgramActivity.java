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
import android.widget.Toast;

import java.util.HashMap;

import at.searles.fractview.editors.EditableDialogFragment;
import at.searles.fractview.fractal.Fractal;
import at.searles.meelan.CompileException;
import at.searles.parsing.ParsingError;

public class EditProgramActivity extends Activity implements EditableDialogFragment.Callback {

	// TODO: Load from sample!

	public static final String PREFS_NAME = "SavedPrograms";
	EditText editor;

	String source; // if source is null, sth changed...

	private static final int LOAD_PROGRAM = -1;
	private static final int SAVE_PROGRAM = -2;

	private SharedPrefsHelper prefsHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		prefsHelper = new SharedPrefsHelper(this, PREFS_NAME);

		setContentView(R.layout.program_layout);

		// fetch program from intent
		source = getIntent().getExtras().getString("source");

		editor = (EditText) findViewById(R.id.programEditText);
		editor.setHorizontallyScrolling(true); // fixme Android Bug: the attribute in the xml is ignored

		// set text in editor
		editor.setText(source);

		editor.addTextChangedListener(new TextWatcher() {
			// if the text is changed we set program to null to see whether we need fresh compiling
			@Override
			public void afterTextChanged(Editable s) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				source = null;
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
				if(source != null || apply()) {
					Intent data = new Intent();
					data.putExtra("source", source);
					setResult(1, data);
					finish();
				}
			}
		});

	}

	boolean apply() {
		// if there is an error, store it.
		ParsingError parsingError = null;
		CompileException compilerException = null;

		source = editor.getText().toString();

		try {
			Fractal fractal = new Fractal(AssetsHelper.DEFAULT_SCALE, source, new HashMap<>());
			fractal.parse(); // Check whether parsing works
			fractal.compile(); // Also checks default-values for external data.
			return true;
		} catch(ParsingError e) {
			parsingError = e;
		} catch(CompileException e) {
			compilerException = e;
		}

		/*SpannableString span = new SpannableString(source);

		// red background with 25% coverage
		int bgColor = Color.argb(64, 255, 128, 128);
		*/
		int cursor;

		// there must have been an exception
		if(compilerException != null) {
			// set underline of length at least 1.
			//Log.d("PE", start + " and end is " + end);
			//span.setSpan(new BackgroundColorSpan(bgColor), start, end, 0);

			// FIXME
			// FIXME
			// FIXME
			// FIXME
			// FIXME
			// FIXME
			// FIXME
			cursor = 0; // compilerException.topPosition();

			Toast.makeText(this, "Compiler Error: " + compilerException.getMessage(), Toast.LENGTH_LONG).show();
			compilerException.printStackTrace();
		} else {
			// it is a parsing error
			cursor = parsingError.pos();

			Toast.makeText(this, "Parsing Error: " + parsingError.getMessage(), Toast.LENGTH_LONG).show();
			parsingError.printStackTrace();
		}

		if(cursor >= 0) {
			editor.setSelection(cursor, cursor);
		}

		// todo next line needed?
		source = null;

		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.program_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_load_program: {
				EditableDialogFragment ft = EditableDialogFragment.newInstance(LOAD_PROGRAM,
						"Load Program", false, EditableDialogFragment.Type.LoadSharedPref);
				ft.show(getFragmentManager(), "dialog");
				ft.getArguments().putString("prefs_name", PREFS_NAME);
			} return true;
			case R.id.action_save_program: {
				EditableDialogFragment ft = EditableDialogFragment.newInstance(SAVE_PROGRAM,
						"Save Program", false, EditableDialogFragment.Type.SaveSharedPref);
				ft.show(getFragmentManager(), "dialog");
				ft.getArguments().putString("prefs_name", PREFS_NAME);
			} return true;
			case R.id.action_compile: {
				if(apply()) {
					Toast.makeText(this, "Program compiled without errors", Toast.LENGTH_SHORT).show();
					// [done?] fixme if successful, don't overwrite it in the end.
				}
			} return true;
			//case R.id.action_presets: {
				// open dialog with possible pre-selections
				/*AlertDialog.Builder builder = new AlertDialog.Builder(this);

				String[] presetIds = new String[PresetFractals.PRESETS.length];

				// FIXME
				// FIXME
				// FIXME
				// FIXME
				// FIXME
				String mb = PresetFractals.readFromAsset(this, "Mandelbrot.fv");
				// FIXME
				// FIXME
				// FIXME
				// FIXME
				// FIXME
				if(mb != null) PresetFractals.PRESETS[0][1] = mb;

				for(int i = 0; i < PresetFractals.PRESETS.length; ++i) {
					presetIds[i] = PresetFractals.PRESETS[i][0];
				}

				builder.setItems(
						presetIds,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int index) {
								editor.setText(PresetFractals.PRESETS[index][1]);
								dialog.dismiss();
							}
						}
				);

				builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

				builder.show();*/
			//}
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void apply(int requestCode, Object o) {
		switch(requestCode) {
			case SAVE_PROGRAM: {
				String name = (String) o;
				prefsHelper.add(name, source, SharedPrefsHelper.SaveMethod.FindNext);
			} break;
			case LOAD_PROGRAM: {
				String name = (String) o;
				source = prefsHelper.get(name);
				editor.setText(source);
			} break;
			default:
				throw new IllegalArgumentException("Bad call to apply, code: " + requestCode);
		}
	}
}