package at.searles.fractview.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import at.searles.fractview.R;
import at.searles.fractview.fractal.Fractal;
import at.searles.fractview.fractal.PresetFractals;
import at.searles.fractview.ui.editors.ColorView;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Colors;
import at.searles.math.color.Palette;
import at.searles.meelan.CompileException;
import at.searles.utils.Pair;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ParameterActivity extends Activity implements MyAlertDialogFragment.MyAlertFragmentHandler {

	public static final int PROGRAM_ACTIVITY_RETURN = 100;

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

	Fractal fb;
	ParameterAdapter adapter; // List adapter for parameters
	ListView listView;

	// if something is edited, here is the information once the dialog calls this activities' confirm method
	private String currentEditId = null;

	@Override
	public void initDialogView(String id, View view) {
		Pair<Fractal.Type, Object> p = fb.get(id);

		Object value = p.b;

		switch(p.a) {
			case Int: {
				EditText editor = (EditText) view.findViewById(R.id.intEditText);
				editor.setText(Integer.toString((Integer) p.b));
			} break;
			case Real: {
				EditText editor = (EditText) view.findViewById(R.id.realEditText);
				editor.setText(Double.toString((Double) p.b));
			} break;
			case Cplx: {
				EditText editorX = (EditText) view.findViewById(R.id.xEditText);
				EditText editorY = (EditText) view.findViewById(R.id.yEditText);
				Cplx c = (Cplx) p.b;
				editorX.setText(Double.toString(c.re()));
				editorY.setText(Double.toString(c.im()));
			} break;
			case Expr: {
				EditText editor = (EditText) view.findViewById(R.id.stringEditText);
				editor.setText((String) value);
			} break;
			case Color: {
				// I initialize the view here.
				ColorView colorView = (ColorView) view.findViewById(R.id.colorView);
				EditText webcolorEditor = (EditText) view.findViewById(R.id.webcolorEditText);

				// I need listeners for both of them.
				colorView.bindToEditText(webcolorEditor);

				webcolorEditor.setText(Colors.toColorString((Integer) value));
				colorView.setColor((Integer) value);
			} break;
			case Scale: {
				EditText editorXX = (EditText) view.findViewById(R.id.xxEditText);
				EditText editorXY = (EditText) view.findViewById(R.id.xyEditText);
				EditText editorYX = (EditText) view.findViewById(R.id.yxEditText);
				EditText editorYY = (EditText) view.findViewById(R.id.yyEditText);
				EditText editorCX = (EditText) view.findViewById(R.id.cxEditText);
				EditText editorCY = (EditText) view.findViewById(R.id.cyEditText);

				Scale sc = (Scale) p.b;

				editorXX.setText(Double.toString(sc.xx));
				editorXY.setText(Double.toString(sc.xy));
				editorYX.setText(Double.toString(sc.yx));
				editorYY.setText(Double.toString(sc.yy));
				editorCX.setText(Double.toString(sc.cx));
				editorCY.setText(Double.toString(sc.cy));
			} break;
			// bool is not handled via a dialog!
			// neither is palette.
			default:
				throw new IllegalArgumentException("No such type");
		}
	}

	@Override
	public boolean applyDialogView(String id, View view) {
		Pair<Fractal.Type, Object> p = fb.get(id);

		switch(p.a) {
			case Int: {
				EditText editor = (EditText) view.findViewById(R.id.intEditText);
				String sVal = editor.getText().toString();

				try {
					int val = Integer.parseInt(sVal);
					fb.setInt(id, val);
					return true;
				} catch(NumberFormatException e) {
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
					return false;
				}
			}
			case Real: {
				EditText editor = (EditText) view.findViewById(R.id.realEditText);
				String sVal = editor.getText().toString();

				try {
					double val = Double.parseDouble(sVal);
					fb.setReal(id, val);
					return true;
				} catch(NumberFormatException e) {
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
					return false;
				}
			}
			case Cplx: {
				EditText editorX = (EditText) view.findViewById(R.id.xEditText);
				EditText editorY = (EditText) view.findViewById(R.id.yEditText);

				String sx = editorX.getText().toString();
				String sy = editorY.getText().toString();

				try {
					double x = Double.parseDouble(sx);

					try {
						double y = Double.parseDouble(sy);
						fb.setCplx(id, new Cplx(x, y));
						return true;
					} catch(NumberFormatException e) {
						// FIXME focus on editorY
						Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
						return false;
					}
				} catch(NumberFormatException e) {
					// FIXME focus on editorX
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
					return false;
				}
			}
			case Expr: {
				EditText editor = (EditText) view.findViewById(R.id.stringEditText);
				String sExpr = editor.getText().toString();
				fb.setExpr(id, sExpr);
				return true;
			}
			case Color: {
				ColorView editor = (ColorView) view.findViewById(R.id.colorView);
				int color = editor.getColor();
				fb.setColor(id, color | 0xff000000);
				return true;
			}
			case Scale: {
				EditText[] editors = new EditText[6];

				editors[0] = ((EditText) view.findViewById(R.id.xxEditText));
				editors[1] = (EditText) view.findViewById(R.id.xyEditText);
				editors[2] = (EditText) view.findViewById(R.id.yxEditText);
				editors[3] = (EditText) view.findViewById(R.id.yyEditText);
				editors[4] = (EditText) view.findViewById(R.id.cxEditText);
				editors[5] = (EditText) view.findViewById(R.id.cyEditText);

				double[] ds = new double[6];

				for(int i = 0; i < 6; ++i) {
					try {
						ds[i] = Double.parseDouble(editors[i].getText().toString());
					} catch(NumberFormatException e) {
						Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
						return false;
					}
				}

				Scale sc = new Scale(ds[0], ds[1], ds[2], ds[3], ds[4], ds[5]);

				fb.setScale(sc);
				return true;
			}
			default:
				// bool and palette is not her
				throw new IllegalArgumentException("No such type");
		}
	}

	/*@Override
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
		Log.d("cplx edit callback: " , "re = " + c.re() + ", im = " + c.im());

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
	}*/

	private void showOptionsDialog(CharSequence[] options, DialogInterface.OnClickListener listener) {
		// show these simple dialogs to reset or center values.
		AlertDialog.Builder builder = new AlertDialog.Builder(ParameterActivity.this);

		builder.setTitle("Select an Option");
		builder.setItems(options, listener);

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

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

		listView.setOnItemClickListener((parent, view, position, id) -> {
			// Element 'position' was selected
            Pair<String, Fractal.Type> p = adapter.elements.get(position);

            currentEditId = p.a;

            switch(p.b) {
                /*case Label: {
                    // this should not happen...
                } return;*/
                case Scale: {
					MyAlertDialogFragment.newInstance("Edit Scale", R.layout.scale_editor, p.a).showDialog(this);
                } return;
                case Expr: {
					MyAlertDialogFragment.newInstance("Edit Expression", R.layout.string_editor, p.a).showDialog(this);
                } return;
                case Bool: {
                    boolean newValue = !(Boolean) fb.get(p.a).b;

                    fb.setBool(p.a, newValue);
                    ((CheckedTextView) view).setChecked(newValue);

                    adapter.notifyDataSetChanged();

                    return;
                }
                case Int: {
					MyAlertDialogFragment.newInstance("Edit Integer", R.layout.int_editor, p.a).showDialog(this);
                } return;
                case Real: {
					MyAlertDialogFragment.newInstance("Edit Real Value", R.layout.real_editor, p.a).showDialog(this);
                }
                return;
                case Cplx: {
					MyAlertDialogFragment.newInstance("Edit Complex Value", R.layout.cplx_editor, p.a).showDialog(this);
                }
                return;
                case Color: {
					MyAlertDialogFragment.newInstance("Edit Color", R.layout.color_editor, p.a).showDialog(this);
                }
                return;
                case Palette: {
					// start new activity
                    Palette value = (Palette) fb.get(p.a).b;

                    Intent i = new Intent(ParameterActivity.this, PaletteActivity.class);

                    i.putExtra("palette", new PaletteActivity.PaletteWrapper(/*p.label,*/ value));
                    startActivityForResult(i, PaletteActivity.PALETTE_ACTIVITY_RETURN);
                }
                return;
				default:
					throw new IllegalArgumentException();
            }
        });

		listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Pair<String, Fractal.Type> p = adapter.elements.get(position);

            switch(p.b) {
                case Scale: {
                    showOptionsDialog(scaleOptions, (dialog, which) -> {
                        Scale original = fb.scale();
                        switch(which) {
                            case 0: // Reset
                                fb.resetScale();
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
                    });

                    return true;
                }
                case Expr: {
                    showOptionsDialog(exprOptions, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch(which) {
                                case 0: // Reset
                                    fb.reset(p.a);
                                    break;
                            }

                            adapter.notifyDataSetChanged();
                        }
                    });
                    return true;
                }
                case Bool: {
                    showOptionsDialog(boolOptions, (dialog, which) -> {
                        switch(which) {
                            case 0: // Reset
                                fb.reset(p.a);
                                // must update it in the interface
                                ((CheckedTextView) view).setChecked((Boolean) fb.get(p.a).b);
                                break;
                        }

                        adapter.notifyDataSetChanged();
                    });
                    return true;
                }
                case Int: {
                    showOptionsDialog(intOptions, (dialog, which) -> {
                        switch(which) {
                            case 0: // Reset
                                fb.reset(p.a);
                                break;
                        }

                        adapter.notifyDataSetChanged();
                    });
                    return true;
                }
                case Real: {
                    showOptionsDialog(realOptions, (dialog, which) -> {
                        switch(which) {
                            case 0: // Reset
                                fb.reset(p.a);
                                break;
                        }

                        adapter.notifyDataSetChanged();
                    });

                    return true;
                }
                case Cplx: {
                    showOptionsDialog(cplxOptions, (dialog, which) -> {
                        switch(which) {
                            case 0: // Reset
                                fb.reset(p.a);
                                break;
                            case 1: // Center
                                fb.setCplx(p.a, new Cplx(fb.scale().cx, fb.scale().cy));
                                break;
                        }

                        adapter.notifyDataSetChanged();
                    });

                    return true;
                }
                case Color: {
                    showOptionsDialog(colorOptions, (dialog, which) -> {
                        switch(which) {
                            case 0: // Reset
                                fb.reset(p.a);
                                break;
                        }

                        adapter.notifyDataSetChanged();
                    });

                    return true;
                }
                case Palette: {
                    showOptionsDialog(paletteOptions, (dialog, which) -> {
                        switch(which) {
                            case 0: // Reset
                                fb.reset(p.a);
                                break;
                        }

                        adapter.notifyDataSetChanged();
                    });

                    return true;
                }
            }
            // edit value

            return false;
        });

		Button okButton = (Button) findViewById(R.id.okButton);
		Button cancelButton = (Button) findViewById(R.id.cancelButton);

		cancelButton.setOnClickListener(view -> {
            setResult(0);
            finish();
        });

		okButton.setOnClickListener(view -> {
            Intent data = new Intent();
            data.putExtra("parameters", fb);
            setResult(1, data);
            finish();
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

				fb.setPalette(currentEditId, wrapper.p);
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

				builder.setMessage("Reset Parameters to Defaults?");
				builder.setPositiveButton("Ok", (dialogInterface, which) -> {
					fb.resetAll();
                    adapter.notifyDataSetChanged(); // something changed...
                });

				builder.setNegativeButton("Cancel", (dialogInterface, which) -> {
                    // do nothing.
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
			elements.add(new Pair<>("Scale", Fractal.Type.Scale));

			for(String id : fb.parameterIds()) {
				elements.add(new Pair<>(id, fb.get(id).a));
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
			return t == Fractal.Type.Bool ? BOOL : t == Fractal.Type.Scale ? SCALE : ELEMENT;
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

					if(!fb.isDefaultValue(e.a)) {
						//Log.d("PA", e.label + " is not default");
						text1.setTypeface(Typeface.DEFAULT_BOLD);
					} else {
						//Log.d("PA", e.label + " is default");
						text1.setTypeface(Typeface.DEFAULT);
					}

					text1.setText(e.a);
					text1.setChecked((Boolean) fb.get(e.a).b);
				} break;
				case ELEMENT: {
					if (view == null)
						view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
					TextView text1 = (TextView) view.findViewById(android.R.id.text1);
					text1.setText(e.a);

					// if not isDefaultValue set bold.
					if(!fb.isDefaultValue(e.a)) {
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
					}
				} break;
				case SCALE: {
					if (view == null)
						view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
					TextView text1 = (TextView) view.findViewById(android.R.id.text1);
					text1.setText("Scale"); // FIXME there is only one. Thus use a string.

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