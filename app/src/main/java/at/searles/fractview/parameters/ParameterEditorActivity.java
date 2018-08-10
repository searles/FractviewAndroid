package at.searles.fractview.parameters;

//import at.searles.meelan.CompileException;
//import at.searles.parsing.ParsingError;

@Deprecated
public class ParameterEditorActivity /* extends Activity implements EditableDialogFragment.Callback*/ {

//
//	// The following use different views (and listeners)
//	private static final int BOOL = 0;
//	private static final int ELEMENT = 1;
//
//	private Fractal fractal;
//	private ParameterAdapter adapter; // List adapter for parameters
//	private ListView listView;
//
//	@Override
//	public void apply(int resourceCode, Object o) {
//		// TODO Should this method move into the adapter?
//		if(o == null) {
//			// There was an error in the input
//			Toast.makeText(this, "ERROR: Bad input", Toast.LENGTH_LONG).show();
//			return;
//		}
//
//		// The resourceCode is the position in the element list
//		Pair<String, Fractal.Type> p = adapter.elements.get(resourceCode);
//
//		switch(p.b) {
//			case Scale: {
//				Scale sc = (Scale) o;
//				fractal.setScale(sc);
//			} break;
//			case Int: {
//				fractal.setInt(p.a, (Integer) o);
//			} break;
//			case Real: {
//				fractal.setReal(p.a, (Double) o);
//			} break;
//			case Cplx: {
//				fractal.setCplx(p.a, (Cplx) o);
//			} break;
//			case Expr: {
//				// This one is more complicated.
//				// Compiling is one here and not in the dialog because I cannot simply
//				// pass a Tree as a parcel in case I modify it accordinly.
//
//				// store id in case of error.
//				// If backup is null, then the original was used.
//				String backup = fractal.isDefault(p.a) ? null : (String) fractal.get(p.a).value();
//
//				try {
//					fractal.setExpr(p.a, (String) o);
//					fractal.compile();
//
//					// compiling was fine...
//					adapter.notifyDataSetChanged();
//				} catch(CompileException e) { // this includes parsing exceptions now
//					Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
//
//					// there was an error. Restore expr for id to original state
//					if (backup == null) {
//						// back to original
//						fractal.setToDefault(p.a);
//					} else {
//						// back to old value
//						fractal.setExpr(p.a, backup);
//					}
//
//					// TODO Collect these. This is code duplication
//
//					// and reopen dialog.
//					EditableDialogFragment ft = EditableDialogFragment.newInstance(
//							resourceCode, "Error in Expression!", false,
//							EditableDialogFragment.Type.Name).setInitVal(o);
//
//					ft.show(getFragmentManager(), "dialog");
//				}
//			} break;
//			case Color: {
//				fractal.setColor(p.a, ((Integer) o) | 0xff000000);
//			} break;
//			default:
//				// bool and palette is not her
//				throw new IllegalArgumentException("No such type");
//		}
//	}
//
//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.parameter_layout);
//
//		if(savedInstanceState == null) {
//			Intent intent = getIntent();
//			this.fractal = BundleAdapter.bundleToFractal(intent.getBundleExtra(SourcesListActivity.FRACTAL_INDENT_LABEL));
//		} else {
//			this.fractal = BundleAdapter.bundleToFractal(savedInstanceState.getBundle(SourcesListActivity.FRACTAL_INDENT_LABEL));
//		}
//
//		if(this.fractal == null) {
//			throw new IllegalArgumentException("fractal is null!");
//		}
//
//		// need to extract all external values from FB. Hence parse it [compiling not necessary]
//
//		try {
//			this.fractal.parse();
//		} catch (CompileException e) {
//			throw new IllegalArgumentException("could not compile fractal: " + e.getMessage() + ". this is a bug.");
//		}
//
//		adapter = new ParameterAdapter(this, this);
//
//		initListView();
//
//		initButtons();
//	}
//
//	private void initButtons() {
//		Button okButton = (Button) findViewById(R.id.okButton);
//		Button cancelButton = (Button) findViewById(R.id.cancelButton);
//
//		cancelButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View view) {
//				setResult(0);
//				finish();
//			}
//		});
//
//		okButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View view) {
//				Intent data = new Intent();
//				data.putExtra(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(fractal));
//				setResult(1, data);
//				finish();
//			}
//		});
//	}
//
//	private void initListView() {
//		listView = (ListView) findViewById(R.id.parameterListView);
//
//		listView.setAdapter(adapter);
//
//		listView.setOnItemClickListener(new ParameterSelectListener(this));
//
//		listView.setOnItemLongClickListener(new ParameterLongSelectListener(this));
//	}
//
//	@Override
//	public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
//		// Save the user's current game state
//		savedInstanceState.putBundle(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(fractal));
//
//		// Always call the superclass so it can save the view hierarchy state
//		super.onSaveInstanceState(savedInstanceState);
//	}
//
//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		super.onActivityResult(requestCode, resultCode, data);
//
//		if (requestCode == PaletteActivity.PALETTE_ACTIVITY_RETURN) {
//			if (resultCode == 1) { // = "Ok"
//				Bundle bundle = data.getBundleExtra(PaletteActivity.PALETTE_LABEL);
//				String id = data.getStringExtra(PaletteActivity.ID_LABEL);
//
//				fractal.setPalette(id, BundleAdapter.bundleToPalette(bundle));
//				adapter.notifyDataSetChanged();
//			}
//		} else if(requestCode == SourceEditorActivity.SOURCE_EDITOR_ACTIVITY_RETURN) {
//			if (resultCode == 1) { // = "Ok"
//				String source = data.getStringExtra(SourceEditorActivity.SOURCE_LABEL);
//
//				Fractal newFractal = fractal.copyNewSource(source, true);
//
//				try {
//					newFractal.parse();
//
//					try {
//						newFractal.compile();
//						this.fractal = newFractal;
//						adapter.init(); // resets content of adapter.
//						adapter.notifyDataSetChanged();
//					} catch(CompileException e) {
//						DialogHelper.confirm(
//								ParameterEditorActivity.this,
//								"Cannot compile parameters",
//								e.getMessage() + "! Reset parameters?",
//								new Runnable() {
//									@Override
//									public void run() {
//										Fractal newResetFractal = new Fractal(source, new HashMap<>());
//
//										try {
//											newResetFractal.parse();
//											newResetFractal.compile();
//											fractal = newResetFractal;
//										} catch(ParsingError | CompileException e) {
//											DialogHelper.error(ParameterEditorActivity.this, e.getMessage());
//										}
//									}
//								},
//								new Runnable() {
//									@Override
//									public void run() {
//										startSourceEditActivity(source);
//									}
//								});
//					}
//				} catch(ParsingError | CompileException error) {
//					// This one should not happen
//					DialogHelper.error(this, "Invalid source code.");
//					error.printStackTrace();
//				}
//			}
//		}
//	}
//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu items for use in the action bar
//		MenuInflater inflater = getMenuInflater();
//		inflater.inflate(R.menu.activity_parameter_editor, menu);
//		return super.onCreateOptionsMenu(menu);
//	}
//
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		// Handle presses on the action bar items
//		switch (item.getItemId()) {
//			case R.id.action_reset: {
//				// confirm that reset is what you want.
//				AlertDialog.Builder builder = new AlertDialog.Builder(this);
//
//				builder.setMessage("Reset Parameters to Defaults?");
//				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialogInterface, int i) {
//						fractal.setAllToDefault();
//						adapter.notifyDataSetChanged(); // something changed...
//					}
//				});
//
//				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialogInterface, int i) {
//						// do nothing
//					}
//				});
//
//				builder.show();
//
//				return true;
//			}
//			case R.id.action_edit_source: {
//				startSourceEditActivity(fractal.sourceCode());
//			} return true;
//
//			default:
//				throw new IllegalArgumentException("not implemented");
//		}
//	}
//
//	private void startSourceEditActivity(String source) {
//		Intent i = new Intent(ParameterEditorActivity.this, SourceEditorActivity.class);
//
//		i.putExtra(SourceEditorActivity.SOURCE_LABEL, source);
//		startActivityForResult(i, SourceEditorActivity.SOURCE_EDITOR_ACTIVITY_RETURN);
//	}

}
