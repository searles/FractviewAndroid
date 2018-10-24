package at.searles.fractview.main;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ListView;

import at.searles.fractal.FractalExternData;
import at.searles.fractview.R;
import at.searles.fractview.SourceEditorActivity;
import at.searles.fractview.favorites.AddFavoritesDialogFragment;
import at.searles.fractview.favorites.FavoritesListActivity;
import at.searles.fractview.parameters.ParameterAdapter;
import at.searles.fractview.parameters.ParameterLongSelectListener;
import at.searles.fractview.parameters.ParameterSelectListener;
import at.searles.tutorial.TutorialActivity;


// Activity is the glue between FractalCalculator and Views.
public class FractviewActivity extends Activity {

	private static final String ADD_FRAGMENT_TAG = "add_fragment";
	private static final int FAVORITES_RETURN = 213;
	private static final int SOURCE_ACTIVITY_RETURN = 631;
	private ParameterAdapter adapter;
	private FractalProviderFragment fractalProviderFragment;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); // this one (re-)creates all fragments on rotation.

		Log.d(getClass().getName(), "creating activity, savedInstanceState=" + savedInstanceState);

		// First, take care of the view.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // make sure that screen stays awake

		setContentView(R.layout.fractview_layout); // will create fragment

		DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);

		NavigationView navigationView = findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(
				new NavigationView.OnNavigationItemSelectedListener() {
					@Override
					public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
						selectMenuItem(menuItem);

						// close drawer when item is tapped
						drawerLayout.closeDrawers();

						return true;
					}
				});

		ListView navListView = findViewById(R.id.nav_list_view);

		// FIXME Background should depend on theme!
		navListView.setBackgroundColor(0x80ffffff);

		initializeFractalFragment();
	}

	@Override
	protected void onDestroy() {
		// remove adapter from listener
		fractalProviderFragment.removeParameterMapListener(adapter);

		super.onDestroy();
	}

	private void initializeFractalFragment() {
		FragmentManager fm = getFragmentManager();
		this.fractalProviderFragment = (FractalProviderFragment) fm.findFragmentById(R.id.fractal_fragment);
		initParameterMenu();
	}

	private void initParameterMenu() {
		this.adapter = new ParameterAdapter(this, fractalProviderFragment);

		ListView listView = findViewById(R.id.nav_list_view);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new ParameterSelectListener(fractalProviderFragment));
		listView.setOnItemLongClickListener(new ParameterLongSelectListener(fractalProviderFragment));

		fractalProviderFragment.addParameterMapListener(adapter);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(data != null) {
			if (requestCode == SOURCE_ACTIVITY_RETURN) {
				if (resultCode == 1) { // = "Ok"
					int owner = data.getIntExtra(SourceEditorActivity.OWNER_LABEL, -1);
					String source = data.getStringExtra(SourceEditorActivity.SOURCE_LABEL);
					fractalProviderFragment.setParameter(FractalExternData.SOURCE_LABEL, owner, source);
				}
			}
//			else if (requestCode == BOOKMARK_ACTIVITY_RETURN) {
//				if (resultCode == 1) { // = "a fractal was selected"
//					Fractal newFractal = BundleAdapter.bundleToFractal(data.getBundleExtra(SourcesListActivity.FRACTAL_INDENT_LABEL));
//					fractalFragment.setFractal(newFractal);
//				}
//			} else if (requestCode == PRESETS_ACTIVITY_RETURN) {
//				if (resultCode == 1) {
//					Fractal newFractal = BundleAdapter.bundleToFractal(data.getBundleExtra(SourcesListActivity.FRACTAL_INDENT_LABEL));
//					fractalFragment.setFractal(newFractal);
//				}
//			}
		}
	}

	private void selectMenuItem(MenuItem menuItem) {
		switch(menuItem.getItemId()) {
			case R.id.action_add_fractal:
				addFractal("juliaset"); // FIXME: allow selection of boolean keys
				return;
			case R.id.action_remove_fractal_1:
				removeFractal(0); // FIXME dynamically
				return;
			case R.id.action_remove_fractal_2:
				removeFractal(1); // FIXME dynamically
				return;
			case R.id.action_add_point: // FIXME allow setting from parameter menu
				// get provider view
				fractalProviderFragment.addInteractivePoint("juliapoint");
				return;
			case R.id.action_tutorial:
				startActivity(new Intent(this, TutorialActivity.class));
				return;
			case R.id.action_favorites:
				startActivityForResult(new Intent(this, FavoritesListActivity.class), FAVORITES_RETURN);
				return;
//			case R.id.action_demos:
//				{
//					// show new activity
//					Intent i = new Intent(FractviewActivity.this, SelectAssetActivity.class);
//					i.putExtra(SelectAssetActivity.FRACTAL_INDENT_LABEL, BundleAdapter.toBundle(fractalFragment.fractal()));
//					startActivityForResult(i, PRESETS_ACTIVITY_RETURN);
//				}
//			return;
//			case R.id.action_size:
//				openChangeImageSizeDialog();
//				return;
//			case R.id.action_paste_from_clipboard: {
//				// paste from clipboard
//				Fractal newFractal = ClipboardHelper.pasteFractal(this);
//
//				if(newFractal != null) {
//					fractalFragment.setFractal(newFractal);
//					// otherwise a message was already shown
//				}
//			} return true;
//
//			case R.id.action_copy_to_clipboard: {
//				// copy to clipboard
//				ClipboardHelper.copyFractal(this, fractalFragment.fractal());
//			} return true;
//			case R.id.action_share: {
//				openShareDialog();
//			} return true;
//
//			case R.id.action_gui_settings: {
//				// FIXME replace by swipe-in
//				openUiSettingsDialog();
//			} return true;
//
//
//
//            default:
//                return super.onOptionsItemSelected(item);
        }
    }

	private void addFractal(String booleanKey) {
		FractalProviderFragment fragment = (FractalProviderFragment) getFragmentManager().findFragmentById(R.id.fractal_fragment);
		fragment.addFractal(booleanKey, "Scale", booleanKey);

		// TODO: update menu
	}

	private void removeFractal(int index) {
		fractalProviderFragment.removeFractal(index);

		// TODO: update menu
	}

	/**
	 * Opens a dialog to add the index-th fractal of the provider to favorites.
	 */
	private void addToFavorites() {
		AddFavoritesDialogFragment fragment = AddFavoritesDialogFragment.newInstance(0); // fixme index!
		fragment.show(getFragmentManager(), ADD_FRAGMENT_TAG);
	}


