package at.searles.fractview;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import at.searles.fractview.fractal.DemoFractalDrawer;
import at.searles.fractview.fractal.Fractal;
import at.searles.fractview.fractal.FractalDrawer;
import at.searles.fractview.renderscript.RenderScriptDrawer;
import at.searles.math.Scale;
import at.searles.meelan.CompileException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the glue maintaining all parameters for drawing the fractal
 * and managing the threads including their interruption.
 *
 * History should not be part of this class!
 *
 * FIXME: How does edit work and do I need an action string.
 *
 * Possible Edit-Actions:
 *     * There is a new fractal (Bookmark/ParameterEdit/Source/History)
 *     ==> call setFractal
 *
 *     * There is a new scale (Display action)
 *     ==> call setScale
 *
 *     * There is a new image size
 *     ==> call setSize
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

    // Allocations and various data
    //int height, width;
	Fractal fractal;

	FractalDrawer drawer = null;
    ExecutorService executorService;
    Future<?> currentFuture;

    // Locks + variable to cancel a running calculation.
	ReentrantLock editLock = new ReentrantLock();
    volatile int cancelled = 0;

	boolean isRunning;

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

	Bitmap bitmap;
	int width;
	int height;

	LinkedList<Fractal> history = new LinkedList<>();

	// call back if something changed/has to be drawn etc...
	UpdateListener listener() {
		return (UpdateListener) getActivity();
	}

	// We must sometimes run lengthy tasks. For this purpose
	// use the following fields. pd must be dispatched and recreated!
	class ProgressDialogData {
		String title = null;
		String msg = null;
		boolean allowSkip = false;

		Runnable postAction = null;

		ProgressDialog pd = null; // if we are busy.

		ProgressDialogData(String title, String msg, boolean allowSkip, Runnable postAction) {
			this.title = title;
			this.msg = msg;
			this.allowSkip = allowSkip;
			this.postAction = postAction;
		}

		void showPD(Activity activity) {
			if(pd != null) throw new IllegalArgumentException();

			pd = new ProgressDialog(getActivity());
			pd.setTitle(title);
			pd.setMessage(msg);
			pd.setCancelable(false);

			if(allowSkip) {
				pd.setButton(DialogInterface.BUTTON_NEGATIVE, "Skip", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dismissAndClear();
					}
				});
			}

			pd.show();
		}

		void dismissPD() {
			if(pd != null) {
				pd.dismiss();
				pd = null;
			}
		}

		void dismissAndClear() {
			dismissPD(); // first, dismiss dialog (since we might create another one in postAction)
			pdData = null; // clear pdData (because we might create another dialog)

			// only now run it.
			if(postAction != null) postAction.run();
		}
	}

	ProgressDialogData pdData = null; // if null, then there is no dialog to be shown.

	// if not null this will be executed whent the calculation has finished (in UI-thread!)
	Runnable invokeWhenFinished = null;

	@Override
	public void onAttach(Activity context) {
		// Deprecated but Android SDK is buggy here!
		Log.d("BMF", "onAttach");
		super.onAttach(context);

		if(pdData != null) {
			pdData.showPD(context);
		}

		// we are in the UI-thread and if this was the first start,
		// onCreate was not yet called. Good point to initialize
		// the RS-Object because it needs the activity and we are
		// in the UI-thread, so no race-conditions.
		if(this.drawer == null) {

			// FIXME
			// FIXME
			// FIXME
			// FIXME
			// FIXME
			// FIXME
			// FIXME
			// FIXME
			// FIXME
			// FIXME
			// FIXME Replace by renderscript drawer

			this.drawer = new RenderScriptDrawer(new FractalDrawer.Controller() {
				@Override
				public void previewGenerated() {
					// fixme can getActivity return null?
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// if getActivity is null, this one will not work anyways
							listener().previewGenerated();
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
							listener().bitmapUpdated();
						}
					});
				}

				@Override
				public void finished(final long ms) {
					isRunning = false;
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// the next line is due to the "wait until finished"-feature
							if(invokeWhenFinished != null) invokeWhenFinished.run();
							if(listener() != null) listener().calculationFinished(ms);
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
			listener().newBitmapCreated(bitmap); // in case of rotation, this fragment is preserved
		}
	}

	@Override
	public void onDetach() {
		Log.d("BMF", "onDetach");
		super.onDetach();
		if(pdData != null) pdData.dismissPD();
		// but we keep the pdData.
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d("BMF", "onCreate");

		super.onCreate(savedInstanceState);
		setRetainInstance(true); // preserve this one on rotation

		// Read initial width/height
		this.width = getArguments().getInt("width");
		this.height = getArguments().getInt("height");

		updateMatrices();

		// create bitmap
		this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		listener().newBitmapCreated(bitmap); // tell others about it.

		// and also the fractal.
		this.fractal = getArguments().getParcelable("fractal");

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
			boolean initializationFinished = false;

			@Override
			protected Void doInBackground(Void...ignore) {
				drawer.init(bitmap, fractal);
				return null;
			}

			@Override
			protected void onPreExecute() {
				Handler handler = new Handler(); // FIXME create a 'delayed dialog'-class.
				handler.postDelayed(() -> {
                    // start in UI-thread (unless we are already done)
                    if(!initializationFinished) {
                        pdData = new ProgressDialogData(
                                "Please wait...",
                                "Initializing program (this may take a few seconds " +
                                "after a fresh install because" +
                                " the GPU scripts are compiled by Android)", false, null);

                        pdData.showPD(getActivity());
                    }
                }, 1250); // show dialog after 1250ms
			}

			@Override
			protected void onPostExecute(Void ignore) {
				initializationFinished = true;

				Log.d("BMF", "Init Fractal");
				drawer.setFractal(fractal);

				Log.d("BMF", "Starting thread");
				startBackgroundTask();

				if(pdData != null) {
					pdData.dismissPD();
					pdData = null;
				}
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

	public boolean setSize(int width, int height) {
		// this might not be run from UI-thread
		assert bitmap != null;

		try {
			Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			drawer.updateBitmap(newBitmap);

			// so far, everythimg worked, hence update fields
			this.bitmap = newBitmap;
			this.width = width;
			this.height = height;

			updateMatrices();

			getActivity().runOnUiThread(() -> listener().newBitmapCreated(bitmap));

			return true;
		} catch(OutOfMemoryError e) {
			// fixme not super-nice, but it works
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


    void startBackgroundTask() {
        // this is always started from UI-thread
        // start calculation in background
        Log.d("BMF", "Start Drawing");
		isRunning = true;

		// FIXME listener() can be null in here...

		listener().calculationStarting();
        currentFuture = executorService.submit(drawer);
    }

    /*public Fractal fractalBuilder() {
        return fractalBuilder;
    }*/

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

	public void saveImageInBackground(final File imageFile, final boolean share, final boolean setAsWallpaper) {
		// We are in the UI-Thread.

		// Create task that will save the image
		final AsyncTask<Void, Void, Boolean> saveTask = new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected void onPreExecute() {
				pdData = new ProgressDialogData("Saving image",
						"Saving image to " + imageFile.getAbsolutePath(),
						false, null);
				pdData.showPD(getActivity());
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				String errorMsg;
				try {
					FileOutputStream fos = new FileOutputStream(imageFile);

					if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
						// Successfully written picture
						fos.close();

						// Show toast
						getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Image saved as " + imageFile.getName(), Toast.LENGTH_SHORT).show());

						return true;
					} else {
						errorMsg = "Error calling \"compress\". This is a bug.";
						fos.close();
					}
				} catch(IOException e) {
					errorMsg = e.getLocalizedMessage();
				}

				final String s = errorMsg; // seriously, please Java 8, come to Android...

				getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Error: " + s,
                        Toast.LENGTH_LONG).show());

				return false;
			}

			@Override
			protected void onPostExecute(Boolean saved) {
				// it is hard to get into a race condition but it is possible.
				pdData.dismissAndClear();

				if(saved) {
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
						// this is only true if allowSettingWallpaper is true.
						WallpaperManager wallpaperManager = WallpaperManager.getInstance(getActivity());
						try {
							wallpaperManager.setBitmap(getBitmap());
						} catch(IOException e) {
							Toast.makeText(getActivity(), e.getLocalizedMessage(),
									Toast.LENGTH_LONG).show();
							// could not set it as wallpaper, but since it was saved,
							// not such a big deal.
						}
					}
				}
			}
		};

		if(isRunning) {
			// show skipable dialog
			pdData = new ProgressDialogData("Waiting until calculation is finished",
					"Skip to save immediately",
					true,
					() -> {
                        invokeWhenFinished = null; // first, clear invokeWhenFinished
                        saveTask.execute(); // then run this one.
                    }
			);

			pdData.showPD(getActivity());

			// a bit of aspect oriented programming would be nice here:
			invokeWhenFinished = () -> {
                // this is in the UI-thread.
                pdData.dismissAndClear();
            };

		} else {
			saveTask.execute();
		}
	}

    /*public void debugBitmap() {
        // fixme delete this method.
        Bitmap bm = drawer.bitmap();
		int count = 0;
		for(int y = 0; y < bm.getHeight(); ++y) {
			for(int x = 0; x < bm.getHeight(); ++x) {
				if(bm.getPixel(x, y) != 0) {
					if(count++ > 10) break;
					Log.d("BF", "pixel color at " + x + ", " + y + " is " + Integer.toHexString(bm.getPixel(x, y)));
				}
			}
		}
		Log.d("BF", "debugBitmap end.");
    }*/

    public interface UpdateListener {
		/**
		 * The view should be updated because the bitmap changed
		 */
		void bitmapUpdated();

		/**
		 * The view should be updated because the bitmap changed
		 */
		void previewGenerated();

		/**
		 * We will now start a new calc. This one is called from the UI-thread.
		 */
		void calculationStarting();

		/**
		 * Called when the calculation is finished (and it was not cancelled)
		 * @param ms milliseconds
		 */
        void calculationFinished(long ms);

		/**
		 * Called after a new bitmap was created.
		 * @param bitmap The new bitmap
		 */
		void newBitmapCreated(Bitmap bitmap);
	}
}