package at.searles.fractview;

import android.app.Activity;
import android.app.Fragment;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import at.searles.fractview.editors.ProgressDialogFragment;
import at.searles.fractview.fractal.Drawer;
import at.searles.fractview.fractal.Fractal;
import at.searles.fractview.fractal.RenderScriptDrawer;
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
public class BitmapFragment extends Fragment implements
						ProgressDialogFragment.Callback {

	public static BitmapFragment newInstance(int width, int height, Fractal fractal) {
		// set initial size
		Bundle bundle = new Bundle();

		bundle.putInt("width", width);
		bundle.putInt("height", height);

		bundle.putParcelable("fractal", fractal);

		BitmapFragment ft = new BitmapFragment();
		ft.setArguments(bundle);

		return ft;
	}

	/**
	 * There are three types of progressdialogs in this one.
	 */
	private enum ProgressDialogValues { Init, RunningSave, RunningShare, RunningWallpaper, Save }

    // Allocations and various data
    private int height, width;
	private Fractal fractal;
	private Bitmap bitmap;

	// Now the drawing stuff
	private Drawer drawer;

	// the next two variables are only changed in the UI thread.

	// if true, the drawing has not started yet.
	private boolean isInitializing = true;

	// if true, the drawing is currently running and the drawer should
	// not be modified.
	private boolean isRunning;

    public boolean isInitializing() {
        return isInitializing;
    }

    private List<BitmapFragmentListener> listeners = new LinkedList<>();

    public void addBitmapFragmentListener(BitmapFragmentListener listener) {
        listeners.add(listener);
    }

    public void removeBitmapFragmentListener(BitmapFragmentListener listener) {
        if(!listeners.remove(listener)) {
            Log.e(getClass().getName(), "Trying to remove an inexistent listener!");
        }
    }

	/**
	 * This variable is only to be modified from the UI-thread
	 * to avoid a race condition.
	 */
	private boolean addToHistory = true; // add the current to history.

	/**
	 * List of past fractals that were drawn using this bitmap fragment.
	 */
	private History history = new History();

	@Override
	public void onAttach(Activity context) {
		// Deprecated but Android SDK is buggy here!
		Log.d("BMF", "onAttach");
		super.onAttach(context);

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
								editors.forEach(Runnable::run);
								editors.clear();
								drawer.clearRequestEdit();
								startBackgroundTask();
							} else {
								isRunning = false;
								listeners.forEach((listener) ->
										listener.calculationFinished(-1, BitmapFragment.this)
								);
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

		listeners.forEach((l) -> l.newBitmapCreated(bitmap, this)); // tell others about it.

		// and also the fractal.
		this.fractal = getArguments().getParcelable("fractal");

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

		// finally, we initialize the drawer.
		// this might take some time after a fresh install, hence do it in the background
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
                drawer.setFractal(fractal);

                isInitializing = false; // done
                for(BitmapFragmentListener listener : listeners) {
                    listener.initializationFinished();
                }

				startBackgroundTask();
			}
		}.execute();
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

	public void setSize(int width, int height, boolean setAsDefaultIfSuccess) {
		// do not add new fractal to history
		edit(new Runnable() {
			@Override
			public void run() {
				setSizeUnsafe(width, height, setAsDefaultIfSuccess);
			}
		});
	}

	private boolean setSizeUnsafe(int width, int height, boolean setAsDefaultIfSuccess) {
		// this might not be run from UI-thread
		assert bitmap != null;

		try {
			Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			drawer.updateBitmap(newBitmap);

			// so far, everything worked, hence update fields
			this.bitmap = newBitmap;
			this.width = width;
			this.height = height;

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					listeners.forEach((l) -> l.newBitmapCreated(bitmap, BitmapFragment.this));
				}
			});

			if(setAsDefaultIfSuccess) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// fixme This one should not be here!
						((MainActivity) getActivity()).storeDefaultSize(width, height);
					}
				});
			}

			return true;
		} catch(OutOfMemoryError e) {
			Log.d(getClass().getName(), "Out of memory");
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getActivity(), "Image too large...", Toast.LENGTH_LONG).show();
				}
			});
			return false;
		} catch(NullPointerException e) {
			// FIXME Put this into some lambda to avoid DRY.
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getActivity(), "Image too large...", Toast.LENGTH_LONG).show();
				}
			});
			return false;
		}
	}

	public void setScale(Scale sc) {
		addToHistory = true;
		edit(() -> setScaleUnsafe(sc));
	}

	public void setScaleRelative(Scale sc) {
		addToHistory = true;
		edit(() -> setScaleUnsafe(fractal.scale().relative(sc)));
	}

	private void setScaleUnsafe(Scale sc) {
		fractal = fractal.copyNewScale(sc);
		drawer.setScale(sc); // not necessary to update the whole fractal.
	}

	public void setFractal(Fractal f) {
		addToHistory = true;
		edit(() -> setFractalUnsafe(f));
	}

	private void setFractalUnsafe(Fractal f) {
		// f must have been parsed and compiled.
		this.fractal = f;
		drawer.setFractal(this.fractal);
	}

	public boolean historyIsEmpty() {
		// history always contains the current element.
		return history.isEmpty();
	}

	public void historyBack() {
		addToHistory = false;
		edit(this::historyBackUnsafe);
	}

	private void historyBackUnsafe() {
		if(!historyIsEmpty()) {
			// this was most likely checked before, but I want
			// to avoid a race condition.
			this.fractal = history.pop();
			drawer.setFractal(this.fractal);
		}
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

		listeners.forEach((l) -> l.calculationStarting(this));

		if(addToHistory) {
			this.history.push(this.fractal);
			addToHistory = false;
		}

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

	// ==================================================================
	// ================= Save/Share/Set as Wallpaper ====================
	// ==================================================================

	public void shareImage() {
		waitForImageRendering(ProgressDialogValues.RunningShare);
	}

	public void setAsWallpaper() {
		waitForImageRendering(ProgressDialogValues.RunningWallpaper);
	}

	public void saveImage(File imageFile) {
		saveFile = imageFile; // keep file
		waitForImageRendering(ProgressDialogValues.RunningSave);
	}

	private static final int SAVE_IMAGE = 1;
	private static final int SHARE_IMAGE = 2;
	private static final int SET_WALLPAPER = 3;

	@Override
	public void onSkip(int requestCode) {
		waiting = null;
		doImageAction(ProgressDialogValues.values()[requestCode]);
	}

	@Override
	public void onCancel(int requestCode) {
		waiting = null;
	}

	private void doImageAction(ProgressDialogValues action) {
		// did someone skip this one?
		switch(action) {
			case RunningSave: {
				// dialog was already dismissed
				doImageAction(SAVE_IMAGE);
			} break;
			case RunningShare: {
				doImageAction(SHARE_IMAGE);
			} break;
			case RunningWallpaper: {
				doImageAction(SET_WALLPAPER);
			} break;
			default:
				throw new IllegalArgumentException("there should not be a skippable dialog for " + action);
		}
	}

	/**
	 * do whatever is requested immediately.
	 * @param actionId Id of the requested action.
     */
	private void doImageAction(int actionId) {
		switch(actionId) {
			case SAVE_IMAGE: {
				saveImageInBackground(saveFile, actionId);
			} break;
			case SHARE_IMAGE: {
				// create TMP file and call share indent
				try {
					File imageFile = File.createTempFile("fractview", ".png", getActivity().getExternalCacheDir());
					saveImageInBackground(imageFile, actionId);
				} catch (IOException e) {
					Toast.makeText(getActivity(), "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
				}

			} break;
			case SET_WALLPAPER: {
				saveImageInBackground(null, actionId);
			} break;

			// in all other cases, do nothing.
		}
	}

	/**
	 * Variable indicates that there is someone waiting...
	 */
	private ProgressDialogValues waiting = null; // null = no waiting dialog.

	private File saveFile;

	private void waitForImageRendering(ProgressDialogValues postAction) {
		// if bitmap fragment is not yet done
		if(isRunning()) {
			// show dialog to wait
			ProgressDialogFragment waitingDialog = ProgressDialogFragment.newInstance(postAction.ordinal(),
					true,
					"Image rendering not yet finished...",
					"Skip to use the incompletely rendered image", true);
			waitingDialog.setTargetFragment(this, -1);
			waitingDialog.show(getFragmentManager(), "waitingDialog");
			waiting = postAction;
		} else {
			// if not running, then save immediately
			doImageAction(postAction);
		}
	}

	/**
	 * Saves the image in the background. As a special tweak, it displays a dialog to
	 * save the file only once the calculation is done. This dialog allows skip or cancel. Skip
	 * saves the image instantly, Cancel cancels the whole thing.
	 * @param imageFile File object in which the image should be saved.
	 */
	private void saveImageInBackground(final File imageFile, int action) {
		// We are in the UI-Thread.
		// Check whether there is some active Activity to store a savingsdialog
		Log.d("BF", "isFinishing in activity: " + getActivity().isFinishing());

		ProgressDialogFragment ft = ProgressDialogFragment.newInstance(ProgressDialogValues.Save.ordinal(),
				true,
				"Please wait...",
				"Saving image...", false);

		try {
			// TODO if this is called when the activity is not showing,
			// it causes a crash.
			ft.show(getFragmentManager(), "saveDialog");
		} catch(IllegalStateException e) {
			e.printStackTrace();
		}
		// no need for target because it is not skippable
		// ft.setTargetFragment(BitmapFragment.this, -1);

		// Create task that will save the image
		new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected void onPreExecute() {}

			@Override
			protected Boolean doInBackground(Void... params) {
				String errorMsg;
				try {
					// either put it into the file or set it as wallpaper
					if(action == SET_WALLPAPER) {
						// image file is ignored here.
						if(imageFile != null) {
							throw new IllegalArgumentException("image file should be null here!");
						}

						// set bitmap
						WallpaperManager wallpaperManager =
								WallpaperManager.getInstance(getActivity());

						wallpaperManager.setBitmap(getBitmap());
						return true;
					} else {
						FileOutputStream fos = new FileOutputStream(imageFile);

						if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
							// Successfully written picture
							fos.close();

							return true;
						} else {
							errorMsg = "Error calling \"compress\".";
							fos.close();
						}
					}
				} catch(IOException e) {
					errorMsg = e.getLocalizedMessage();
				}

				// There was some error

				String finalErrorMsg = errorMsg;
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getActivity(), "ERROR: " + finalErrorMsg,
								Toast.LENGTH_LONG).show();
					}
				});

				return false;
			}

			@Override
			protected void onPostExecute(Boolean saved) {
				ProgressDialogFragment ft = (ProgressDialogFragment) getFragmentManager().findFragmentByTag("saveDialog");

				if(ft != null) ft.dismissAllowingStateLoss();

				if(saved) {
					if(action == SAVE_IMAGE) {
						// this is executed after saving was successful
						// Add it to the gallery
						Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
						Uri contentUri = Uri.fromFile(saveFile);
						mediaScanIntent.setData(contentUri);
						getActivity().sendBroadcast(mediaScanIntent);

						saveFile = null; // just to be save...
					} else if(action == SHARE_IMAGE) {
						// Share image
						Uri contentUri = Uri.fromFile(imageFile);
						// after it was successfully saved, share it.
						Intent share = new Intent(Intent.ACTION_SEND);
						share.setType("image/png");
						share.putExtra(Intent.EXTRA_STREAM, contentUri);
						startActivity(Intent.createChooser(share, "Share Image"));
					}
				}
			}
		}.execute();
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
		 */
		void previewGenerated(BitmapFragment src);

		/**
		 * We will now start a new calc. This one is called from the UI-thread.
		 */
		void calculationStarting(BitmapFragment src);

		/**
		 * Called when the calculation is finished (and it was not cancelled)
		 * @param ms milliseconds
		 */
        void calculationFinished(long ms, BitmapFragment src);


		// FIXME the next one seems to be a bit odd compared to the other ones.
		/**
		 * Called after a new bitmap was created.
		 * @param bitmap The new bitmap
		 */
		void newBitmapCreated(Bitmap bitmap, BitmapFragment src);
	}
}