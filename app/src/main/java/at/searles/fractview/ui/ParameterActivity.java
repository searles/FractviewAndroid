package at.searles.fractview.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import at.searles.fractview.R;
import at.searles.fractview.fractal.DefaultData.Type;
import at.searles.fractview.fractal.Fractal;
import at.searles.fractview.fractal.PresetFractals;
import at.searles.fractview.ui.editors.*;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.CompileException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;

public class ParameterActivity extends Activity implements ScaleEditor.Callback, IntEditor.Callback, RealEditor.Callback, CplxEditor.Callback, ColorEditor.Callback, ExprEditor.Callback {

	public static final int PROGRAM_ACTIVITY_RETURN = 100;

	enum ParameterType { /* Label, */ Scale, Bool, Int, Real, Cplx, Color, Expr, Palette };

	static final CharSequence[] scaleOptions = { "Reset to Default", "Center on Origin", "Orthogonalize", "Straighten" };
	static final CharSequence[] boolOptions = { "Reset to Default" };
	static final CharSequence[] intOptions = { "Reset to Default" };
	static final CharSequence[] realOptions = { "Reset to Default" };
	static final CharSequence[] cplxOptions = { "Reset to Default", "Set to Center" };
	static final CharSequence[] colorOptions = { "Reset to Default" };
	static final CharSequence[] exprOptions = { "Reset to Default" };
	static final CharSequence[] paletteOptions = { "Reset to Default" };

	static final int BOOL = 0;
	static final int ELEMENT = 1;
	static final int SCALE = 2;

	static class ParameterElements {

		final ParameterType t;
		// FIXME do I need this? final int index; //
		final String label;

		ParameterElements(ParameterType t, String label) {
			this.t = t;
			this.label = label;
		}
	}

	Fractal fb;
	ParameterAdapter adapter; // List adapter for parameters
	ArrayList<ParameterElements> elements;
	ListView listView;

	// if something is edited, here is the information once the dialog calls this activities' confirm method
	private String currentEditId = null;


	@Override
	public boolean applyScale(Scale sc) {
		fb.setScale(sc);
		adapter.notifyDataSetChanged();
		return true;
	}

	@Override
	public boolean applyInt(int i) {
		fb.setInt(currentEditId, i);
		adapter.notifyDataSetChanged();
		return true;
	}

	@Override
	public boolean applyReal(double d) {
		fb.setReal(currentEditId, d);
		adapter.notifyDataSetChanged();
		return true;
	}

	@Override
	public boolean applyColor(int color) {
		fb.setColor(currentEditId, color);
		adapter.notifyDataSetChanged();
		return true;
	}

	@Override
	public boolean applyCplx(Cplx c) {
		fb.setCplx(currentEditId, c);
		adapter.notifyDataSetChanged();
		return true;
	}

	@Override
	public boolean applyExpr(String data) {
		// store id in case of error
		String resetValue = fb.isDefaultValue(currentEditId) ? null : fb.expr(currentEditId);

		try {
			fb.setExpr(currentEditId, data);
			fb.compile();

			adapter.notifyDataSetChanged();
			return true;
		} catch(CompileException e) { // this includes parsing exceptions now
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

			if (resetValue == null) {
				fb.resetExpr(currentEditId);
			} else {
				fb.setExpr(currentEditId, resetValue);
			}

			return false;
		}
	}

	private void showOptionsDialog(CharSequence[] options, DialogInterface.OnClickListener listener) {
		// show these simple dialogs to reset or center values.
		AlertDialog.Builder builder = new AlertDialog.Builder(ParameterActivity.this);

		builder.setTitle("Select an Option");
		builder.setItems(options, listener);

		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		builder.show();
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.parameter_layout);

		if(savedInstanceState == null) {
			Intent intent = getIntent();
			this.fb = intent.getParcelableExtra("fractal");
		} else {
			this.fb = savedInstanceState.getParcelable("fractal");
			this.currentEditId = savedInstanceState.getString("edit_id");
		}
		// need to extract all external values from FB. Hence parse it [compiling not necessary]

