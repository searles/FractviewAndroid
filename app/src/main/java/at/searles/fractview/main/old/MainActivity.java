package at.searles.fractview.main.old;

// Activity is the glue between FractalCalculator and Views.
@Deprecated
public class MainActivity /* extends Activity
		implements ActivityCompat.OnRequestPermissionsResultCallback,
		ShareModeDialogFragment.Callback*/ {

    //public static final int ALPHA_PREFERENCES = 0xaa000000;

	/*public static final int MAX_INIT_SIZE = 2048 * 1536;*/

//	public static final int PARAMETER_ACTIVITY_RETURN = 101;
//	public static final int PRESETS_ACTIVITY_RETURN = 102;
//	public static final int BOOKMARK_ACTIVITY_RETURN = 103;
//
//	public static final String WIDTH_LABEL = "width"; // FIXME Also used in ImageSizeDialog, put into res
//	public static final String HEIGHT_LABEL = "height"; // FIXME put into res.
//
//	public static final String RENDERSCRIPT_FRAGMENT_TAG = "92349831";
//	public static final String BITMAP_FRAGMENT_TAG = "234917643";
//	public static final String FRACTAL_FRAGMENT_TAG = "2asdfsdf";
//	private static final String SHARE_MODE_DIALOG_TAG = "593034kf";
//	private static final String SAVE_TO_MEDIA_TAG = "458hnof";
//	private static final String IMAGE_SIZE_DIALOG_TAG = "km9434f";
//
//	private static final int FAVORITES_ICON_SIZE = 64;
//	private static final int FALLBACK_DEFAULT_WIDTH = 640;
//	private static final int FALLBACK_DEFAULT_HEIGHT = 480;
//
//	/**
//	 * The following fragments and views need to cooperate.
//	 */
//    private FractalCalculator fractalCalculator;
//	private SingleFractalFragment fractalFragment;
//	private FractalCalculatorView imageView;
//
//	LinkedList<Runnable> destroyTasks = new LinkedList<>();
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//		Log.d(getClass().getName(), "onCreate");
//
//		// First, take care of the view.
//		setContentView(R.layout.main);
//
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // make sure that screen stays awake
//
//		super.onCreate(savedInstanceState); // this one (re-)creates the bitmap fragment on rotation.
//
//		imageView = (FractalCalculatorView) findViewById(R.id.mainBitmapFragmentView);
//
//		initRenderScriptFragment();
//		initFractalFragment();
//		initBitmapFragment(); // this adds a newly created bitmap fragment to the listener list in fractalfragment.
//
//		initListeners();
//	}
//
//	private void initRenderScriptFragment() {
//		FragmentManager fm = getFragmentManager();
//		CalculatorFragment initializationFragment = (CalculatorFragment) fm.findFragmentByTag(RENDERSCRIPT_FRAGMENT_TAG);
//
//		if(initializationFragment == null) {
//			Log.d(getClass().getName(), "creating CalculatorFragment");
//
//			initializationFragment = CalculatorFragment.newInstance();
//
//			FragmentTransaction transaction = fm.beginTransaction();
//			transaction.add(initializationFragment, RENDERSCRIPT_FRAGMENT_TAG);
//			transaction.commit();
//		} else {
//			Log.d(getClass().getName(), "CalculatorFragment already exists");
//		}
//	}
//
//	private void initFractalFragment() {
//		FragmentManager fm = getFragmentManager();
//
//		fractalFragment = (SingleFractalFragment) fm.findFragmentByTag(FRACTAL_FRAGMENT_TAG);
//
//		if(fractalFragment == null) {
//			String sourceCode = AssetsHelper.readSourcecode(getAssets(), "Default.fv");
//
//			Fractal initFractal = new Fractal(sourceCode, new HashMap<>());
//
//			fractalFragment = SingleFractalFragment.newInstance(initFractal);
//
//			FragmentTransaction transaction = getFragmentManager().beginTransaction();
//			transaction.add(fractalFragment, FRACTAL_FRAGMENT_TAG);
//			transaction.commitAllowingStateLoss(); // Question: Why should there be a stateloss?
//
//			Log.d(getClass().getName(), "creating FractalProviderFragment");
//		} else {
//			Log.d(getClass().getName(), "FractalProviderFragment already exists");
//		}
//	}
//
//	private void initBitmapFragment() {
//		FragmentManager fm = getFragmentManager();
//
//		fractalCalculator = (FractalCalculator) fm.findFragmentByTag(BITMAP_FRAGMENT_TAG);
//
//		if(fractalCalculator == null) {
//			Log.d(getClass().getName(), "creating FractalCalculator");
//
//			// fetch dimensions from preferences or display size.
//			// Get settings from shared preferences
//			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//
//			int defaultWidth = prefs.getInt(WIDTH_LABEL, -1);
//			int defaultHeight = prefs.getInt(HEIGHT_LABEL, -1);
//
//			if(defaultWidth == -1 || defaultHeight == -1) {
//				Point dim = screenDimensions(this);
//
//				defaultWidth = dim.x;
//				defaultHeight = dim.y;
//			}
//
//			fractalCalculator = FractalCalculator.newInstance(defaultWidth, defaultHeight);
//
//			FragmentTransaction transaction = getFragmentManager().beginTransaction();
//			transaction.add(fractalCalculator, BITMAP_FRAGMENT_TAG);
//			transaction.commitAllowingStateLoss(); // Question: Why should there be a stateloss?
//		}
//	}
//
//	private void initListeners() {
//		// call fractalfragment for zoom events
//		imageView.setCallBack(fractalFragment.createCallback());
//
//		if(fractalCalculator.isInitializing()) {
//			fractalFragment.addListener(fractalCalculator);
//		}
//
//		// initialize the view
//		FractalCalculatorListener viewListener = imageView.createListener();
//
//		fractalCalculator.addListener(viewListener);
//
//		if(fractalCalculator.isRunning()) {
//			// show progress bar
//			viewListener.drawerStarted(fractalCalculator);
//		}
//
//		destroyTasks.add(new Runnable() {
//			@Override
//			public void run() {
//				fractalCalculator.removeListener(viewListener);
//			}
//		});
//
//		if(fractalCalculator.bitmap() != null) {
//			imageView.scaleableImageView().setBitmap(fractalCalculator.bitmap());
//			// otherwise, we added the listener. It will inform the view
//			// when a bitmap is available.
//		}
//	}
//
//	@Override
//	public void onDestroy() {
//		imageView.dispose();
//
//		while (!destroyTasks.isEmpty()) {
//			destroyTasks.remove().run();
//		}
//
//		super.onDestroy();
//	}
//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu items for use in the action bar
//		MenuInflater inflater = getMenuInflater();
//		inflater.inflate(R.menu.activity_main, menu);
//
//		return super.onCreateOptionsMenu(menu);
//	}
//
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
//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		super.onActivityResult(requestCode, resultCode, data);
//
//		if(data != null) {
//			if (requestCode == PARAMETER_ACTIVITY_RETURN) {
//				if (resultCode == 1) { // = "Ok"
//					Fractal newFractal = BundleAdapter.bundleToFractal(data.getBundleExtra(SourcesListActivity.FRACTAL_INDENT_LABEL));
//					fractalFragment.setFractal(newFractal);
//				}
//			} else if (requestCode == BOOKMARK_ACTIVITY_RETURN) {
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
//		}
//	}
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
}