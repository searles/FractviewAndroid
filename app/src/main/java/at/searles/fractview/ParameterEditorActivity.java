package at.searles.fractview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import at.searles.fractal.Fractal;
import at.searles.fractal.android.BundleAdapter;
import at.searles.fractview.ui.DialogHelper;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.CompileException;
import at.searles.parsing.ParsingError;
import at.searles.utils.Pair;

public class ParameterEditorActivity extends Activity implements EditableDialogFragment.Callback {

	private static final CharSequence[] scaleOptions = { "Reset to Default", "Center on Origin", "Orthogonalize", "Straighten" };
	private static final CharSequence[] boolOptions = { "Reset to Default" };
	private static final CharSequence[] intOptions = { "Reset to Default" };
	private static final CharSequence[] realOptions = { "Reset to Default" };
	private static final CharSequence[] cplxOptions = { "Reset to Default", "Set to Center" };
	private static final CharSequence[] colorOptions = { "Reset to Default" };
	private static final CharSequence[] exprOptions = { "Reset to Default" };
	private static final CharSequence[] paletteOptions = { "Reset to Default" };

	// The following use different views (and listeners)
	private static final int BOOL = 0;
	private static final int ELEMENT = 1;

	private Fractal fractal;
	private ParameterAdapter adapter; // List adapter for parameters
	private ListView listView;

	@Override
	public void apply(int resourceCode, Object o) {
		// TODO Should this method move into the adapter?
		if(o == null) {
			// There was an error in the input
			Toast.makeText(this, "ERROR: Bad input", Toast.LENGTH_LONG).show();
			return;
		}

		// The resourceCode is the position in the element list
		Pair<String, Fractal.Type> p = adapter.elements.get(resourceCode);

		switch(p.b) {
			case Scale: {
				Scale sc = (Scale) o;
				fractal.setScale(sc);
			} break;
			case Int: {
				fractal.setInt(p.a, (Integer) o);
			} break;
			case Real: {
				fractal.setReal(p.a, (Double) o);
			} break;
			case Cplx: {
				fractal.setCplx(p.a, (Cplx) o);
			} break;
			case Expr: {
				// This one is more complicated.
				// Compiling is one here and not in the dialog because I cannot simply
				// pass a Tree as a parcel in case I modify it accordinly.

				// store id in case of error.
				// If backup is null, then the original was used.
				String backup = fractal.isDefault(p.a) ? null : (String) fractal.get(p.a).value();

				try {
					fractal.setExpr(p.a, (String) o);
					fractal.compile();

					// compiling was fine...
					adapter.notifyDataSetChanged();
				} catch(CompileException e) { // this includes parsing exceptions now
					Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

					// there was an error. Restore expr for id to original state
					if (backup == null) {
						// back to original
						fractal.setToDefault(p.a);
					} else {
						// back to old value
						fractal.setExpr(p.a, backup);
					}

					// TODO Collect these. This is code duplication

					// and reopen dialog.
					EditableDialogFragment ft = EditableDialogFragment.newInstance(
							resourceCode, "Error in Expression!", false,
							EditableDialogFragment.Type.Name).setInitVal(o);

					ft.show(getFragmentManager(), "dialog");
				}
			} break;
			case Color: {
				fractal.setColor(p.a, ((Integer) o) | 0xff000000);
			} break;
			default:
				// bool and palette is not her
				throw new IllegalArgumentException("No such type");
		}
	}

	private void showOptionsDialog(CharSequence[] options, DialogInterface.OnClickListener listener) {
		// show these simple dialogs to reset or center values.
		AlertDialog.Builder builder = new AlertDialog.Builder(ParameterEditorActivity.this);

		builder.setTitle("Select an Option");
		builder.setItems(options, listener);

		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				dialogInterface.dismiss();
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
			this.fractal = BundleAdapter.bundleToFractal(intent.getBundleExtra(SourcesListActivity.FRACTAL_INDENT_LABEL));
		} else {
			this.fractal = BundleAdapter.bundleToFractal(savedInstanceState.getBundle(SourcesListActivity.FRACTAL_INDENT_LABEL));
		}

		if(this.fractal == null) {
			throw new IllegalArgumentException("fractal is null!");
		}

		// need to extract all external values from FB. Hence parse it [compiling not necessary]

		try {
			this.fractal.parse();
		} catch (CompileException e) {
			throw new IllegalArgumentException("could not compile fractal: " + e.getMessage() + ". this is a bug.");
		}

		adapter = new ParameterAdapter(this);

		initListView();

