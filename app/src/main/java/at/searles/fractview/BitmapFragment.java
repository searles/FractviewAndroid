package at.searles.fractview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import at.searles.fractal.Drawer;
import at.searles.fractal.Fractal;
import at.searles.fractal.History;
import at.searles.fractal.android.BundleAdapter;
import at.searles.fractview.ui.DialogHelper;
import at.searles.math.Scale;
import at.searles.meelan.CompileException;

/**
 * This class is the glue maintaining all parameters for drawing the fractal
 * and managing the threads including their interruption.
 *
 *
 * Possible Edit-Actions:
 *     * There is a new fractal (Bookmark/ParameterEdit/Source/History)
 *     ==> call setFractal
 *
 *     * There is a new scale (Display action)
 *     ==> call setScale
 *
 *     * There is a new image size
 *     ==> call setSizeUnsafe
 *
 *     * Editor:
 *         pre-exec: increment cancel-counter
 *         do-bg: Fetch lock, wait for termination and do edit. At end of it, decrement cancel and release lock.
 *         post-exec: check cancel-counter and if necessary start drawing.
 *
 *     * Communication Drawer <=> BMF:
 *         BMF fetches progress from drawer
 *         BMF sends cancel-request to drawer
 *         BMF can check whether Drawer is finished
 *         BMF can block thread until drawer is finished (for edit)
 *
 *
 *
 * An Editor
 */
public class BitmapFragment extends Fragment {

	public static BitmapFragment newInstance(int width, int height, Fractal fractal) {
		// set initial size
		Bundle bundle = new Bundle();

		bundle.putInt("width", width);
		bundle.putInt("height", height);

		Bundle fractalBundle = BundleAdapter.fractalToBundle(fractal);
		bundle.putBundle("fractal", fractalBundle);

		BitmapFragment ft = new BitmapFragment();
		ft.setArguments(bundle);

		return ft;
	}

    // Allocations and various data
    private int height, width;
	private Fractal fractal;
	private Bitmap bitmap;

	// Now the drawing stuff
	private Drawer drawer;

	// if true, the drawing is currently running and the drawer should
	// not be modified. Only modified in UI thread
	private boolean isRunning;

    private List<BitmapFragmentListener> listeners = new LinkedList<>();

    public void addBitmapFragmentListener(BitmapFragmentListener listener) {
		Log.d(getClass().getName(), "adding listener " + listener);
        listeners.add(listener);
    }

    public void removeBitmapFragmentListener(BitmapFragmentListener listener) {
		Log.d(getClass().getName(), "removing listener " + listener);

        if(!listeners.remove(listener)) {
            Log.e(getClass().getName(), "Trying to remove an inexistent listener!");
        }
    }

	private List<BitmapFragmentPlugin> plugins = new LinkedList<>();

	public void addBitmapFragmentPlugin(BitmapFragmentPlugin plugin) {
		Log.d(getClass().getName(), "adding plugin " + plugin);
		plugins.add(plugin);
	}

	public void removeBitmapFragmentPlugin(BitmapFragmentPlugin plugin) {
		Log.d(getClass().getName(), "remove plugin " + plugin);
		if(!plugins.remove(plugin)) {
			Log.e(getClass().getName(), "Trying to remove an inexistent plugin!");
		}
	}


	/**
	 * List of past fractals that were drawn using this bitmap fragment.
	 */
	private History history = new History();

