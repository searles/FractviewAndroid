package at.searles.fractview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.WallpaperManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import at.searles.fractview.fractal.FavoriteEntry;
import at.searles.fractview.fractal.Fractal;
import at.searles.fractview.fractal.Parameters;
import at.searles.fractview.fractal.PresetFractals;
import at.searles.fractview.ui.FavoritesActivity;
import at.searles.fractview.ui.ParameterActivity;
import at.searles.fractview.ui.PresetsActivity;
import at.searles.fractview.ui.ScaleableImageView;
import at.searles.fractview.ui.editors.ImageSizeEditor;
import at.searles.fractview.ui.editors.ShareEditor;
import at.searles.meelan.CompileException;


// Activity is the glue between BitmapFragment and Views.
public class MainActivity extends Activity implements BitmapFragment.UpdateListener, ActivityCompat.OnRequestPermissionsResultCallback, ImageSizeEditor.Callback, ShareEditor.Callback {

    //public static final int ALPHA_PREFERENCES = 0xaa000000;

	public static final int MAX_INIT_SIZE = 2048 * 1536;

	public static final int PARAMETER_ACTIVITY_RETURN = 101;
	public static final int PRESETS_ACTIVITY_RETURN = 102;
	public static final int BOOKMARK_ACTIVITY_RETURN = 103;

	public static final int IMAGE_PERMISSIONS = 104;
	// public static final int WALLPAPER_PERMISSIONS = 104;

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

