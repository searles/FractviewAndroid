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
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;

import at.searles.fractal.FavoriteEntry;
import at.searles.fractal.Fractal;
import at.searles.fractal.android.BundleAdapter;
import at.searles.fractview.bitmap.BitmapFragment;
import at.searles.fractview.bitmap.BitmapFragmentListener;
import at.searles.fractview.bitmap.ui.BitmapFragmentView;
import at.searles.fractview.editors.ImageSizeDialogFragment;
import at.searles.fractview.fractal.SingleFractalFragment;
import at.searles.fractview.renderscript.RenderScriptFragment;
import at.searles.fractview.saving.SaveAsDialogFragment;
import at.searles.fractview.saving.SaveFragment;
import at.searles.fractview.saving.ShareModeDialogFragment;
import at.searles.fractview.ui.DialogHelper;
import at.searles.tutorial.TutorialActivity;


// Activity is the glue between BitmapFragment and Views.
public class MainActivity extends Activity
		implements ActivityCompat.OnRequestPermissionsResultCallback,
		ShareModeDialogFragment.Callback {

    //public static final int ALPHA_PREFERENCES = 0xaa000000;

	/*public static final int MAX_INIT_SIZE = 2048 * 1536;*/

	public static final int PARAMETER_ACTIVITY_RETURN = 101;
	public static final int PRESETS_ACTIVITY_RETURN = 102;
	public static final int BOOKMARK_ACTIVITY_RETURN = 103;

	public static final String WIDTH_LABEL = "width"; // FIXME Also used in ImageSizeDialog, put into res
	public static final String HEIGHT_LABEL = "height"; // FIXME put into res.

	public static final String RENDERSCRIPT_FRAGMENT_TAG = "92349831";
	public static final String BITMAP_FRAGMENT_TAG = "234917643";
	public static final String FRACTAL_FRAGMENT_TAG = "2asdfsdf";
	private static final String SHARE_MODE_DIALOG_TAG = "593034kf";
	private static final String SAVE_TO_MEDIA_TAG = "458hnof";
	private static final String IMAGE_SIZE_DIALOG_TAG = "km9434f";

	private static final int FAVORITES_ICON_SIZE = 64;
	private static final int FALLBACK_DEFAULT_WIDTH = 640;
	private static final int FALLBACK_DEFAULT_HEIGHT = 480;

	/**
	 * The following fragments and views need to cooperate.
	 */
    private BitmapFragment bitmapFragment;
	private SingleFractalFragment fractalFragment;
	private BitmapFragmentView imageView;

	LinkedList<Runnable> destroyTasks = new LinkedList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
		Log.d("MA", "onCreate");

		// First, take care of the view.
		setContentView(R.layout.main);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // make sure that screen stays awake

		super.onCreate(savedInstanceState); // this one (re-)creates the bitmap fragment on rotation.

		imageView = (BitmapFragmentView) findViewById(R.id.mainBitmapFragmentView);

		initRenderScriptFragment();
		initFractalFragment();
		initBitmapFragment(); // this adds a newly created bitmap fragment to the listener list in fractalfragment.
	}
	
	private void initRenderScriptFragment() {
		FragmentManager fm = getFragmentManager();
		RenderScriptFragment renderScriptFragment = (RenderScriptFragment) fm.findFragmentByTag(RENDERSCRIPT_FRAGMENT_TAG);

		if(renderScriptFragment == null) {
			renderScriptFragment = RenderScriptFragment.newInstance();

			FragmentTransaction transaction = fm.beginTransaction();
			transaction.add(renderScriptFragment, RENDERSCRIPT_FRAGMENT_TAG);
			transaction.commit();
		}
	}

	private void initFractalFragment() {
		FragmentManager fm = getFragmentManager();

		fractalFragment = (SingleFractalFragment) fm.findFragmentByTag(FRACTAL_FRAGMENT_TAG);

		if(fractalFragment == null) {
			String sourceCode = AssetsHelper.readSourcecode(getAssets(), "Default.fv");

			Fractal initFractal = new Fractal(sourceCode, new HashMap<>());

			fractalFragment = SingleFractalFragment.newInstance(initFractal);

			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.add(fractalFragment, FRACTAL_FRAGMENT_TAG);
			transaction.commitAllowingStateLoss(); // Question: Why should there be a stateloss?
		}

		// call fractalfragment for zoom events
		imageView.setCallBack(fractalFragment.createCallback());
	}

	private void initBitmapFragment() {
		FragmentManager fm = getFragmentManager();

		bitmapFragment = (BitmapFragment) fm.findFragmentByTag(BITMAP_FRAGMENT_TAG);

		if(bitmapFragment == null) {
			// fetch dimensions from preferences or display size.
			// Get settings from shared preferences
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

			int defaultWidth = prefs.getInt(WIDTH_LABEL, -1);
			int defaultHeight = prefs.getInt(HEIGHT_LABEL, -1);

			Point dim = screenDimensions(this);

			int displayWidth = dim.x;
			int displayHeight = dim.y;

			bitmapFragment = BitmapFragment.newInstance(defaultWidth, defaultHeight, displayWidth, displayHeight);

			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.add(bitmapFragment, BITMAP_FRAGMENT_TAG);
			transaction.commitAllowingStateLoss(); // Question: Why should there be a stateloss?

			fractalFragment.addListener(bitmapFragment);
		}

		// initialize the view
		BitmapFragmentListener viewListener = imageView.createListener();

		bitmapFragment.addBitmapFragmentListener(viewListener);
		destroyTasks.add(new Runnable() {
			@Override
			public void run() {
				bitmapFragment.removeBitmapFragmentListener(viewListener);
			}
		});

		if(bitmapFragment.bitmap() != null) {
			imageView.scaleableImageView().setBitmap(bitmapFragment.bitmap());
			// otherwise, we added the listener. It will inform the view
			// when a bitmap is available.
		}
	}

	@Override
	public void onDestroy() {
		imageView.dispose();

		while (!destroyTasks.isEmpty()) {
			destroyTasks.remove().run();
		}

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main, menu);

		return super.onCreateOptionsMenu(menu);
	}

	public static final int SAVE_TO_MEDIA_PERMISSIONS = 105;
	public static final int WALLPAPER_PERMISSIONS = 106;

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
			case R.id.action_size: {
				openChangeImageSizeDialog();

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
				// FIXME Replace this activity
				Intent i = new Intent(MainActivity.this, ParameterEditorActivity.class);
				i.putExtra(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(fractalFragment.fractal()));
				startActivityForResult(i, PARAMETER_ACTIVITY_RETURN);
			} return true;

			case R.id.action_favorites: {
				// show new activity
				Intent i = new Intent(MainActivity.this, FavoritesListActivity.class);
				startActivityForResult(i, BOOKMARK_ACTIVITY_RETURN);
			} return true;

			case R.id.action_demos: {
				// show new activity
				Intent i = new Intent(MainActivity.this, SourcesListActivity.class);
				i.putExtra(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(fractalFragment.fractal()));
				startActivityForResult(i, PRESETS_ACTIVITY_RETURN);
			} return true;

			case R.id.action_paste_from_clipboard: {
				// paste from clipboard
				Fractal newFractal = ClipboardHelper.pasteFractal(this);

				if(newFractal != null) {
					fractalFragment.setFractal(newFractal);
					// otherwise a message was already shown
				}
			} return true;

			case R.id.action_copy_to_clipboard: {
				// copy to clipboard
				ClipboardHelper.copyFractal(this, fractalFragment.fractal());
			} return true;

			case R.id.action_gui_settings: {
				openUiSettingsDialog();
			} return true;

			case R.id.action_share: {
				openShareDialog();
			} return true;

			case R.id.action_tutorial: {
				// show new activity
				Intent i = new Intent(MainActivity.this, TutorialActivity.class);
				startActivity(i);
			} return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

	// ===================================================================

	private void openShareDialog() {
		ShareModeDialogFragment shareModeDialogFragment = ShareModeDialogFragment.newInstance();
		shareModeDialogFragment.show(getFragmentManager(), SHARE_MODE_DIALOG_TAG);
	}

	@Override
	public void onShareModeResult(ShareModeDialogFragment.Result result) {
		switch (result) {
			case Share:
				SaveFragment.registerNewInstanceForParent(bitmapFragment);
				return;
			case Save:
				int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
				int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

				if(readPermission != PackageManager.PERMISSION_GRANTED || writePermission != PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(MainActivity.this,
							new String[]{
									Manifest.permission.READ_EXTERNAL_STORAGE,
									Manifest.permission.WRITE_EXTERNAL_STORAGE
							}, SAVE_TO_MEDIA_PERMISSIONS);
					return;
				}

				SaveAsDialogFragment fragment = SaveAsDialogFragment.newInstance();
				fragment.show(getFragmentManager(), SAVE_TO_MEDIA_TAG);

				return;
			case Wallpaper:
				int wallpaperPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SET_WALLPAPER);

				if(wallpaperPermission != PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(MainActivity.this,
							new String[]{
									Manifest.permission.SET_WALLPAPER
							}, WALLPAPER_PERMISSIONS);
					return;
				}

				// FIXME SaveFragment.createSetWallpaper().init(bitmapFragment);

				return;
			default:
				throw new UnsupportedOperationException();
		}
	}

	//FIXME Override in API 23
	@SuppressLint("Override")
	public void onRequestPermissionsResult(int requestCode,
										   @NotNull String permissions[], @NotNull int[] grantResults) {
		switch (requestCode) {
			case SAVE_TO_MEDIA_PERMISSIONS:
				if(grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
					DialogHelper.error(this, "No permission to write to external storage");
					return;
				}

				// try again...
				onShareModeResult(ShareModeDialogFragment.Result.Save);
				return;
			case WALLPAPER_PERMISSIONS:
				if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
					DialogHelper.error(this, "No permission to set wallpaper");
					return;
				}

				onShareModeResult(ShareModeDialogFragment.Result.Wallpaper);
				return;
			default:
				throw new UnsupportedOperationException();
		}
	}

	// ===================================================================

	private void openUiSettingsDialog() {
		// FIXME put into swipe in list.
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
	}

	private void openChangeImageSizeDialog() {
		ImageSizeDialogFragment fragment = ImageSizeDialogFragment.newInstance(BITMAP_FRAGMENT_TAG, bitmapFragment.width(), bitmapFragment.height());
		fragment.show(getFragmentManager(), IMAGE_SIZE_DIALOG_TAG);
	}

	public void saveFavorite(String name) {
		if(name.isEmpty()) {
			Toast.makeText(MainActivity.this, "ERROR: Name must not be empty", Toast.LENGTH_LONG).show();
			return;
		}

		Fractal fractal = fractalFragment.fractal();

		// create icon out of bitmap
		Bitmap icon = Commons.createIcon(bitmapFragment.bitmap(), FAVORITES_ICON_SIZE);

		FavoriteEntry fav = new FavoriteEntry(icon, fractal, Commons.fancyTimestamp());

		SharedPrefsHelper.storeInSharedPreferences(this, name, fav, FavoritesListActivity.FAVORITES_SHARED_PREF);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(data != null) {
			if (requestCode == PARAMETER_ACTIVITY_RETURN) {
				if (resultCode == 1) { // = "Ok"
					Fractal newFractal = BundleAdapter.bundleToFractal(data.getBundleExtra(SourcesListActivity.FRACTAL_INDENT_LABEL));
					fractalFragment.setFractal(newFractal);
				}
			} else if (requestCode == BOOKMARK_ACTIVITY_RETURN) {
				if (resultCode == 1) { // = "a fractal was selected"
					Fractal newFractal = BundleAdapter.bundleToFractal(data.getBundleExtra(SourcesListActivity.FRACTAL_INDENT_LABEL));
					fractalFragment.setFractal(newFractal);
				}
			} else if (requestCode == PRESETS_ACTIVITY_RETURN) {
				if (resultCode == 1) {
					Fractal newFractal = BundleAdapter.bundleToFractal(data.getBundleExtra(SourcesListActivity.FRACTAL_INDENT_LABEL));
					fractalFragment.setFractal(newFractal);
				}
			}
		}
	}


//	// =======================================================================
//	// ============= Some History ... ========================================
//	// =======================================================================

	@Override
	public void onBackPressed() {
		// first, send it to image view
		if(imageView.backButtonAction()) return;
		if(fractalFragment.historyBack()) return;
		super.onBackPressed();
	}


	public static Point screenDimensions(Context context) {
		// FIXME put into commons.
		Point dim = new Point();
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

		Display display = wm.getDefaultDisplay();

		if(display == null) {
			Log.e(MainActivity.class.getName(), "default display was null");
			dim.set(FALLBACK_DEFAULT_WIDTH, FALLBACK_DEFAULT_HEIGHT);
		} else {
			wm.getDefaultDisplay().getSize(dim);
		}

		// if width < height swap.
		if(dim.x < dim.y) {
			//noinspection SuspiciousNameCombination
			dim.set(dim.y, dim.x);
		}

		return dim;
	}
}
