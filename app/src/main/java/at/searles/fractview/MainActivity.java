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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.File;

import at.searles.fractview.editors.EditableDialogFragment;
import at.searles.fractview.fractal.FavoriteEntry;
import at.searles.fractview.fractal.Fractal;
import at.searles.fractview.fractal.PresetFractals;
import at.searles.fractview.ui.ScaleableImageView;
import at.searles.meelan.CompileException;


// Activity is the glue between BitmapFragment and Views.
public class MainActivity extends Activity
		implements ActivityCompat.OnRequestPermissionsResultCallback,
		BitmapFragment.UpdateListener,
		EditableDialogFragment.Callback {

    //public static final int ALPHA_PREFERENCES = 0xaa000000;

	public static final int MAX_INIT_SIZE = 2048 * 1536;

	public static final int PARAMETER_ACTIVITY_RETURN = 101;
	public static final int PRESETS_ACTIVITY_RETURN = 102;
	public static final int BOOKMARK_ACTIVITY_RETURN = 103;

	public static final long PROGRESS_UPDATE_MILLIS = 500; // update the progress bar every ... ms.

	ScaleableImageView imageView; // fixme don't forget to change size of this one.
	ProgressBar progressBar;

    BitmapFragment bitmapFragment;

	SharedPreferences prefs;

	FragmentManager fm;

	//int backgroundColor;
    //int textColor;

	/**
	 * Class used for updating the view on a regular basis
	 */
	class UpdateAction implements Runnable {
		Handler updateHandler = new Handler();
		boolean updateActionRunning = false;

		void updateProgress() {
			progressBar.setProgress((int) (bitmapFragment.progress() * progressBar.getMax() + 0.5));
		}

		// start update action
		void schedule() {
			// always in UI-thread.
			if(bitmapFragment == null) return;

			if(!updateActionRunning && bitmapFragment.isRunning()) {
				updateProgress();

				// only start if it is not running yet.
				progressBar.setProgress(0);
				progressBar.setVisibility(View.VISIBLE);

				updateActionRunning = true;
				updateHandler.postDelayed(this, PROGRESS_UPDATE_MILLIS);
			}
		}

		@Override
		public void run() {
			if(bitmapFragment != null) {
				if (bitmapFragment.isRunning()) {
					updateProgress();
					updateHandler.postDelayed(this, PROGRESS_UPDATE_MILLIS);
				} else {
					progressBar.setVisibility(View.INVISIBLE);
					updateActionRunning = false;
				}
			}
		}
	}

	UpdateAction updateAction = new UpdateAction();

	/**
	 * Must be called from the UI-thread
	 */
	public void startProgressUpdates() {
		// start if not running.
		// fixme check thread
		updateAction.schedule();
	}



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

		imageView = (ScaleableImageView) findViewById(R.id.imageView);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		progressBar.setVisibility(View.INVISIBLE); // will be shown maybe later

		super.onCreate(savedInstanceState); // this one (re-)creates the bitmap fragment on rotation.

		// Get settings from shared preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		fm = getFragmentManager();

		bitmapFragment = (BitmapFragment) fm.findFragmentByTag("bitmap_fragment");

		// FIXME Order is stupid.
		// How to fix it:
		// First, create bitmap fragment with dimensions.
		//     This also creates the bitmap.
		//     in BMF, drawer is null at this point, but still the bitmap can be set for the viewer.
		//     Then, call initDrawer-method in background with dialog. This initializes the drawer.

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

			bitmapFragment = new BitmapFragment();

			// set initial size
			Bundle bundle = new Bundle();

			bundle.putInt("width", w); // Bundle because that is what I get in onCreate.
			bundle.putInt("height", h);

			String sourceCode;

			sourceCode = AssetsHelper.readSourcecode(getAssets(), "Default.fv");

			if(sourceCode == null)
				throw new IllegalArgumentException("init fractal is null??");

			Fractal initFractal = new Fractal(
					PresetFractals.INIT_SCALE,
					sourceCode,
					new Fractal.Parameters()
			);

			bundle.putParcelable("fractal", initFractal);

			bitmapFragment.setArguments(bundle);

			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.add(bitmapFragment, "bitmap_fragment");
			transaction.commitAllowingStateLoss(); // fixme why would there be a stateloss?
		}

		// now we have a valid bitmap fragment, but careful! it is not yet initialized.
		imageView.setBitmapFragment(bitmapFragment);

		updateAction.schedule(); // start progress if necessary
	}

	@Override
	public void onDestroy() {
		// if an update thread is running, stop it now.
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

		Toast.makeText(this,
				this.getString(R.string.label_new_default_size, width, height),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);

		// set the grid flag.
		MenuItem gridMenu = menu.findItem(R.id.action_show_grid);
		gridMenu.setChecked(imageView.getShowGrid());

		MenuItem rotationLockMenu =menu.findItem(R.id.action_rotation_lock);
		rotationLockMenu.setChecked(imageView.getRotationLock());

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
						bitmapFragment.shareImage();
					}
				} else {
					Toast.makeText(this, "ERROR: Cannot share/save images without " +
							"read or write permissions.", Toast.LENGTH_LONG).show();
				}
			} break;

			case WALLPAPER_PERMISSIONS: {
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					bitmapFragment.setAsWallpaper();
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
				EditableDialogFragment ft = EditableDialogFragment.newInstance(IMAGE_SIZE,
						"Change Image Size", false, EditableDialogFragment.Type.ImageSize)
						.setInitVal(new int[]{ bitmapFragment.width(), bitmapFragment.height() });

				ft.show(getFragmentManager(), "dialog");
			} return true;

			case R.id.action_add_favorite: {
				EditableDialogFragment ft = EditableDialogFragment.newInstance(ADD_FAVORITE,
						"Add Favorite", false, EditableDialogFragment.Type.Name);

				ft.show(getFragmentManager(), "dialog");
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

			case R.id.action_import: {
				// paste from clipboard
				Fractal newFractal = ClipboardHelper.pasteFractal(this);

				if(newFractal != null) {
					setNewFractal(newFractal);
				}
			} return true;

			case R.id.action_export: {
				// copy to clipboard
				ClipboardHelper.copyFractal(this, bitmapFragment.fractal());
			} return true;

			case R.id.action_show_grid: {
				boolean checked = !item.isChecked();
				item.setChecked(checked);
				imageView.setShowGrid(checked);
			} return true;

			case R.id.action_rotation_lock: {
				boolean checked = !item.isChecked();
				item.setChecked(checked);
				imageView.setRotationLock(checked);
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
											bitmapFragment.shareImage();
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
											bitmapFragment.setAsWallpaper();
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
		EditableDialogFragment.newInstance(SAVE_IMAGE,
				"Enter filename", false, EditableDialogFragment.Type.Name)
				.show(getFragmentManager(), "dialog");
	}

	// Labels for EditableDialogFragment
	private static final int ADD_FAVORITE = 1; // dialog to enter a name for a favorite
	private static final int IMAGE_SIZE = 2; // dialog to change image resolution
	private static final int SAVE_IMAGE = 3; // dialog to save the image

	@Override
	public void apply(int resourceCode, Object o) {
		switch (resourceCode) {
			case ADD_FAVORITE: {
				saveFavorite((String) o);
			} break;
			case IMAGE_SIZE: {
				// it requires amazingly much code to simply
				// check whether a string contains a parseable
				// positive integer...
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
			case SAVE_IMAGE: {
				String filename = (String) o;

				if(filename.isEmpty()) {
					Toast.makeText(this, "ERROR: Filename must not be empty.", Toast.LENGTH_LONG).show();
				}

				File directory = new File(
						Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
						"Fractview");

				Log.d("MA", "Saving file: Path is " + directory);

				if(!directory.exists()) {
					Log.d("MA", "Creating directory");
					if(!directory.mkdir()) {
						Toast.makeText(this, "ERROR: Could not create directory.",
								Toast.LENGTH_LONG).show();
					}
				}

				// loop up index
				for(int i = 0;; ++i) {
					final File imageFile = new File(directory, filename
							+ (i == 0 ? "" : ("(" + i + ")"))
							+ (filename.endsWith(".png") ? "" : ".png"));

					if(!imageFile.exists()) {
						// Saving is done in the following background thread
						bitmapFragment.saveImage(imageFile);
						return;
					}
				}

				// unreachable code.
			}
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

		SharedPreferences favoritesPrefs = getSharedPreferences("favorites", Context.MODE_PRIVATE);

		// if title exists, append an index
		if(favoritesPrefs.contains(name)) {
			int index = 1; // start from 1
			while(favoritesPrefs.contains(name + " (" + index + ")")) {
				index ++;
			}

			name = name + " (" + index + ")";
		}

		SharedPreferences.Editor editor = favoritesPrefs.edit();

		try {
			// and put it into shared preferences and store it thus.
			editor.putString(name, fav.toJSON().toString());
			editor.apply();
		} catch (JSONException e) {
			e.printStackTrace();
			Toast.makeText(MainActivity.this, "ERROR: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
	}

	void setNewFractal(final Fractal newFractal) {
		// set new but not yet compiled fractal
		try {
			newFractal.parse();
			newFractal.compile();

			// yay, success

			bitmapFragment.edit(new Runnable() {
				@Override
				public void run() {
					bitmapFragment.setFractal(newFractal);
				}
			});
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

			// fixme shouldn't history be in here? Problem is that I would have to preserve it...
			bitmapFragment.edit(new Runnable() {
				@Override
				public void run() {
					if(!bitmapFragment.historyIsEmpty()) {
						// to avoid race condition
						bitmapFragment.historyBack();
					}
				}
			});

			return true;
		}
	}

	@Override
	public void onBackPressed() {
		if(historyBack()) return;
		super.onBackPressed();
	}


	/*@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			if(historyBack()) return true;
		}
		return super.onKeyDown(keyCode, event);
	}*/

	// ================================================================
	// ========= Interactive View =====================================
	// ================================================================

    /*@Override
    public void pointMoved(final String label, PointF pos) {
        // FIXME: Change this into an "argument updated-method"
        pos = vToB(pos.x, pos.y);
        pos = bitmapFragment.fromBitmapToNormalized(pos.x, pos.y);
        final Double2 p = bitmapFragment.scale.scale(pos.x, pos.y);

        // FIXME: Only if is julia
		if(label.equals("Julia")) {
			if (bitmapFragment.fractal.isJuliaSet()) {
				bitmapFragment.edit(new Runnable() {
					@Override
					public void run() {
						bitmapFragment.fractal.setJulia(p);
					}
				}, getString(R.string.point_moved));
			} else {
				// it is a mandelbrot set. The julia-parameter does not have any impact
				bitmapFragment.fractal.setJulia(p);
			}
		} else {
			bitmapFragment.edit(new Runnable() {
				@Override
				public void run() {
					bitmapFragment.fractal.setParameter(label, p);
				}
			}, getString(R.string.point_moved));
		}
    }

	private void showPoint(String label, Double2 p) {
		/*fixme PointF vp = bitmapFragment.scale.invScale(p.x, p.y);
		vp = bitmapFragment.fromNormalizedToBitmap(vp.x, vp.y);
		final PointF q = bToV(vp.x, vp.y);

		interactiveView.setPoint(label, q);
	}

    void updateInteractiveView() {
        // Call this if the coordinates have changed for any reason
        Log.d("MA", "Arguments updated");

		// FIXME: Clear only when type was updated
		// FIXME: Only show points that are marked as visible
		// FIXME: Label for julia-set
		//interactiveView.clear();

		// first, jula parameter

		// fixme next, parameters of fractal
		/*for(String label : bitmapFragment.fractal.getPointParameters()) {
			showPoint(label, bitmapFragment.fractal.getPoint(label));
		}*

        // if it is invisible (for whatever reason)
        if(interactiveView.getVisibility() == View.INVISIBLE) {
            // make it visible.
            runOnUiThread(new Runnable() {
                public void run() {
                    interactiveView.setVisibility(View.VISIBLE);
                }
            });
        } else {
            interactiveView.postInvalidate();
        }
    }*/




	// =============================================================================
	// =========== Callbacks from Bitmap Fragment ==================================
	// =============================================================================

	// FIXME in order to be able to handle multiple bitmap fragments, there should be
	// FIXME some argument as parameter.

	@Override
	public void previewGenerated(BitmapFragment source) {
		// can be called from outside the UI-thread!
		Log.d("MA", "preview generated");
		imageView.removeLastScale();
		imageView.invalidate();
	}

	@Override
	public void bitmapUpdated(BitmapFragment source) {
		// can be called from outside the UI-thread!
		// Log.d("MA", "bitmap updated");
		imageView.invalidate();
	}

	@Override
	public void calculationStarting(BitmapFragment source) {
		// this is already called from the ui-thread.
		// we now start a handler that will update the progress every 25 ms and show it
		// in the progress bar.
		startProgressUpdates();
	}

	@Override
	public void calculationFinished(final long ms, BitmapFragment source) {
		Toast.makeText(MainActivity.this,
				getString(R.string.label_calc_finished, ms), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void newBitmapCreated(Bitmap bitmap, BitmapFragment source) {
		Log.d("MA", "received newBitmapCreated");
		imageView.setImageBitmap(bitmap);
		imageView.requestLayout();
	}
}