	public static void copyToClipboard(Context context, Fractal fractal) {
		try {
			String export = fractal.toJSON().toString(2);
			ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("fractview", export);
			clipboard.setPrimaryClip(clip);
			Toast.makeText(context, "Copied specification to clipboard", Toast.LENGTH_SHORT).show();
		} catch (JSONException e) {
			e.printStackTrace();
			Toast.makeText(context, "Could not create JSON", Toast.LENGTH_LONG).show();
		}
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

			String sourceCode = null;

			try {
				sourceCode = PresetFractals.readSourcecode(this, "Default.fv");
			} catch (IOException e) {
				throw new IllegalArgumentException("Default.fv is missing?!");
			}

			Fractal initFractal = new Fractal(
					PresetFractals.INIT_SCALE,
					sourceCode,
					new Parameters()
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
		// FIXME make sure that fractal is compilable!
		bitmapFragment.getArguments().putParcelable("fractal", bitmapFragment.fractal());

		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void newBitmapCreated(Bitmap bitmap) {
		Log.d("MA", "received newBitmapCreated");
		imageView.setImageBitmap(bitmap);
		imageView.requestLayout();
	}

	void storeSize(int width, int height) {
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
		return super.onCreateOptionsMenu(menu);
	}


	private void shareAction(boolean allowSettingWallpaper) {
		final ShareEditor shareEditor = new ShareEditor("Save/Share Fractal", "fractview");
		shareEditor.allowSettingWallpaper(allowSettingWallpaper);

		shareEditor.createDialog(this);
	}

	//FIXME Override in API 23
	@SuppressLint("Override")
	public void onRequestPermissionsResult(int requestCode,
										   @NotNull String permissions[], @NotNull int[] grantResults) {
		switch (requestCode) {
			case IMAGE_PERMISSIONS: {
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
					shareAction(grantResults[2] == PackageManager.PERMISSION_GRANTED);
				} else {
					Toast.makeText(this, "Cannot share/save images without " +
							"read or write permissions.", Toast.LENGTH_LONG).show();
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
				ImageSizeEditor editor = new ImageSizeEditor("Edit Image Size", bitmapFragment.width(), bitmapFragment.height());
				editor.createDialog(this);
			} return true;

			case R.id.action_share: {
				// save/share image
 				applyShare(); // FIXME Now it should be simple

				//bitmapFragment.debugBitmap();

				// First, ask for permission (Android 6)

				// <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
				// <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
				// <uses-permission android:name="android.permission.SET_WALLPAPER"/>


				// FIXME
				// FIXME
				// FIXME
				// FIXME

				int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
				int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
				int wallpaperPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SET_WALLPAPER);

				if(readPermission != PackageManager.PERMISSION_GRANTED || writePermission != PackageManager.PERMISSION_GRANTED) {
					// I am anyways showing a Toast that I can't write if I can't write.
					ActivityCompat.requestPermissions(this,
							new String[]{
									Manifest.permission.READ_EXTERNAL_STORAGE,
									Manifest.permission.WRITE_EXTERNAL_STORAGE,
									Manifest.permission.SET_WALLPAPER
							}, IMAGE_PERMISSIONS);
				} else {
					// shareAction(wallpaperPermission == PackageManager.PERMISSION_GRANTED);
				}
			} return true;

			case R.id.action_save_favorite: {
				// create alert dialog with edittext
				AlertDialog.Builder builder = new AlertDialog.Builder(this);

				builder.setTitle("Enter Favorite's Name");
				builder.setIcon(R.drawable.ic_bookmark_white_24dp);

				final EditText titleEdit = new EditText(this);
				builder.setView(titleEdit);

				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						// Fetch icon from bitmap fragment
						Fractal fractal = bitmapFragment.fractal();
						FavoriteEntry fav = FavoriteEntry.create(fractal, bitmapFragment.getBitmap());

						SharedPreferences favoritesPrefs = getSharedPreferences("favorites", Context.MODE_PRIVATE);

						String title = titleEdit.getText().toString().trim();

						if(title.isEmpty()) {
							Toast.makeText(MainActivity.this, "label cannot be empty", Toast.LENGTH_LONG).show();
						} else {
							// if title exists, append an index
							if(favoritesPrefs.contains(title)) {
								int index = 0;
								while(favoritesPrefs.contains(title + " (" + index + ")")) {
									index ++;
								}
								title = title + " (" + index + ")";
							}

							SharedPreferences.Editor editor = favoritesPrefs.edit();

							try {
								// and put it into shared preferences and store it thus.
								editor.putString(title, fav.toJSON().toString());
								editor.apply();

								Toast.makeText(MainActivity.this, "Saved as " + title, Toast.LENGTH_SHORT).show();
							} catch (JSONException e) {
								e.printStackTrace();
								Toast.makeText(MainActivity.this, "error storing favorite: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
							}
						}
					}
				});

				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						dialogInterface.dismiss(); // cancel and dismiss are equivalent
					}
				});

				builder.show();
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
				Intent i = new Intent(MainActivity.this, PresetsActivity.class);
				i.putExtra("fractal", bitmapFragment.fractal());
				startActivityForResult(i, PRESETS_ACTIVITY_RETURN);
			} return true;

			case R.id.action_import: {
				// paste from clipboard
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

				if(!clipboard.hasPrimaryClip()) {
					Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_LONG).show();
					return false;
				}

				/*if(!clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
					Toast.makeText(this, "Clipboard does not contain text", Toast.LENGTH_LONG).show();
					return false;
				}*/

				CharSequence pasteText = clipboard.getPrimaryClip().getItemAt(0).getText();

				try {
					final Fractal newFractal = Fractal.fromJSON(new JSONObject(pasteText.toString()));
					setNewFractal(newFractal);
					Toast.makeText(this, "Pasted from clipboard", Toast.LENGTH_SHORT).show();
				} catch (JSONException e) {
					e.printStackTrace();
					Toast.makeText(this, "Could not parse content of clipboard", Toast.LENGTH_LONG).show();
				}

			} return true;

			case R.id.action_export: {
				// copy to clipboard
				MainActivity.copyToClipboard(this, bitmapFragment.fractal());
			} return true;
			case R.id.action_show_grid: {
				boolean checked = !item.isChecked();
				item.setChecked(checked);
				imageView.setShowGrid(checked);
				return true;
			}

            default:
                return super.onOptionsItemSelected(item);
        }
    }

	void setNewFractal(final Fractal newFractal) {
		// set new but not yet compiled fractal
		try {
			newFractal.parse();
			newFractal.compile();

			// yay, success

			bitmapFragment.edit(() -> bitmapFragment.setFractal(newFractal));
		} catch(CompileException e) {
			e.printStackTrace();
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == PARAMETER_ACTIVITY_RETURN) {
			if(resultCode == 1) { // = "Ok"
				Fractal newFractal = data.getParcelableExtra("parameters");
				setNewFractal(newFractal);
			}
		} else if(requestCode == BOOKMARK_ACTIVITY_RETURN) {
			if(resultCode == 1) { // = "a fractal was selected"
				Fractal newFractal = data.getParcelableExtra("fractal");
				setNewFractal(newFractal);
			}
		} else if(requestCode == PRESETS_ACTIVITY_RETURN) {
			if(resultCode == 1) {
				Fractal newFractal = data.getParcelableExtra("fractal");
				setNewFractal(newFractal);
			}
		}
	}

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

	/*@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			if(historyBack()) return true;
		}
		return super.onKeyDown(keyCode, event);
	}*/

	@Override
	public void onBackPressed() {
		if(historyBack()) return;
		super.onBackPressed();
	}

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

	@Override
	public void previewGenerated() {
		// can be called from outside the UI-thread!
		Log.d("MA", "preview generated");
		imageView.removeLastScale();
		imageView.invalidate();
	}

	@Override
	public void bitmapUpdated() {
		// can be called from outside the UI-thread!
		Log.d("MA", "bitmap updated");
		imageView.invalidate();
	}

	@Override
	public void calculationStarting() {
		// this is already called from the ui-thread.
		// we now start a handler that will update the progress every 25 ms and show it
		// in the progress bar.
		startProgressUpdates();
	}

	@Override
    public void calculationFinished(final long ms) {
		Toast.makeText(MainActivity.this, getString(R.string.label_calc_finished, ms), Toast.LENGTH_SHORT).show();
    }

	@Override
	public boolean applyImageSize(final int width, final int height, final boolean setAsDefault) {
		if(width == bitmapFragment.width() &&
				height == bitmapFragment.height()) {
			// no need for a new drawing.
			if (setAsDefault) {
				storeSize(width, height);
			}
		} else {
			bitmapFragment.edit(new Runnable() {
				@Override
				public void run() {
					// Set size
					if (bitmapFragment.setSize(width, height)) {
						if (setAsDefault) {
							// commit to shared preferences
							runOnUiThread(new Runnable() {
								public void run() {
									storeSize(width, height);
								}
							});
						}

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// fixme: combine these somehow in a better way.
								// imageView.updateBitmap();
								// fixme imageView.initMatrices();
							}
						});
					} else {
						runOnUiThread(new Runnable() {
							public void run() {
								Toast.makeText(MainActivity.this, getString(R.string.label_bitmap_fail,
										bitmapFragment.width(), bitmapFragment.height()), Toast.LENGTH_LONG).show();
							}
						});
					}
				}
			});
		}
		// fixme: Where should I
		// fixme: perform the check to create the new image?
		//
		// fixme: Where should I check whether it should be used as default?

		return true;
	}


	// FIXME
	// FIXME
	// FIXME
	// FIXME
	// FIXME
	// FIXME

	@Override
	public boolean applySave(String filename, final boolean share, final boolean setAsWallpaper) {
		return false;
	}
	/*
		// Get path for picture
		File directory = new File(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"Fractview");

		Log.d("MA", "Saving file: Path is " + directory);

		if(!directory.exists()) {
			Log.d("MA", "Creating directory");
			if(!directory.mkdir()) {
				Toast.makeText(MainActivity.this, "Could not create directory (maybe permission denied?)",
						Toast.LENGTH_LONG).show();

				return false;
			}
		}

		// loop up index
		for(int i = 0;; ++i) {
			final File imageFile = new File(directory, filename
					+ (i == 0 ? "" : ("(" + i + ")"))
							+ (filename.endsWith(".png") ? "" : ".png"));

			if(!imageFile.exists()) {
				// Saving is done in the following background thread
				bitmapFragment.saveImageInBackground(imageFile, share, setAsWallpaper);
				return true;
			}
		}
	}*/



	public void applyShare() {
		// share picture
		try {
			File imageFile = File.createTempFile("fractview", "png", getExternalCacheDir());
			Uri contentUri = Uri.fromFile(imageFile);

			bitmapFragment.saveImageInBackground(imageFile, () -> {
				Intent share = new Intent(Intent.ACTION_SEND);
				share.setType("image/png");
				share.putExtra(Intent.EXTRA_STREAM, contentUri);
				startActivity(Intent.createChooser(share, "Share Image"));
			});
		} catch(IOException e) {}

		/*
							// do all the sharing part
					// this is executed after saving was successful
					// Add it to the gallery
					Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
					Uri contentUri = Uri.fromFile(imageFile);
					mediaScanIntent.setData(contentUri);
					getActivity().sendBroadcast(mediaScanIntent);

					// If share is selected share it
					if(share) {
						Intent share = new Intent(Intent.ACTION_SEND);
						share.setType("image/png");
						share.putExtra(Intent.EXTRA_STREAM, contentUri);
						startActivity(Intent.createChooser(share, "Share Image"));
					}

					// If wallpaper is selected, set as wallpaper
					if(setAsWallpaper) {
					}

		 */
	}

	public void setWallpaper() {
		// FIXME fill with life.
		// this is only true if allowSettingWallpaper is true.
		/*WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
		try {
			wallpaperManager.setBitmap(getBitmap());
		} catch(IOException e) {
			Toast.makeText(getActivity(), e.getLocalizedMessage(),
					Toast.LENGTH_LONG).show();
			// could not set it as wallpaper, but since it was saved,
			// not such a big deal.
		}*/
	}
}
