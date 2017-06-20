package at.searles.fractview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import at.searles.fractview.editors.EditableDialogFragment;
import at.searles.fractview.fractal.FavoriteEntry;
import at.searles.fractview.fractal.Fractal;
import at.searles.fractview.ui.BitmapFragmentView;
import at.searles.meelan.CompileException;


// Activity is the glue between BitmapFragment and Views.
public class MainActivity extends Activity
		implements ActivityCompat.OnRequestPermissionsResultCallback,
		EditableDialogFragment.Callback {

    //public static final int ALPHA_PREFERENCES = 0xaa000000;

	public static final int MAX_INIT_SIZE = 2048 * 1536;

	public static final int PARAMETER_ACTIVITY_RETURN = 101;
	public static final int PRESETS_ACTIVITY_RETURN = 102;
	public static final int BOOKMARK_ACTIVITY_RETURN = 103;

	BitmapFragmentView imageView; // fixme don't forget to change size of this one.
	ProgressBar progressBar;

	/**
	 * Bitmap fragment contains the only image
	 */
    private BitmapFragment bitmapFragment;

	/**
	 * Listener for bitmap fragment
	 */
	private BitmapFragment.BitmapFragmentListener bitmapFragmentListener;

	SharedPreferences prefs;

	FragmentManager fm;

	public static Point screenDimensions(Context context) {
		Point dim = new Point();
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getSize(dim);

		// if width < height swap.
		if(dim.x < dim.y) {
			//noinspection SuspiciousNameCombination
			dim.set(dim.y, dim.x);
		}

		while(dim.x * dim.y > MAX_INIT_SIZE) {
			// I use a maximum size because maybe there are sometimes 10000x8000pix-screens...
			dim.x /= 2;
			dim.y /= 2;
		}

		return dim;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
		Log.d("MA", "onCreate");

		// First, take care of the view.
		setContentView(R.layout.main);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // make sure that screen stays awake

		imageView = (BitmapFragmentView) findViewById(R.id.mainBitmapFragmentView);

		super.onCreate(savedInstanceState); // this one (re-)creates the bitmap fragment on rotation.

		// Get settings from shared preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		fm = getFragmentManager();

		bitmapFragment = (BitmapFragment) fm.findFragmentByTag("bitmap_fragment");

		if(bitmapFragment == null) {
			Log.d("MA", "bitmap fragment is null");

			// fetch dimensions from preferences or display size.

			int w = prefs.getInt("width", -1);
			int h = prefs.getInt("height", -1);

			if(w == -1 || h == -1) {
				Log.i("BMF", "No dimensions in shared preferences, using display size");

				Point dim = screenDimensions(this);

				w = dim.x;
				h = dim.y;
			}

			// create bitmap fragment
			Log.d("MA", "Creating new BitmapFragment");

			String sourceCode = AssetsHelper.readSourcecode(getAssets(), "Default.fv");

			Fractal initFractal = new Fractal(
					AssetsHelper.DEFAULT_SCALE,
					sourceCode,
					new Fractal.Parameters()
			);

			bitmapFragment = BitmapFragment.newInstance(w, h, initFractal);


			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.add(bitmapFragment, "bitmap_fragment");
			transaction.commitAllowingStateLoss(); // fixme why would there be a stateloss?
		}

		// set up listeners for bitmap fragment
		bitmapFragmentListener = new BitmapFragment.BitmapFragmentListener() {

			@Override
			public void drawerStarted(BitmapFragment source) {
				// this is already called from the ui-thread.
				// we now start a handler that will update the progress every 25 ms and show it
				// in the progress bar.
			}

			@Override
			public void drawerFinished(long ms, BitmapFragment source) {
				DialogHelper.info(MainActivity.this, "Finished after " + Commons.duration(ms));
			}

			@Override
			public void initializationFinished() {
				// not needed here.
			}

			@Override
			public void bitmapUpdated(BitmapFragment src) {
				// not needed here
			}

			@Override
			public void previewGenerated(BitmapFragment src) {
				// not needed here
			}

			@Override
			public void newBitmapCreated(Bitmap bitmap, BitmapFragment src) {
				// ignore
			}
		};

		// now we have a valid bitmap fragment, but careful! it is not yet initialized.
		imageView.setBitmapFragment(bitmapFragment);
	}

	@Override
	public void onDestroy() {
		imageView.dispose();
		bitmapFragment.removeBitmapFragmentListener(bitmapFragmentListener);
		bitmapFragment = null; // all action that was not done till now is gone.
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
		Log.d("MA", "on save instance called in MA");

		// FIXME make sure that fractal is compilable!
		bitmapFragment.getArguments().putParcelable("fractal", bitmapFragment.fractal());

		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	void storeDefaultSize(int width, int height) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putInt("width", width);
		editor.putInt("height", height);
		editor.apply();

		DialogHelper.info(this, "New Default Size: " + width + " x " + height);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);

		return super.onCreateOptionsMenu(menu);
	}

	public static final int IMAGE_PERMISSIONS_SHARE = 104;
	public static final int IMAGE_PERMISSIONS_SAVE = 105;
	public static final int WALLPAPER_PERMISSIONS = 106;


	//FIXME Override in API 23
	@SuppressLint("Override")
	public void onRequestPermissionsResult(int requestCode,
										   @NotNull String permissions[], @NotNull int[] grantResults) {
		switch (requestCode) {
			case IMAGE_PERMISSIONS_SAVE:
			case IMAGE_PERMISSIONS_SHARE: {
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
					if(requestCode == IMAGE_PERMISSIONS_SAVE) {
						saveImage();
					} else {
						SavePlugin.createShare().init(bitmapFragment);
					}
				} else {
					Toast.makeText(this, "ERROR: Cannot share/save images without " +
							"read or write permissions.", Toast.LENGTH_LONG).show();
				}
			} break;

			case WALLPAPER_PERMISSIONS: {
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					SavePlugin.createSetWallpaper().init(bitmapFragment);
				} else {
					Toast.makeText(this, "Cannot set image as wallpaper without " +
							"permissions.", Toast.LENGTH_LONG).show();
				}
			} break;
		}
	}


	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
			case R.id.action_size: {
				// change size of the image
                DialogHelper.inputCustom(this, "Resize Image", R.layout.image_size_editor,
                        new DialogHelper.DialogFunction() {
                            @Override
                            public void apply(DialogInterface d) {
                                // insert current size
                                EditText widthView = (EditText) ((AlertDialog) d).findViewById(R.id.widthEditText);
                                EditText heightView = (EditText) ((AlertDialog) d).findViewById(R.id.heightEditText);

                                widthView.setText(Integer.toString(bitmapFragment.width()));
                                heightView.setText(Integer.toString(bitmapFragment.height()));

                                // listener to button
                                Button resetButton = (Button) ((AlertDialog) d).findViewById(R.id.resetSizeButton);

                                resetButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        SharedPreferences prefs =
                                                PreferenceManager.getDefaultSharedPreferences(view.getContext());

                                        Point dim = new Point();

                                        dim.set(prefs.getInt("width", -1), prefs.getInt("height", -1));

                                        if (dim.x <= 0 || dim.y <= 0) {
                                            dim = MainActivity.screenDimensions(view.getContext());
                                        }

                                        widthView.setText(Integer.toString(dim.x));
                                        heightView.setText(Integer.toString(dim.y));
                                    }
                                });

                            }
                        },
                        new DialogHelper.DialogFunction() {
                            @Override
                            public void apply(DialogInterface d) {
                                // insert current size
                                EditText widthView = (EditText) ((AlertDialog) d).findViewById(R.id.widthEditText);
                                EditText heightView = (EditText) ((AlertDialog) d).findViewById(R.id.heightEditText);

                                boolean setAsDefault = ((CheckBox) ((AlertDialog) d).findViewById(R.id.defaultCheckBox)).isChecked();

                                int w, h;

                                try {
                                    w = Integer.parseInt(widthView.getText().toString());
                                } catch(NumberFormatException e) {
                                    DialogHelper.error(((AlertDialog) d).getContext(), "invalid width");
                                    return;
                                }

                                try {
                                    h = Integer.parseInt(heightView.getText().toString());
                                } catch(NumberFormatException e) {
                                    DialogHelper.error(((AlertDialog) d).getContext(), "invalid height");
                                    return;
                                }

                                if(w < 1) {
                                    DialogHelper.error(((AlertDialog) d).getContext(), "width must be >= 1");
                                    return;
                                }

                                if(h < 1) {
                                    DialogHelper.error(((AlertDialog) d).getContext(), "height must be >= 1");
                                    return;
                                }

                                if(w == bitmapFragment.width() && h == bitmapFragment.height()) {
                                    DialogHelper.info(((AlertDialog) d).getContext(), "size not changed");

                                    if(setAsDefault) storeDefaultSize(w, h);
                                } else {
                                    // call editor
                                    bitmapFragment.setSize(w, h, setAsDefault);
                                }
                            }
                        });
			} return true;

			case R.id.action_add_favorite: {
				DialogHelper.inputText(this, "Add Favorite", "", new Commons.KeyAction() {
					@Override
					public void apply(String key) {
						saveFavorite(key);
					}
				});
			} return true;

			case R.id.action_parameters: {
				Intent i = new Intent(MainActivity.this, ParameterActivity.class);
				i.putExtra("fractal", bitmapFragment.fractal());
				startActivityForResult(i, PARAMETER_ACTIVITY_RETURN);
			} return true;

			case R.id.action_favorites: {
				// show new activity
				Intent i = new Intent(MainActivity.this, FavoritesActivity.class);
				startActivityForResult(i, BOOKMARK_ACTIVITY_RETURN);
			} return true;

			case R.id.action_presets: {
				// show new activity
				Intent i = new Intent(MainActivity.this, PresetProgramsActivity.class);
				i.putExtra("fractal", bitmapFragment.fractal());
				startActivityForResult(i, PRESETS_ACTIVITY_RETURN);
			} return true;

			case R.id.action_paste_from_clipboard: {
				// paste from clipboard
				Fractal newFractal = ClipboardHelper.pasteFractal(this);

				if(newFractal != null) {
					setNewFractal(newFractal);
				}
			} return true;

			case R.id.action_copy_to_clipboard: {
				// copy to clipboard
				ClipboardHelper.copyFractal(this, bitmapFragment.fractal());
			} return true;

			case R.id.action_gui_settings: {
				// show alert dialog with two checkboxes
				final CharSequence[] items = {"Show Grid","Rotation Lock", "Confirm Zoom with Tab", "Deactivate Zoom"};

				new AlertDialog.Builder(this)
						.setCancelable(true)
						.setMultiChoiceItems(items,
								new boolean[]{
										imageView.scaleableImageView().getShowGrid(),
										imageView.scaleableImageView().getRotationLock(),
										imageView.scaleableImageView().getConfirmZoom(),
										imageView.scaleableImageView().getDeactivateZoom()
								},
								new DialogInterface.OnMultiChoiceClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
								// fixme can move the editor to BitmapFragmentView?
								switch(indexSelected) {
									case 0: {
										// show/hide grid
										imageView.scaleableImageView().setShowGrid(isChecked);
									} break;
									case 1: {
										// rotation lock
										imageView.scaleableImageView().setRotationLock(isChecked);
									} break;
									case 2: {
										// confirm edit with a tab
										imageView.scaleableImageView().setConfirmZoom(isChecked);
									} break;
									case 3: {
										// deactivate zoom
										imageView.scaleableImageView().setDeactivateZoom(isChecked);
									} break;
								}
							}
						}).setPositiveButton("Close", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {}
						}).create().show();
			} return true;

			case R.id.action_share: {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);

				String[] items = {"Share Image", "Save Image", "Set Image as Wallpaper"};

				builder.setItems(items,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch (which) {
									case 0: { // Share
										// save/share image
										int readPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
										int writePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

										if(readPermission != PackageManager.PERMISSION_GRANTED || writePermission != PackageManager.PERMISSION_GRANTED) {
											// I am anyways showing a Toast that I can't write if I can't write.
											ActivityCompat.requestPermissions(MainActivity.this,
													new String[]{
															Manifest.permission.READ_EXTERNAL_STORAGE,
															Manifest.permission.WRITE_EXTERNAL_STORAGE
													}, IMAGE_PERMISSIONS_SHARE);
										} else {
											SavePlugin.createShare().init(bitmapFragment);
										}
									}
									break;
									case 1: { // save
										int readPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
										int writePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

										if(readPermission != PackageManager.PERMISSION_GRANTED || writePermission != PackageManager.PERMISSION_GRANTED) {
											// I am anyways showing a Toast that I can't write if I can't write.
											ActivityCompat.requestPermissions(MainActivity.this,
													new String[]{
															Manifest.permission.READ_EXTERNAL_STORAGE,
															Manifest.permission.WRITE_EXTERNAL_STORAGE
													}, IMAGE_PERMISSIONS_SAVE);
										} else {
											saveImage();
										}
									}
									break;
									case 2: { // set as wallpaper
										int wallpaperPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SET_WALLPAPER);

										if(wallpaperPermission != PackageManager.PERMISSION_GRANTED) {
											ActivityCompat.requestPermissions(MainActivity.this,
													new String[]{
															Manifest.permission.SET_WALLPAPER
													}, WALLPAPER_PERMISSIONS);
										} else {
											SavePlugin.createSetWallpaper().init(bitmapFragment);
										}
									}
									break;
									default:
										throw new IllegalArgumentException("no such selection: " + which);
								}
							}
						});
				builder.setCancelable(true);

				builder.show();


			} return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

	/*@Override
	public void initValueRequest(int requestCode, EditableDialogFragment fragment, View view) {
		switch(requestCode) {
			case 1: {
				fragment.setValue(12, view);
			} break;
			case 2: {
				fragment.setValue(24, view);
			} break;
			default:
				throw new IllegalArgumentException("bad request code: " + requestCode);
		}
	}*/

	private void saveImage() {
        DialogHelper.inputCustom(this, "Enter filename", R.layout.save_image_layout,
                new DialogHelper.DialogFunction() {
                    @Override
                    public void apply(DialogInterface d) {
						// Initialize editText with mask from timestamp
						EditText editText = (EditText) ((AlertDialog) d).findViewById(R.id.filenameEditText);

						// create timestamp
						editText.setText("fractview_" + Commons.timestamp());
                    }
                },
                new DialogHelper.DialogFunction() {
                    @Override
                    public void apply(DialogInterface d) {
                        // check "bookmark"-checkbox.
                        EditText editText = (EditText) ((AlertDialog) d).findViewById(R.id.filenameEditText);
                        CheckBox checkBox = (CheckBox) ((AlertDialog) d).findViewById(R.id.addToFavoritesCheckBox);

                        String filename = editText.getText().toString();
                        boolean addToFavorites = checkBox.isChecked();

                        if(filename.isEmpty()) {
                            DialogHelper.error(MainActivity.this, "Filename must not be empty");
                            return;
                        }

                        if(addToFavorites) {
                            saveFavorite(filename);
                        }

                        File directory = new File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                "Fractview");

                        Log.d("MA", "Saving file: Path is " + directory);

                        if(!directory.exists()) {
                            Log.d("MA", "Creating directory");
                            if(!directory.mkdir()) {
                                DialogHelper.error(MainActivity.this, "Could not create directory");
                            }
                        }

                        // loop up index
                        for(int i = 0;; ++i) {
                            final File imageFile = new File(directory, filename
                                    + (i == 0 ? "" : ("(" + i + ")"))
                                    + (filename.endsWith(".png") ? "" : ".png"));

                            if(!imageFile.exists()) {
                                // Saving is done in the following plugin
								SavePlugin.createSave(imageFile).init(bitmapFragment);
                                return;
                            }
                        }

                    }
                });
	}

	// Labels for EditableDialogFragment
	private static final int IMAGE_SIZE = 2; // dialog to change image resolution

	@Override
	public void apply(int resourceCode, Object o) {
		switch (resourceCode) {
			case IMAGE_SIZE: {
				int[] retVal = (int[]) o;

				int w = retVal[0], h = retVal[1];
				boolean setAsDefault = retVal[2] == 1;

				if(w == bitmapFragment.width() && h == bitmapFragment.height()) {
					Toast.makeText(this, "size not changed", Toast.LENGTH_SHORT).show();

					if(setAsDefault) storeDefaultSize(w, h);
				} else {
					// call editor
					bitmapFragment.setSize(w, h, setAsDefault);
				}
			} break;
			default:
				throw new IllegalArgumentException("Did not expect this: " + resourceCode);
		}
	}

	void saveFavorite(String name) {
		if(name.isEmpty()) {
			Toast.makeText(MainActivity.this, "ERROR: Name must not be empty", Toast.LENGTH_LONG).show();
			return;
		}

		// Fetch icon from bitmap fragment
		Fractal fractal = bitmapFragment.fractal();
		FavoriteEntry fav = FavoriteEntry.create(name, fractal, bitmapFragment.getBitmap());

		FavoritesManager.add(this, name, fav, SharedPrefsHelper.SaveMethod.FindNext);
	}

	void setNewFractal(final Fractal newFractal) {
		// set new but not yet compiled fractal
		try {
			newFractal.parse();
			newFractal.compile();

			// yay, success
			bitmapFragment.setFractal(newFractal);
		} catch(CompileException e) {
			e.printStackTrace();
			Toast.makeText(this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(data != null) {
			if (requestCode == PARAMETER_ACTIVITY_RETURN) {
				if (resultCode == 1) { // = "Ok"
					Fractal newFractal = data.getParcelableExtra("parameters");
					setNewFractal(newFractal);
				}
			} else if (requestCode == BOOKMARK_ACTIVITY_RETURN) {
				if (resultCode == 1) { // = "a fractal was selected"
					Fractal newFractal = data.getParcelableExtra("fractal");
					setNewFractal(newFractal);
				}
			} else if (requestCode == PRESETS_ACTIVITY_RETURN) {
				if (resultCode == 1) {
					Fractal newFractal = data.getParcelableExtra("fractal");
					setNewFractal(newFractal);
				}
			}
		}
	}

	// =======================================================================
	// ============= Some History ... ========================================
	// =======================================================================

	boolean warnedAboutHistoryEmpty = false;

	boolean historyBack() {
		// we give one warning if back was already hit.
		if(bitmapFragment.historyIsEmpty()) {
			if(warnedAboutHistoryEmpty) return false;
			else {
				Toast.makeText(this, "History is empty", Toast.LENGTH_SHORT).show();
				warnedAboutHistoryEmpty = true;
				return true;
			}
		} else {
			warnedAboutHistoryEmpty = false; // reset here.

			if(!bitmapFragment.historyIsEmpty()) {
				bitmapFragment.historyBack();
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public void onBackPressed() {
		// first, send it to image view
		if(imageView.backButtonAction()) return;
		if(historyBack()) return;
		super.onBackPressed();
	}
}