		try {
			this.fb.parse();
		} catch (CompileException e) {
			throw new IllegalArgumentException("could not compile fractal: " + e.getMessage() + ". this is a bug.");
		}

		adapter = new ParameterAdapter(this);

		listView = (ListView) findViewById(R.id.parameterListView);

		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
				final ParameterElements p = elements.get(position);

				currentEditId = p.label;

				switch(p.t) {
					/*case Label: {
						// this should not happen...
					} return;*/
					case Scale: {
						Scale value = fb.scale();

						DialogEditFragment.createDialog(ParameterActivity.this,
								new ScaleEditor("Scale", value));
					} return;
					case Expr: {
						String value = fb.expr(p.label);

						DialogEditFragment.createDialog(ParameterActivity.this,
								new ExprEditor(p.label, value));
					} return;
					case Bool: {
						boolean newValue = !fb.bool(p.label);

						fb.setBool(p.label, newValue);
						((CheckedTextView) view).setChecked(newValue);

						adapter.notifyDataSetChanged();

						return;
					}
					case Int: {
						int value = fb.intVal(p.label);

						DialogEditFragment.createDialog(ParameterActivity.this,
							new IntEditor(p.label, value));
					} return;
					case Real: {
						double value = fb.real(p.label);

						DialogEditFragment.createDialog(ParameterActivity.this,
							new RealEditor(p.label, value));
					}
					return;
					case Cplx: {
						Cplx value = fb.cplx(p.label);

						DialogEditFragment.createDialog(ParameterActivity.this,
							new CplxEditor(p.label, value));
					}
					return;
					case Color: {
						int value = fb.color(p.label);

						DialogEditFragment.createDialog(ParameterActivity.this,
							new ColorEditor(p.label, value));
					}
					return;
					case Palette: {
						Palette value = fb.palette(p.label);

						Intent i = new Intent(ParameterActivity.this, PaletteActivity.class);

						i.putExtra("palette", new PaletteActivity.PaletteWrapper(/*p.label,*/ value));
						startActivityForResult(i, PaletteActivity.PALETTE_ACTIVITY_RETURN);
					}
					return;
				}
			}
		});

		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
				final ParameterElements p = elements.get(position);

				switch(p.t) {
					case Scale: {
						showOptionsDialog(scaleOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Scale original = fb.scale();
								switch(which) {
									case 0: // Reset
										fb.setScale(PresetFractals.INIT_SCALE);
										break;
									case 1: // Origin
										fb.setScale(new Scale(original.xx, original.xy, original.yx, original.yy, 0, 0));
										break;
									case 2: // Ratio

										// Step 1: make x/y-vectors same length
										double xx = original.xx;
										double xy = original.xy;

										double yx = original.yx;
										double yy = original.yy;

										double lenx = Math.sqrt(xx * xx + xy * xy);
										double leny = Math.sqrt(yx * yx + yy * yy);

										double mlen = Math.max(lenx, leny);

										xx *= mlen / lenx;
										xy *= mlen / lenx;
										yx *= mlen / leny;
										yy *= mlen / leny;

										double vx = (xx + yx) / 2;
										double vy = (xy + yy) / 2;

										double ax = vx + vy;
										double ay = vx - vy;

										// fixme find proper orientation
										fb.setScale(new Scale(ax, ay, -ay, ax, original.cx, original.cy));

										break;
									case 3: // Straighten
										double xlen = Math.hypot(original.xx, original.xy);
										double ylen = Math.hypot(original.yx, original.yy);
										fb.setScale(new Scale(xlen, 0, 0, ylen, original.cx, original.cy));
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
					case Expr: {
						showOptionsDialog(exprOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which) {
									case 0: // Reset
										fb.resetExpr(p.label);
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});
						return true;
					}
					case Bool: {
						showOptionsDialog(boolOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which) {
									case 0: // Reset
										fb.resetBool(p.label);
										// must update it in the interface
										((CheckedTextView) view).setChecked(fb.bool(p.label));
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});
						return true;
					}
					case Int: {
						showOptionsDialog(intOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which) {
									case 0: // Reset
										fb.resetInt(p.label);
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});
						return true;
					}
					case Real: {
						showOptionsDialog(realOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which) {
									case 0: // Reset
										fb.resetReal(p.label);
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
					case Cplx: {
						showOptionsDialog(cplxOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which) {
									case 0: // Reset
										fb.resetCplx(p.label);
										break;
									case 1: // Center
										fb.setCplx(p.label, new Cplx(fb.scale().cx, fb.scale().cy));
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
					case Color: {
						showOptionsDialog(colorOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which) {
									case 0: // Reset
										fb.resetColor(p.label);
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
					case Palette: {
						showOptionsDialog(paletteOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which) {
									case 0: // Reset
										fb.resetPalette(p.label);
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
				}
				// edit value

				return false;
			}
		});

		Button okButton = (Button) findViewById(R.id.okButton);
		Button cancelButton = (Button) findViewById(R.id.cancelButton);

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setResult(0);
				finish();
			}
		});

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent data = new Intent();
				data.putExtra("parameters", fb);
				setResult(1, data);
				finish();
			}
		});
	}

	@Override
	public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
		// Save the user's current game state
		savedInstanceState.putParcelable("fractal", fb);
		savedInstanceState.putString("edit_id", currentEditId);

		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	void setNewSourceCode(String sourceCode) {
		try {
			// in first attempt preserve arguments
			final Fractal newFractal = fb.copyNewSource(sourceCode, true);

			newFractal.parse();
			newFractal.compile();

			this.fb = newFractal;
			adapter.init();
			adapter.notifyDataSetChanged();

			Toast.makeText(this, "Keeping parameters", Toast.LENGTH_SHORT).show();
		} catch(CompileException e) {
			Toast.makeText(this, "Resetting parameters due to errors: " + e.getMessage(), Toast.LENGTH_LONG).show();

			try {
				final Fractal newFractal = fb.copyNewSource(sourceCode, false);

				newFractal.parse();
				newFractal.compile();

				this.fb = newFractal;
				adapter.init();
				adapter.notifyDataSetChanged();
			} catch(CompileException e2) {
				e.printStackTrace();
				Toast.makeText(this, e2.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == PaletteActivity.PALETTE_ACTIVITY_RETURN) {
			if (resultCode == 1) { // = "Ok"
				PaletteActivity.PaletteWrapper wrapper = data.getParcelableExtra("palette");

				fb.setPalette(/*wrapper.label,*/currentEditId, wrapper.p);
				adapter.notifyDataSetChanged();
			}
		} else if (requestCode == PROGRAM_ACTIVITY_RETURN) {
			if (resultCode == 1) { // = "Ok"
				String sourceCode = data.getExtras().getString("source");
				setNewSourceCode(sourceCode);
			}

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.parameters_activity_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_reset: {
				// confirm that reset is what you want.
				AlertDialog.Builder builder = new AlertDialog.Builder(this);

				builder.setTitle("Reset Parameters to Defaults");
				builder.setMessage("Do you really want to reset all parameters to their default values?");
				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						fb.setScale(PresetFractals.INIT_SCALE);
						for(String id : fb.boolLabels()) fb.resetBool(id);
						for(String id : fb.exprLabels()) fb.resetExpr(id);
						for(String id : fb.intLabels()) fb.resetInt(id);
						for(String id : fb.realLabels()) fb.resetReal(id);
						for(String id : fb.cplxLabels()) fb.resetCplx(id);
						for(String id : fb.colorLabels()) fb.resetColor(id);
						for(String id : fb.paletteLabels()) fb.resetPalette(id);

						adapter.notifyDataSetChanged(); // something changed...
					}
				});

				builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						// do nothing.
					}
				});

				builder.show();

				return true;
			}
			case R.id.action_edit_source: {
				Intent i = new Intent(this, ProgramActivity.class);
				i.putExtra("source", this.fb.sourceCode());
				startActivityForResult(i, PROGRAM_ACTIVITY_RETURN);
			} return true;
		}

		return false;
	}

				// create new dialog with fractal editor in it.
				/*FractalEditor fractalEditor = new FractalEditor("Edit FractalBuilder");

				SettingsEditor<Fractal> editor = new SettingsEditor.Adapter<Fractal>(
						fractalEditor, new SettingsEditor.OnConfirmedListener<Fractal>() {
					@Override
					public boolean confirmed(final Fractal fractal) {
						bitmapFragment.edit(new Runnable() {
							@Override
							public void run() {
								bitmapFragment.fractalBuilder.set(fractal);
							}
						}, "edit"); // FIXME String
						return true;
					}
				});

				editor.set(bitmapFragment.fractal());
				DialogEditFragment.createDialog(this, editor);*/

	class ParameterAdapter extends BaseAdapter implements ListAdapter {

		private final LayoutInflater inflater;

		ParameterAdapter(Context context) {
			inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
			// fill elements with elements :)
			elements = new ArrayList<>();
			init();
		}

		void init() {
			elements.clear();
			elements.add(new ParameterElements(ParameterType.Scale, "Scale"));

			for(Map.Entry<String, Type> entry : fb.parameterMap()) {
				ParameterType t;

				switch(entry.getValue()) {
					case BOOL: t = ParameterType.Bool; break;
					case EXPR: t = ParameterType.Expr; break;
					case INT: t = ParameterType.Int; break;
					case REAL: t = ParameterType.Real; break;
					case CPLX: t = ParameterType.Cplx; break;
					case COLOR: t = ParameterType.Color; break;
					case PALETTE: t = ParameterType.Palette; break;
					default:
						throw new IllegalArgumentException();
				}

				elements.add(new ParameterElements(t, entry.getKey()));
			}
		}

		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public int getCount() {
			return elements.size();
		}

		@Override
		public String getItem(int position) {
			return elements.get(position).label;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getViewTypeCount() {
			// labels + one for each type.
			// fixme more of them (should contain a preview/checkbox for bools)
			return 3;
		}

		@Override
		public int getItemViewType(int position) {
			ParameterType t = elements.get(position).t;
			return t == ParameterType.Bool ? BOOL : t == ParameterType.Scale ? SCALE : ELEMENT;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			int viewType = getItemViewType(position);

			ParameterElements e = elements.get(position);

			switch(viewType) {
				case BOOL: {
					if (view == null)
						view = inflater.inflate(android.R.layout.simple_list_item_checked, parent, false);

					CheckedTextView text1 = (CheckedTextView) view.findViewById(android.R.id.text1);

					if(!fb.isDefaultValue(e.label)) {
						//Log.d("PA", e.label + " is not default");
						text1.setTypeface(Typeface.DEFAULT_BOLD);
					} else {
						//Log.d("PA", e.label + " is default");
						text1.setTypeface(Typeface.DEFAULT);
					}

					text1.setText(e.label);
					text1.setChecked(fb.bool(e.label));
				} break;
				case ELEMENT: {
					if (view == null)
						view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
					TextView text1 = (TextView) view.findViewById(android.R.id.text1);
					text1.setText(e.label);

					// if not isDefaultValue set bold.
					if(!fb.isDefaultValue(e.label)) {
						//Log.d("PA", e.label + " is not default");
						text1.setTypeface(Typeface.DEFAULT_BOLD);
					} else {
						//Log.d("PA", e.label + " is default");
						text1.setTypeface(Typeface.DEFAULT);
					}

					TextView text2 = (TextView) view.findViewById(android.R.id.text2);
					switch(e.t) {
						case Expr: text2.setText("Expression"); break;
						case Int: text2.setText("Integer Number"); break;
						case Real: text2.setText("Real Number"); break;
						case Cplx: text2.setText("Complex Number"); break;
						case Color: text2.setText("Color"); break;
						case Palette: text2.setText("Palette"); break;
					}
				} break;
				case SCALE: {
					if (view == null)
						view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
					TextView text1 = (TextView) view.findViewById(android.R.id.text1);
					text1.setText(e.label);

					if(!fb.scale().equals(PresetFractals.INIT_SCALE)) {
						//Log.d("PA", e.label + " is not default");
						text1.setTypeface(Typeface.DEFAULT_BOLD);
					} else {
						//Log.d("PA", e.label + " is default");
						text1.setTypeface(Typeface.DEFAULT);
					}
				}
			}


			return view;
		}
	}
}