		initButtons();
	}

	private void initButtons() {
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
				data.putExtra(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(fractal));
				setResult(1, data);
				finish();
			}
		});
	}

	private void initListView() {
		listView = (ListView) findViewById(R.id.parameterListView);

		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// Element 'position' was selected
				Pair<String, Fractal.Type> p = adapter.elements.get(position);

				switch(p.b) {
                /*case Label: {
                    // this should not happen...
                } return;*/
					case Scale: {
						EditableDialogFragment ft = EditableDialogFragment.newInstance(
								position, "Edit Scale", false, EditableDialogFragment.Type.Scale)
								.setInitVal(fractal.scale());

						ft.show(getFragmentManager(), "dialog");
					} return;
					case Expr: {
						EditableDialogFragment ft = EditableDialogFragment.newInstance(
								position, "Edit Expression", false, EditableDialogFragment.Type.Name)
								.setInitVal(fractal.get(p.a).value());

						ft.show(getFragmentManager(), "dialog");
					} return;
					case Bool: {
						boolean newValue = !(Boolean) fractal.get(p.a).value();

						fractal.setBool(p.a, newValue);
						((CheckedTextView) view).setChecked(newValue);

						adapter.notifyDataSetChanged();

						return;
					}
					case Int: {
						EditableDialogFragment ft = EditableDialogFragment.newInstance(
								position, "Edit Integer Value", false, EditableDialogFragment.Type.Int)
								.setInitVal(fractal.get(p.a).value());

						ft.show(getFragmentManager(), "dialog");
					} return;
					case Real: {
						EditableDialogFragment ft = EditableDialogFragment.newInstance(
								position, "Edit Real Value", false, EditableDialogFragment.Type.Real)
								.setInitVal(fractal.get(p.a).value());

						ft.show(getFragmentManager(), "dialog");
					}
					return;
					case Cplx: {
						EditableDialogFragment ft = EditableDialogFragment.newInstance(
								position, "Edit Complex Value", false, EditableDialogFragment.Type.Cplx)
								.setInitVal(fractal.get(p.a).value());

						ft.show(getFragmentManager(), "dialog");
					}
					return;
					case Color: {
						EditableDialogFragment ft = EditableDialogFragment.newInstance(
								position, "Edit Color", false, EditableDialogFragment.Type.Color)
								.setInitVal(fractal.get(p.a).value());

						ft.show(getFragmentManager(), "dialog");
					}
					return;
					case Palette: {
						// start new activity
						Palette value = (Palette) fractal.get(p.a).value();

						Intent i = new Intent(ParameterEditorActivity.this, PaletteActivity.class);

						i.putExtra(PaletteActivity.PALETTE_LABEL, BundleAdapter.paletteToBundle(value));
						i.putExtra(PaletteActivity.ID_LABEL, p.a); // label should also be in here.

						startActivityForResult(i, PaletteActivity.PALETTE_ACTIVITY_RETURN);
					}
					return;
					default:
						throw new IllegalArgumentException();
				}
			}
		});

		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Pair<String, Fractal.Type> p = adapter.elements.get(position);

				switch (p.b) {
					case Scale: {
						showOptionsDialog(scaleOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								Scale original = fractal.scale();
								switch (which) {
									// FIXME Put the rotations into Scale itself!
									case 0: // Reset
										fractal.setToDefault(Fractal.SCALE_KEY);
										break;
									case 1: // Origin
										fractal.setScale(new Scale(original.xx(), original.xy(), original.yx(), original.yy(), 0, 0));
										break;
									case 2: // Ratio
										// TODO: Orientation: Use diagonals.

										// FIXME Check these
										// Step 1: make x/y-vectors same length
										double xx = original.xx();
										double xy = original.xy();

										double yx = original.yx();
										double yy = original.yy();

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
										fractal.setScale(new Scale(ax, ay, -ay, ax, original.cx(), original.cy()));

										break;
									case 3: // Straighten
										double xlen = Math.hypot(original.xx(), original.xy());
										double ylen = Math.hypot(original.yx(), original.yy());
										fractal.setScale(new Scale(xlen, 0, 0, ylen, original.cx(), original.cy()));
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
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fractal.setToDefault(p.a);
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
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fractal.setToDefault(p.a);
										// must update it in the interface
										((CheckedTextView) view).setChecked((Boolean) fractal.get(p.a).value());
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
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fractal.setToDefault(p.a);
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
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fractal.setToDefault(p.a);
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
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fractal.setToDefault(p.a);
										break;
									case 1: // Center
										fractal.setCplx(p.a, new Cplx(fractal.scale().cx(), fractal.scale().cy()));
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
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fractal.setToDefault(p.a);
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
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fractal.setToDefault(p.a);
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
				}

				return false;
			}
		});
	}

	@Override
	public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
		// Save the user's current game state
		savedInstanceState.putBundle(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(fractal));

		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == PaletteActivity.PALETTE_ACTIVITY_RETURN) {
			if (resultCode == 1) { // = "Ok"
				Bundle bundle = data.getBundleExtra(PaletteActivity.PALETTE_LABEL);
				String id = data.getStringExtra(PaletteActivity.ID_LABEL);

				fractal.setPalette(id, BundleAdapter.bundleToPalette(bundle));
				adapter.notifyDataSetChanged();
			}
		} else if(requestCode == SourceEditorActivity.SOURCE_EDITOR_ACTIVITY_RETURN) {
			if (resultCode == 1) { // = "Ok"
				String source = data.getStringExtra(SourceEditorActivity.SOURCE_LABEL);

				Fractal newFractal = fractal.copyNewSource(source, true);

				try {
					newFractal.parse();

					try {
						newFractal.compile();
						this.fractal = newFractal;
						adapter.init(); // resets content of adapter.
						adapter.notifyDataSetChanged();
					} catch(CompileException e) {
						DialogHelper.confirm(
								ParameterEditorActivity.this,
								"Cannot compile parameters",
								"Reset parameters?",
								new Runnable() {
									@Override
									public void run() {
										Fractal newResetFractal = new Fractal(source, new HashMap<>());

										try {
											newResetFractal.parse();
											newResetFractal.compile();
											fractal = newResetFractal;
										} catch(ParsingError | CompileException e) {
											DialogHelper.error(ParameterEditorActivity.this, e.getMessage());
										}
									}
								},
								new Runnable() {
									@Override
									public void run() {
										startSourceEditActivity();
									}
								});
					}
				} catch(ParsingError | CompileException error) {
					// This one should not happen
					DialogHelper.error(this, "Invalid source code.");
					error.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_parameter_editor, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_reset: {
				// confirm that reset is what you want.
				AlertDialog.Builder builder = new AlertDialog.Builder(this);

				builder.setMessage("Reset Parameters to Defaults?");
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						fractal.setAllToDefault();
						adapter.notifyDataSetChanged(); // something changed...
					}
				});

				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						// do nothing
					}
				});

				builder.show();

				return true;
			}
			case R.id.action_edit_source: {
				startSourceEditActivity();
			} return true;

			default:
				throw new IllegalArgumentException("not implemented");
		}
	}

	private void startSourceEditActivity() {
		Intent i = new Intent(ParameterEditorActivity.this, SourceEditorActivity.class);

		i.putExtra(SourceEditorActivity.SOURCE_LABEL, fractal.sourceCode());
		startActivityForResult(i, SourceEditorActivity.SOURCE_EDITOR_ACTIVITY_RETURN);
	}

	private class ParameterAdapter extends BaseAdapter implements ListAdapter {

		final LayoutInflater inflater;
		final ArrayList<Pair<String, Fractal.Type>> elements;

		ParameterAdapter(Context context) {
			inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
			// fill elements with elements :)
			elements = new ArrayList<>();
			init();
		}

		/**
		 * Fill elements-list with content.
		 */
		void init() {
			elements.clear();

			// First add scale.
			// elements.add(new Pair<>("Scale", Fractal.Type.Scale));

			for(String id : fractal.parameters()) {
				elements.add(new Pair<>(id, fractal.get(id).type()));
			}
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public int getCount() {
			return elements.size();
		}

		@Override
		public String getItem(int position) {
			return elements.get(position).a;
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
			Fractal.Type t = elements.get(position).b;
			return t == Fractal.Type.Bool ? BOOL : ELEMENT;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			int viewType = getItemViewType(position);

			Pair<String, Fractal.Type> e = elements.get(position);

			switch(viewType) {
				case BOOL: {
					if (view == null)
						view = inflater.inflate(android.R.layout.simple_list_item_checked, parent, false);

					CheckedTextView text1 = (CheckedTextView) view.findViewById(android.R.id.text1);

					if(!fractal.isDefault(e.a)) {
						//Log.d("PA", e.label + " is not default");
						text1.setTypeface(Typeface.DEFAULT_BOLD);
					} else {
						//Log.d("PA", e.label + " is default");
						text1.setTypeface(Typeface.DEFAULT);
					}

					text1.setText(e.a);
					text1.setChecked((Boolean) fractal.get(e.a).value());
				} break;
				case ELEMENT: {
					if (view == null)
						view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
					TextView text1 = (TextView) view.findViewById(android.R.id.text1);
					text1.setText(e.a);

					// if not isDefaultValue set bold.
					if(!fractal.isDefault(e.a)) {
						//Log.d("PA", e.label + " is not default");
						text1.setTypeface(Typeface.DEFAULT_BOLD);
					} else {
						//Log.d("PA", e.label + " is default");
						text1.setTypeface(Typeface.DEFAULT);
					}

					TextView text2 = (TextView) view.findViewById(android.R.id.text2);
					switch(e.b) {
						case Expr: text2.setText("Expression"); break;
						case Int: text2.setText("Integer Number"); break;
						case Real: text2.setText("Real Number"); break;
						case Cplx: text2.setText("Complex Number"); break;
						case Color: text2.setText("Color"); break;
                        case Palette: text2.setText("Palette"); break;
                        case Scale: text2.setText("Scale"); break;
                        default: Log.e(getClass().getName(), "missing label for " + e.b);
					}
				} break;
			}

			return view;
		}
	}
}