//	public static final int SAVE_TO_MEDIA_PERMISSIONS = 105;
//	public static final int WALLPAPER_PERMISSIONS = 106;
//
//
//	// ===================================================================
//
//	private void openShareDialog() {
//		ShareModeDialogFragment shareModeDialogFragment = ShareModeDialogFragment.newInstance();
//		shareModeDialogFragment.show(getFragmentManager(), SHARE_MODE_DIALOG_TAG);
//	}
//
//	@Override
//	public void onShareModeResult(ShareModeDialogFragment.Result result) {
//		switch (result) {
//			case Share:
//				fractalCalculator.registerFragmentAsChild(ShareBitmapFragment.newInstance(), SaveInBackgroundFragment.SAVE_FRAGMENT_TAG);
//				return;
//			case Save:
//				int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
//				int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//
//				if(readPermission != PackageManager.PERMISSION_GRANTED || writePermission != PackageManager.PERMISSION_GRANTED) {
//					ActivityCompat.requestPermissions(MainActivity.this,
//							new String[]{
//									Manifest.permission.READ_EXTERNAL_STORAGE,
//									Manifest.permission.WRITE_EXTERNAL_STORAGE
//							}, SAVE_TO_MEDIA_PERMISSIONS);
//					return;
//				}
//
//				EnterFilenameDialogFragment fragment = EnterFilenameDialogFragment.newInstance(BITMAP_FRAGMENT_TAG);
//				fragment.show(getFragmentManager(), SAVE_TO_MEDIA_TAG);
//
//				return;
//			case Wallpaper:
//				int wallpaperPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SET_WALLPAPER);
//
//				if(wallpaperPermission != PackageManager.PERMISSION_GRANTED) {
//					ActivityCompat.requestPermissions(MainActivity.this,
//							new String[]{
//									Manifest.permission.SET_WALLPAPER
//							}, WALLPAPER_PERMISSIONS);
//					return;
//				}
//
//				fractalCalculator.registerFragmentAsChild(SetWallpaperFragment.newInstance(), SaveInBackgroundFragment.SAVE_FRAGMENT_TAG);
//
//				return;
//			default:
//				throw new UnsupportedOperationException();
//		}
//	}
//
//	//FIXME Override in API 23
//	@SuppressLint("Override")
//	public void onRequestPermissionsResult(int requestCode,
//										   @NotNull String permissions[], @NotNull int[] grantResults) {
//		switch (requestCode) {
//			case SAVE_TO_MEDIA_PERMISSIONS:
//				if(grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
//					DialogHelper.error(this, "No permission to write to external storage");
//					return;
//				}
//
//				// try again...
//				onShareModeResult(ShareModeDialogFragment.Result.Save);
//				return;
//			case WALLPAPER_PERMISSIONS:
//				if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//					DialogHelper.error(this, "No permission to set wallpaper");
//					return;
//				}
//
//				onShareModeResult(ShareModeDialogFragment.Result.Wallpaper);
//				return;
//			default:
//				throw new UnsupportedOperationException();
//		}
//	}
//
//	// ===================================================================
//
//	private void openUiSettingsDialog() {
//		// FIXME put into swipe in list.
//		// show alert dialog with two checkboxes
//		final CharSequence[] items = {"Show Grid","Rotation Lock", "Confirm Zoom with Tab", "Deactivate Zoom"};
//
//		new AlertDialog.Builder(this)
//                .setCancelable(true)
//                .setMultiChoiceItems(items,
//                        new boolean[]{
//                                imageView.scaleableImageView().getShowGrid(),
//                                imageView.scaleableImageView().getRotationLock(),
//                                imageView.scaleableImageView().getConfirmZoom(),
//                                imageView.scaleableImageView().getDeactivateZoom()
//                        },
//                        new DialogInterface.OnMultiChoiceClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
//                        // fixme can move the editor to FractalCalculatorView?
//                        switch(indexSelected) {
//                            case 0: {
//                                // show/hide grid
//                                imageView.scaleableImageView().setShowGrid(isChecked);
//                            } break;
//                            case 1: {
//                                // rotation lock
//                                imageView.scaleableImageView().setRotationLock(isChecked);
//                            } break;
//                            case 2: {
//                                // confirm edit with a tab
//                                imageView.scaleableImageView().setConfirmZoom(isChecked);
//                            } break;
//                            case 3: {
//                                // deactivate zoom
//                                imageView.scaleableImageView().setDeactivateZoom(isChecked);
//                            } break;
//                        }
//                    }
//                }).setPositiveButton("Close", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int id) {}
//                }).create().show();
//	}
//
//	private void openChangeImageSizeDialog() {
//		ImageSizeDialogFragment fragment = ImageSizeDialogFragment.newInstance(BITMAP_FRAGMENT_TAG, fractalCalculator.width(), fractalCalculator.height());
//		fragment.show(getFragmentManager(), IMAGE_SIZE_DIALOG_TAG);
//	}
//
//	public void saveFavorite(String name) {
//		if(name.isEmpty()) {
//			Toast.makeText(MainActivity.this, "ERROR: Name must not be empty", Toast.LENGTH_LONG).show();
//			return;
//		}
//
//		Fractal fractal = fractalFragment.fractal();
//
//		// create icon out of bitmap
//		Bitmap icon = Commons.createIcon(fractalCalculator.bitmap(), FAVORITES_ICON_SIZE);
//
//		FavoriteEntry fav = new FavoriteEntry(icon, fractal, Commons.fancyTimestamp());
//
//		SharedPrefsHelper.storeInSharedPreferences(this, name, fav, FavoritesListActivity.FAVORITES_SHARED_PREF);
//	}
//
//
//
////	// =======================================================================
////	// ============= Some History ... ========================================
////	// =======================================================================
//
//	@Override
//	public void onBackPressed() {
//		// first, send it to image view
//		if(imageView.backButtonAction()) return;
//		if(fractalFragment.historyBack()) return;
//		super.onBackPressed();
//	}
//
//
//	public static Point screenDimensions(Context context) {
//		// FIXME put into commons.
//		Point dim = new Point();
//		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//
//		Display display = wm != null ? wm.getDefaultDisplay() : null;
//
//		if(display == null) {
//			Log.d(MainActivity.class.getName(), "default display was null");
//			dim.set(FALLBACK_DEFAULT_WIDTH, FALLBACK_DEFAULT_HEIGHT);
//		} else {
//			wm.getDefaultDisplay().getSize(dim);
//		}
//
//		// if width < height swap.
//		if(dim.x < dim.y) {
//			//noinspection SuspiciousNameCombination
//			dim.set(dim.y, dim.x);
//		}
//
//		return dim;
//	}

	private void initMenu() {
    	// TODO
		/*
		 * [X] Hide top bar
		 * Edit (opens swipe-in from left)
		 *   -> FractalProviderEditor
		 * Settings (opens swipe-in from right)
		 *   -> Scroll-Lock (no edits)
		 *   -> Rotation-Lock
		 *   -> Keep centered
		 *   -> Edit points - opens dialog with all editable (cplx and expr).
		 *                    Warn if an expr is reset to 0:0 because it is not a number.
		 *   -> Show grid
		 *   -> Resolution
		 * Demos -> opens AssetsActivity.
		 * Direct Save/Share
		 * Favorites (opens favorites activity)
		 * Add #name to Favorites (opens test dialog) (multiple times)
		 * (conditional) Split view -> opens dialog for all extern bools. Bool is fixed, scale is not shared.
		 * (conditional) Show only #name
		 * Render at different resolution - opens dialog with resolution (option 'keep ratio') and Supersampling.
		 */
	}
}