	@Override
	public void onAttach(Activity context) {
		// Deprecated but Android SDK is buggy here!
		Log.d("BMF", "onAttach");
		super.onAttach(context);

		for(BitmapFragmentPlugin plugin : plugins) {
			plugin.attachContext(context);
		}

		// we are in the UI-thread and if this was the first start,
		// onCreate was not yet called. Good point to initialize
		// the RS-Object because it needs the activity and we are
		// in the UI-thread, so no race-conditions.
		if(this.drawer == null) {
			Log.d("BMF", "First time, onAttach was called...");
			this.drawer = new RenderScriptDrawer(context);

            this.drawer.setListener(
                new Drawer.DrawerListener() {
				@Override
				public void bitmapUpdated(boolean firstUpdate) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
						@Override
						public void run() {
                            for(BitmapFragmentListener listener : listeners) {
								if(firstUpdate) {
									listener.previewGenerated(BitmapFragment.this);
								} else {
									listener.bitmapUpdated(BitmapFragment.this);
								}
                            }
						}
					});
				}

				@Override
				public void finished() {
					Log.d(getClass().getName(), "drawer is finished");
					new Handler(Looper.getMainLooper()).post(new Runnable() {
						@Override
						public void run() {
							if(!editors.isEmpty()) {
								for(Runnable editor : editors) {
									editor.run();
								}
								editors.clear();
								drawer.clearRequestEdit();
								startBackgroundTask();
							} else {
								isRunning = false;

								for(BitmapFragmentListener listener : listeners) {
									listener.drawerFinished(-1, BitmapFragment.this);
								}
							}
						}
					});
				}
			});
		}

		if(this.bitmap != null) {
            for(BitmapFragmentListener listener : listeners) {
                listener.newBitmapCreated(bitmap, this); // in case of rotation, this fragment is preserved
            }
		}
	}

	@Override
	public void onDetach() {
		Log.d("BMF", "onDetach");
		super.onDetach();

		for(BitmapFragmentPlugin plugin : plugins) {
			plugin.detach();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d("BMF", "onCreate");

		super.onCreate(savedInstanceState);
		setRetainInstance(true); // preserve this one on rotation

		// Read initial width/height
		this.width = getArguments().getInt("width");
		this.height = getArguments().getInt("height");

		if(this.width <= 0 || this.height <= 0) {
			throw new IllegalArgumentException("width was " + width + ", height was " + height);
		}

		// create bitmap
		this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		for(BitmapFragmentListener listener : listeners) {
			listener.newBitmapCreated(bitmap, this); // tell others about it.
		}

		// and also the fractal.
		this.fractal = BundleAdapter.bundleToFractal(getArguments().getBundle(SourcesListActivity.FRACTAL_INDENT_LABEL));

		if(this.fractal == null) throw new IllegalArgumentException("no fractal in arguments!");

		// The fractal must be compilable! If it is a user-defined one
		// it was checked before it was put into the parcel.

		// the initial fractal must always be compilable, otherwise it is no fun.
		try {
			// Fractal is guaranteed to compile
			this.fractal.parse();
			this.fractal.compile();
		} catch(CompileException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("initial fractal is not compilable: " + e.getMessage());
		}

		// initialize drawer. This might take some time, thus do it from
		// a plugin.
		BitmapFragmentPlugin initPlugin = new BitmapFragmentPlugin() {

			BitmapFragmentPlugin self = this;
			AlertDialog dialog = null;

			@Override
			public void init(BitmapFragment fragment) {
				fragment.addBitmapFragmentPlugin(this);

				if(fragment.getActivity() != null) {
					attachContext(fragment.getActivity());
				}

				new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void...ignore) {
						Log.d(getClass().getName(), "init drawer");

						drawer.init(bitmap, fractal);
						return null;
					}

					@Override
					protected void onPostExecute(Void ignore) {
						Log.d(getClass().getName(), "init is done");

						if(dialog != null) {
							dialog.dismiss();
						}

						removeBitmapFragmentPlugin(self);
						drawer.setFractal(fractal);

						for(BitmapFragmentListener listener : listeners) {
							listener.initializationFinished();
						}

						startBackgroundTask();
					}
				}.execute();
			}

			@Override
			public void attachContext(Activity context) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Initializing Fractview");
				builder.setMessage("After the app cache was cleaned this might take a few seconds...\n\n\nThank you for using Fractview!");
				builder.setCancelable(false);

				dialog = builder.show();
			}

			@Override
			public void detach() {
				dialog = null;
			}
		};

		initPlugin.init(this);
	}

	public float progress() {
		return drawer.progress();
	}

	public boolean isRunning() {
		return isRunning;
	}

	// ------------------------
	// Now for the editing part
	// ------------------------

	public void setSize(int width, int height) {
		edit(() -> setSizeUnsafe(width, height));
	}

	private boolean setSizeUnsafe(int width, int height) {
		// this might not be run from UI-thread
		assert bitmap != null;

		try {
			Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			drawer.updateBitmap(newBitmap);

			// so far, everything worked, hence update fields
			this.bitmap = newBitmap;
			this.width = width;
			this.height = height;

			Commons.uiRun(new Runnable() {
				@Override
				public void run() {
					listeners.forEach((l) -> l.newBitmapCreated(bitmap, BitmapFragment.this));
				}
			});

			return true;
		} catch(OutOfMemoryError | NullPointerException e) {
            // FIXME!!! Potential crash!
            // New in Java 7. Weird syntax...
			Log.d(getClass().getName(), "Out of memory");
			Commons.uiRun(() ->
                    DialogHelper.error(getActivity(), "Image too large...")
			);
			return false;
		}
	}

	public void setScale(Scale sc) {
		edit(() -> setScaleUnsafe(sc));
	}

	public void setScaleRelative(Scale sc) {
		edit(() -> setScaleUnsafe(fractal.scale().relative(sc)));
	}

	private void setScaleUnsafe(Scale sc) {
		fractal = fractal.copyNewScale(sc);
		drawer.setScale(sc); // not necessary to update the whole fractal.
		history.addToHistory(fractal);
	}

	public void setFractal(Fractal f) {
		edit(() -> setFractalUnsafe(f));
	}

	private void setFractalUnsafe(Fractal f) {
		// f must have been parsed and compiled.
		this.fractal = f;
		drawer.setFractal(this.fractal);
		history.addToHistory(this.fractal);
	}

	public boolean historyIsEmpty() {
		// history always contains the current element.
		return history.isEmpty();
	}

	public void historyBack() {
		Fractal f = history.removeLast();
		setFractal(f);
	}

	private LinkedList<Runnable> editors = new LinkedList<>();

	private void edit(final Runnable editor) {
		// always in UI thread
		if(isRunning) {
			Log.d(getClass().getName(), "isRunning is true, hence adding editor to list.");
			editors.add(editor);
			drawer.requestEdit();
		} else {
			Log.d(getClass().getName(), "isRunning is false, hence calling editor");
			editor.run();
			startBackgroundTask();
		}
    }


	/**
	 * This method starts the drawing thread in the background.
	 */
    private void startBackgroundTask() {
        // this is always started from UI-thread
        // start calculation in background
        Log.d(getClass().getName(), "start drawing");
		isRunning = true;

		listeners.forEach((l) -> l.drawerStarted(this));

		new Thread(drawer).start(); // fixme is that the android way?
    }

	// =============================================================
	// ====== Some get functions ===================================
	// =============================================================

	public Fractal fractal() {
		return fractal;
	}

	/**
	 * @return bitmap of this bitmap fragment. Might be null if no bitmap has been generated yet.
	 */
    public Bitmap getBitmap() {
        return bitmap;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }



	/**
	 * BitmapFragment does some callbacks to the activity that
	 * created this BitmapFragment. It uses the following interface
	 * for this purpose.
	 */
	public interface BitmapFragmentListener {
        void initializationFinished();

		/**
		 * The view and progress bars should be updated because
		 * the bitmap changed
		 */
		void bitmapUpdated(BitmapFragment src);

		/**
		 * The view should be updated and the view matrices reset
		 * because a first preview was generated in the bitmap.
         * Maybe merge this one with bitmapUpdated?
		 */
		void previewGenerated(BitmapFragment src);

		/**
		 * We will now start a new calc. This one is called from the UI-thread.
		 */
		void drawerStarted(BitmapFragment src);

		/**
		 * Called when the calculation is finished (and it was not cancelled)
		 * @param ms milliseconds
		 */
        void drawerFinished(long ms, BitmapFragment src);

		/**
		 * Called after a new bitmap was created.
		 * @param bitmap The new bitmap
		 */
		void newBitmapCreated(Bitmap bitmap, BitmapFragment src);
	}
}
