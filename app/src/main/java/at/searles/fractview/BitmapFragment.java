package at.searles.fractview;

import android.app.Activity;
import android.app.Fragment;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import at.searles.fractview.editors.ProgressDialogFragment;
import at.searles.fractview.fractal.Fractal;
import at.searles.fractview.fractal.FractalDrawer;
import at.searles.fractview.fractal.RenderScriptDrawer;
import at.searles.math.Scale;
import at.searles.meelan.CompileException;

/**
 * This class is the glue maintaining all parameters for drawing the fractal
 * and managing the threads including their interruption.
 *
 * History should not be part of this class!
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
	private FractalDrawer drawer = null;
    private ExecutorService executorService;
    private Future<?> currentFuture;

    // Locks + variable to cancel a running calculation.
	private ReentrantLock editLock = new ReentrantLock();

	// the next two variables are only changed in the UI thread.

	// the next one counts the cancel requests.
	private volatile int cancelled = 0;

	// if running.
	private boolean isRunning;

	/**
	 * Matrices to convert coordinates into value that is
	 * independent from the bitmap-size. Normized always
	 * contains the square -1,-1 - 1-1 with 0,0 in the middle
	 * but also keeps the ratio of the image.
	 */
	public final Matrix bitmap2norm = new Matrix();

	/**
	 * Inverse of bitmap2norm
	 */
	public final Matrix norm2bitmap = new Matrix();


	LinkedList<Fractal> history = new LinkedList<>();

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
			this.drawer = new RenderScriptDrawer(new FractalDrawer.Controller() {
				@Override
				public void previewGenerated() {
					// fixme can getActivity return null?
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// if getActivity is null, this one will not work anyways
							((UpdateListener) getActivity()).previewGenerated(BitmapFragment.this);
						}
					});
				}

				@Override
				public void bitmapUpdated() {
					// fixme can getActivity return null?
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// if getActivity is null, this one will not work anyways
							((UpdateListener) getActivity()).bitmapUpdated(BitmapFragment.this);
						}
					});
				}

				@Override
				public void finished(final long ms) {
					Log.d("BMF", "finished was called");
					isRunning = false;
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Log.d("BMF", "Calling dismiss of waiting dialog");
							// check whether there is some pending action
							ProgressDialogFragment waitingDialog =
									(ProgressDialogFragment) getFragmentManager().findFragmentByTag("waitingDialog");

							if(waitingDialog != null) {
								// this check is due to a possible race condition
								// yes, there is some action missing. Dismiss dialog and do it.
								waitingDialog.dismissAllowingStateLoss();
							}

							if(waiting != null) {
								ProgressDialogValues tmp = waiting;
								waiting = null;
								doImageAction(tmp);
							}

							// and also inform the activity
							((UpdateListener) getActivity()).calculationFinished(ms, BitmapFragment.this);
						}
					});
				}

				@Override
				public boolean isCancelled() {
					return cancelled > 0;
				}

				@Override
				public Activity getActivity() {
					return BitmapFragment.this.getActivity();
				}
			});
		}

		if(this.bitmap != null) {
			((UpdateListener) getActivity()).newBitmapCreated(bitmap, this); // in case of rotation, this fragment is preserved
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

		updateMatrices();

		// create bitmap
		this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		((UpdateListener) getActivity()).newBitmapCreated(bitmap, this); // tell others about it.

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

		// create executor service
		executorService = Executors.newCachedThreadPool();

		// finally, we initialize the drawer.
		// this might take some time after a fresh install, hence do it in the background
		new AsyncTask<Void, Void, Void>() {
			boolean finished = false;

			@Override
			protected Void doInBackground(Void...ignore) {
				drawer.init(bitmap, fractal);
				return null;
			}

			@Override
			protected void onPreExecute() {
				Handler handler = new Handler(); // FIXME create a 'delayed dialog'-class.
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						// start in UI-thread (unless we are already done)
						if(!finished) {
							Log.d("BMF", "Creating dialog");
							ProgressDialogFragment.newInstance(ProgressDialogValues.Init.ordinal(),
									true,
									"Please wait...",
									"Initializing program (this may take a few seconds "
											+ "after the program cache was cleaned because"
											+ " the GPU scripts are compiled by Android)", false)
									.show(getFragmentManager(), "initRSDialog");
						}
					}
                }, 1250); // show dialog after 1250ms
			}

			@Override
			protected void onPostExecute(Void ignore) {
				finished = true;

				ProgressDialogFragment ft = (ProgressDialogFragment) getFragmentManager().findFragmentByTag("initRSDialog");

				if(ft != null) {
					Log.d("BF", "Trying to close dialog");
					ft.dismissAllowingStateLoss();
				}

				Log.d("BMF", "Init Fractal");
				drawer.setFractal(fractal);

				Log.d("BMF", "Starting thread");
				startBackgroundTask();
			}
		}.execute();
	}

	/**
	 * Must be called when the size is modified.
	 */
	protected void updateMatrices() {
		float m = Math.min(width, height);

		bitmap2norm.setValues(new float[]{
				2f / m, 0f, -width / m,
				0f, 2f / m, -height / m,
				0f, 0f, 1f
		});

		//bitmap2norm.reset();
		//bitmap2norm.setScale(2f / m, 2f / m);
		//bitmap2norm.setTranslate(width / m, height / m);

		//norm2bitmap.reset();
		//norm2bitmap.setScale(m / 2f, m / 2f);
		//norm2bitmap.setTranslate(width / 2f, height / 2f);

		norm2bitmap.setValues(new float[]{
				m / 2f, 0f, width / 2f,
				0f, m / 2f, height / 2f,
				0f, 0f, 1f
		});
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

			updateMatrices();

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					((UpdateListener) getActivity()).newBitmapCreated(bitmap, BitmapFragment.this);
				}
			});

			if(setAsDefaultIfSuccess) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						((MainActivity) getActivity()).storeDefaultSize(width, height);
					}
				});
			}

			return true;
		} catch(OutOfMemoryError e) {
			// fixme not super-nice, but it works
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getActivity(), "ERROR: Out of memory", Toast.LENGTH_LONG).show();
				}
			});
			return false;
		} catch(NullPointerException e) {
			// fixme not super-nice and ugly DRY
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getActivity(), "ERROR: Out of memory", Toast.LENGTH_LONG).show();
				}
			});
			return false;
		}
	}

	public void setScale(Scale sc) {
		history.addLast(this.fractal);
		fractal = fractal.copyNewScale(sc);
		drawer.setScale(sc); // not necessary to update the whole fractal.
	}

	public void setFractal(Fractal f) {
		// f must be parsed and compiled.
		history.addLast(this.fractal);
		this.fractal = f;
		drawer.setFractal(this.fractal);
	}

	public boolean historyIsEmpty() {
		// history always contains the current element.
		return history.isEmpty();
	}

	public void historyBack() {
		this.fractal = history.removeLast();
		drawer.setFractal(this.fractal);
	}

	public void edit(final Runnable editor) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                // in UI-thread
                cancelled++;
            }

            @Override
            protected Void doInBackground(Void... voids) {
				// only one edit at a time. Other edits have to wait here!

				// There are two types of edits:
				// Change parameters/program
				// Change image
				// The last one can only be done when the thread has stopped. The other one may be performed also
				// before. That is why we have a boolean here and actually two mutexes.
                editLock.lock();

                try {
					Log.d("BMF", "Locking for edit");
					long duration = System.currentTimeMillis();

					if(currentFuture != null) {
						currentFuture.get(); // wait for termination
					} else {
						// If the bitmap fragment is recreated after being terminated,
						// currentFuture is null.
						Log.d("BMF", "Just telling, it seems the app was terminated while in another activity");
					}

					duration = System.currentTimeMillis() - duration;
					Log.d("BMF", "Cancelled: " + duration);

					editor.run();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                } finally {
                    editLock.unlock();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                // in UI-thread
				cancelled --;

				// if cancelled != null then there will be another editor.
                if (cancelled == 0) {
                    startBackgroundTask();
                } else {
                    Log.d("BMF", "did not start because other edits are pending");
					// fixme if editing is because of lastscale but no new calculation is started
					// I have to call combineLastScales
                }
            }
        }.execute();
    }


	/**
	 * This method starts the drawing thread in the background.
	 */
    private void startBackgroundTask() {
        // this is always started from UI-thread
        // start calculation in background
        Log.d("BMF", "Start Drawing");
		isRunning = true;

		// FIXME ((UpdateListener) getActivity()) can be null in here...

		((UpdateListener) getActivity()).calculationStarting(this);
        currentFuture = executorService.submit(drawer);
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

	/*@Override
	public void imageSaved(File file, int id) {
		// FIXME
		// FIXME
		if(id == SAVE_IMAGE_EXTERN) {
			// do nothing
		} else if(id == SHARE_IMAGE) {
			Uri contentUri = Uri.fromFile(file);
			Intent share = new Intent(Intent.ACTION_SEND);
			share.setType("image/png");
			share.putExtra(Intent.EXTRA_STREAM, contentUri);
			startActivity(Intent.createChooser(share, "Share Image"));
		} else if(id == SET_IMAGE_AS_WALLPAPER) {

		}
	}*/

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
	public interface UpdateListener {
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