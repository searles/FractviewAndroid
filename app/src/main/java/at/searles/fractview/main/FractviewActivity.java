package at.searles.fractview.main;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ListView;

import at.searles.fractview.R;
import at.searles.fractview.favorites.AddFavoritesDialogFragment;
import at.searles.fractview.parameters.ParameterAdapter;
import at.searles.fractview.parameters.ParameterLongSelectListener;
import at.searles.fractview.parameters.ParameterSelectListener;


// Activity is the glue between FractalCalculator and Views.
public class FractviewActivity extends Activity {

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
						// set item as selected to persist highlight
						selectMenuItem(menuItem);
						// close drawer when item is tapped
						drawerLayout.closeDrawers();

						// Add code here to update the UI based on the item selected
						// For example, swap UI fragments here

						return true;
					}
				});

		ListView navListView = findViewById(R.id.nav_list_view);

		// TODO Background dependant on theme!
		navListView.setBackgroundColor(0x80ffffff);

		initializeFractalFragment();
	}

	private void selectMenuItem(MenuItem menuItem) {
		// fixme
	}

	private void initializeFractalFragment() {
		FragmentManager fm = getFragmentManager();
		FractalProviderFragment fractalProviderFragment = (FractalProviderFragment) fm.findFragmentById(R.id.fractal_fragment);

		// init menu according to existing fragment
		initProviderMenu(fractalProviderFragment);
	}

	private void initProviderMenu(FractalProviderFragment fragment) {
		ParameterAdapter adapter = new ParameterAdapter(this, fragment.provider());

		ListView listView = findViewById(R.id.nav_list_view);

		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new ParameterSelectListener(fragment));

		listView.setOnItemLongClickListener(new ParameterLongSelectListener(fragment.provider()));
	}

	/**
	 * Opens a dialog to add the index-th fractal of the provider to favorites.
	 */
	private void addToFavorites(int index) {
		AddFavoritesDialogFragment.newInstance(index);
	}

//	@Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle presses on the action bar items
//        switch (item.getItemId()) {
//			case R.id.action_add_favorite: {
//				DialogHelper.inputText(this, "Add Favorite", "", new Commons.KeyAction() {
//					@Override
//					public void apply(String key) {
//						saveFavorite(key);
//					}
//				});
//			} return true;
//			case R.id.action_size: {
//				openChangeImageSizeDialog();
//
//			} return true;
//
//
//			case R.id.action_parameters: {
//				// FIXME Replace this case by swipe-in menu
//				Intent i = new Intent(MainActivity.this, ParameterEditorActivity.class);
//				i.putExtra(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(fractalFragment.fractal()));
//				startActivityForResult(i, PARAMETER_ACTIVITY_RETURN);
//			} return true;
//
//			case R.id.action_favorites: {
//				// show new activity
//				Intent i = new Intent(MainActivity.this, FavoritesListActivity.class);
//				startActivityForResult(i, BOOKMARK_ACTIVITY_RETURN);
//			} return true;
//
//			case R.id.action_demos: {
//				// show new activity
//				Intent i = new Intent(MainActivity.this, SourcesListActivity.class);
//				i.putExtra(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(fractalFragment.fractal()));
//				startActivityForResult(i, PRESETS_ACTIVITY_RETURN);
//			} return true;
//
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
//
//			case R.id.action_gui_settings: {
//				// FIXME replace by swipe-in
//				openUiSettingsDialog();
//			} return true;
//
//			case R.id.action_share: {
//				openShareDialog();
//			} return true;
//
//			case R.id.action_tutorial: {
//				// show new activity
//				Intent i = new Intent(MainActivity.this, TutorialActivity.class);
//				startActivity(i);
//			} return true;
//
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }


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
