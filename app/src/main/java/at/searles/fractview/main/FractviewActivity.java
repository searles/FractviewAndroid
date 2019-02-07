package at.searles.fractview.main;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ListView;

import org.jetbrains.annotations.NotNull;

import at.searles.fractal.Fractal;
import at.searles.fractal.data.FractalData;
import at.searles.fractview.R;
import at.searles.fractview.SourceEditorActivity;
import at.searles.fractview.assets.SelectAssetActivity;
import at.searles.fractview.favorites.FavoritesActivity;
import at.searles.fractview.fractal.BundleAdapter;
import at.searles.fractview.parameters.ParameterAdapter;
import at.searles.fractview.parameters.ParameterLongSelectListener;
import at.searles.fractview.parameters.ParameterSelectListener;
import at.searles.tutorial.TutorialActivity;


// Activity is the glue between FractalCalculator and Views.
public class FractviewActivity extends Activity {

	private static final int FRACTAL_RETURN = 213;
	private static final int SOURCE_ACTIVITY_RETURN = 631;

    public static final int WALLPAPER_PERMISSIONS = 352349;
	public static final int SAVE_TO_MEDIA_PERMISSIONS = 265324;

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

		NavigationView navigationView = findViewById(R.id.menu_nav_view);
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

		ListView navListView = findViewById(R.id.parameter_nav_list_view);

		// FIXME Background should depend on theme!
		navListView.setBackgroundColor(0x80ffffff);

		initializeFractalFragment();
	}

	@Override
	protected void onDestroy() {
		// remove adapter from listener
		fractalProviderFragment.removeListener(adapter);

		super.onDestroy();
	}

	private void initializeFractalFragment() {
		FragmentManager fm = getFragmentManager();
		this.fractalProviderFragment = (FractalProviderFragment) fm.findFragmentById(R.id.fractal_fragment);
		initParameterMenu();
	}

	private void initParameterMenu() {
		this.adapter = new ParameterAdapter(this, fractalProviderFragment);

		ListView listView = findViewById(R.id.parameter_nav_list_view);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new ParameterSelectListener(fractalProviderFragment));
		listView.setOnItemLongClickListener(new ParameterLongSelectListener(fractalProviderFragment));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(data != null) {
			if (requestCode == SOURCE_ACTIVITY_RETURN) {
				if (resultCode == 1) { // = "Ok"
					int owner = data.getIntExtra(SourceEditorActivity.OWNER_LABEL, -1);
					String source = data.getStringExtra(SourceEditorActivity.SOURCE_LABEL);
					fractalProviderFragment.setParameterValue(Fractal.SOURCE_LABEL, owner, source);
				}
			}
			else if (requestCode == FRACTAL_RETURN) {
				if (resultCode == 1) { // = "a fractal was selected"
					FractalData newFractal = BundleAdapter.fractalFromBundle(data.getBundleExtra(FavoritesActivity.FRACTAL_INDENT_LABEL));
					fractalProviderFragment.setKeyFractal(newFractal);
				}
			}
		}
	}

	private void selectMenuItem(MenuItem menuItem) {
		switch(menuItem.getItemId()) {
			case R.id.action_forward:
				fractalProviderFragment.historyForward();
				return;
			case R.id.action_demos:
				// show new activity
				Intent i = new Intent(FractviewActivity.this, SelectAssetActivity.class);
				i.putExtra(SelectAssetActivity.IN_FRACTAL_LABEL, BundleAdapter.toBundle(fractalProviderFragment.getKeyFractal()));
				startActivityForResult(i, FRACTAL_RETURN); // return is a fractal
				return;
			case R.id.action_favorites:
				startActivityForResult(new Intent(this, FavoritesActivity.class), FRACTAL_RETURN);
				return;
			case R.id.action_tutorial:
				startActivity(new Intent(this, TutorialActivity.class));
				return;
			case R.id.action_add_fractal_view:
				fractalProviderFragment.addFractalFromKey();
				return;
			case R.id.action_remove_fractal_view:
				fractalProviderFragment.removeFractalFromKey();

				if(fractalProviderFragment.fractalCount() == 0) {
					// add new default view.
					fractalProviderFragment.addDefaultFractal();
				}

				return;
			case R.id.action_add_favorite:
				// call from fractal because of child-fragment-manager
				fractalProviderFragment.showAddToFavoritesDialog();
				return;
			case R.id.action_share:
				fractalProviderFragment.showShareOrSaveDialog();
				return;
			case R.id.action_size:
				fractalProviderFragment.showChangeSizeDialog();
				return;
 			case R.id.action_gui_settings:
				openUiSettingsDialog();
				return;
            default:
            	throw new IllegalArgumentException("option not implemented");
        }
    }

	private void openUiSettingsDialog() {

		// UI Settings:
		// [x] Show Grid
		// [x] Activate On-Screen Scaling
		//   [x] Confirm Zoom with Tab
		//   [x] Lock Rotation
		//   [x] Lock Center
		// --------------------
		// [x] Apply to all views
		final CharSequence[] items = {"Show Grid","Rotation Lock", "Confirm Zoom with Tab", "Deactivate Zoom"};

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
	}

	@Override
	public void onBackPressed() {
		// send it to the provider
		fractalProviderFragment.onBackPressed();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   @NotNull String permissions[], @NotNull int[] grantResults) {
		switch (requestCode) {
			case SAVE_TO_MEDIA_PERMISSIONS:
				if(grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
					return;
				}

				// try again...
				fractalProviderFragment.showSaveBitmapDialog();
				return;
			case WALLPAPER_PERMISSIONS:
				if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
					return;
				}

				// try again
				fractalProviderFragment.setBitmapAsWallpaper();
				return;
			default:
				throw new UnsupportedOperationException();
		}
	}

	// ===================================================================


////	// =======================================================================
////	// ============= Some History ... ========================================
////	// =======================================================================
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
//		}e
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
