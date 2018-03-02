package at.searles.fractview.bitmap;

import android.app.Activity;
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
import at.searles.fractal.DrawerListener;
import at.searles.fractal.Fractal;
import at.searles.fractal.android.BundleAdapter;
import at.searles.fractview.MainActivity;
import at.searles.fractview.SourcesListActivity;
import at.searles.fractview.renderscript.RenderScriptFragment;
import at.searles.fractview.renderscript.RenderScriptListener;
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
public class BitmapFragment extends Fragment implements DrawerListener, RenderScriptListener {


	private static final String WIDTH_LABEL = "width";
	private static final String HEIGHT_LABEL = "height";

	private static final String SCREEN_WIDTH_LABEL = "screenWidth";
	private static final String SCREEN_HEIGHT_LABEL = "screenHeight";

	// conservative defaults if there are no valid values available...
	private static final int DEFAULT_WIDTH = 1024;
	private static final int DEFAULT_HEIGHT = 600;

	public static BitmapFragment newInstance(int width, int height,
											 int screenWidth, int screenHeight, Fractal fractal) {
		// set initial size
		Bundle bundle = new Bundle();

		bundle.putInt(WIDTH_LABEL, width);
		bundle.putInt(HEIGHT_LABEL, height);

		bundle.putInt(SCREEN_WIDTH_LABEL, screenWidth);
		bundle.putInt(SCREEN_HEIGHT_LABEL, screenHeight);

		Bundle fractalBundle = BundleAdapter.fractalToBundle(fractal);
		bundle.putBundle(SourcesListActivity.FRACTAL_INDENT_LABEL, fractalBundle);

		BitmapFragment ft = new BitmapFragment();
		ft.setArguments(bundle);

		return ft;
	}

	// if true, the drawing is currently running and the drawer should
	// not be modified. Only modified in UI thread
	private enum Status {
		INITIALIZING, // in invalid state
		RUNNING,      // drawer is running
		IDLE,         // waiting for IdleJobs
		PROCESSING    // in working loop.
	}

	// Allocations and various data
	private int height, width;

	private Fractal fractal;
	private Bitmap bitmap;

	/**
	 * If a new bitmap is created, it is first stored here.
	 */
	private Bitmap newBitmap;

	/**
	 * The drawer does the actual drawing operation
	 */
	private Drawer drawer;

	/**
	 * Set by IdleJob. If true, then the drawer will restart after
	 * finishing the job queue.
	 */
	private boolean triggerStart = false;

	/**
	 * The state of the BitmapFragment
	 */
	private Status status;

	/**
	 * The job-dequeue.
	 */
	private LinkedList<IdleJob> jobQueue;

	/**
	 * Listeners
	 */
	private List<BitmapFragmentListener> listeners = new LinkedList<>();

    public void addBitmapFragmentListener(BitmapFragmentListener listener) {
		Log.d(getClass().getName(), "adding listener " + listener);
        listeners.add(listener);
    }

    public void removeBitmapFragmentListener(BitmapFragmentListener listener) {
		Log.d(getClass().getName(), "removing listener " + listener);

        if(!listeners.remove(listener)) {
            Log.e(getClass().getName(), "Trying to remove a non-existing listener: " + listener);
        }
    }

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
			initDrawer(context);
		}

		if(this.bitmap != null) {
            for(BitmapFragmentListener listener : listeners) {
                listener.newBitmapCreated(bitmap, this); // in case of rotation, this fragment is preserved
            }
		}
	}

	// ======= Part 1: The job queue ======

	void scheduleIdleJob(IdleJob job, boolean highPriority) {
		// in ui thread
		if(highPriority) {
<<<<<<< HEAD:app/src/main/java/at/searles/fractview/bitmap/BitmapFragment.java
			jobQueue.addFirst(job);
		} else {
			jobQueue.addLast(job);
		}

		if(status == Status.IDLE) {
			executeJobQueue();
		} else if(status == Status.RUNNING && job.cancelRunning()) {
			drawer.cancel();
		}

		// if status == EDIT, this job will be executed in the current run
		//   since we are in the UI thread.
		// if status == INVALID, there will be a check for idle tasks.
	}

	calculationFinished(boolean interrupted) {
		// called when drawer is finished/successfully cancelled.
		// in UI-thread!
		manageIdleJobs();
	}

	void manageIdleJobs() {
		if(jobs.isEmpty()) {
			if(triggerStart) {
				startDrawing();
			} else {
				status = IDLE;
			}
		} else {
			status = PROCESSING;
			createJobTask().execute();
		}
	}

	void startDrawing() {
		status = RUNNING;
		drawer.execute();
	}

	AsyncTask createJobTask() {
		// this is insensitive if the job was already executed before.
		// in that case, this task will simply wait for its result.
		return new AsyncTask() {
			IdleJob job = null;

			onPreExecute() {
				// in ui thread
				IdleJob job = jobs.removeFirst();

				if(job.imageIsModified()) {
					triggerStart = true;
				}

				if(job.getStatus() == AsyncTask.Status.PENDING) {
					job.execute();
				}
			}

			doInBackground() {
				// wait for the idlejob to terminate.
				job.get();
			}

			onPostExecute() {
				job = null; // clean up.
				manageIdleJobs();
			}
		}
	}




	// ======= Part 2: ======

	private void initDrawer() {

		if(fragment == null || fragment.isInitializing()) {
			throw new IllegalArgumentException("Fragment must exist and have been initialized.");
		}

		this.drawer = fragment.createDrawer();

		this.drawer.setListener(this);
	}

	@Override
	public void drawingUpdated(boolean firstUpdate) {
		// not in the UI thread!
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
	public void drawingFinished() {
		// Not in the UI Thread.
		Log.d(getClass().getName(), "drawer is finished");
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				if(!editors.isEmpty()) {
					for(Runnable editor : editors) {
						// FIXME Editors should be async
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

	@Override
	public void onDetach() {
		Log.d(getClass().getName(), "onDetach");
		super.onDetach();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true); // preserve this one on rotation

		// first, set up logic
		RenderScriptFragment renderScriptFragment =
				(RenderScriptFragment) getFragmentManager().findFragmentByTag(MainActivity.RENDERSCRIPT_FRAGMENT_TAG);

		renderScriptFragment.addListener(this);


		// Read initial width/height
		int screenWidth = getArguments().getInt(SCREEN_WIDTH_LABEL);
		int screenHeight = getArguments().getInt(SCREEN_HEIGHT_LABEL);

		if(screenWidth < 0 || screenHeight < 0) {
			Log.e(getClass().getName(), "invalid screen size: " + screenWidth + "x" + screenHeight);
			screenWidth = DEFAULT_WIDTH;
			screenHeight = DEFAULT_HEIGHT;
		}

		this.width = getArguments().getInt(WIDTH_LABEL);
		this.height = getArguments().getInt(HEIGHT_LABEL);

		if(width < 0 || height < 0) {
			this.width = screenWidth;
			this.height = screenHeight;
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

		setFractal(fractal());

		if(!setSizeUnsafe(width, height)) {
			if(!setSizeUnsafe(screenWidth, screenHeight)) {

				width = DEFAULT_WIDTH;
				height = DEFAULT_HEIGHT;

				while(!setSizeUnsafe(width, height)) {
					// + 3 so that the result is at least 1.
					width = (width + 3) / 4;
					height = (height + 3) / 4;
				}
			}
		}

		Log.d(getClass().getName(), "init is done");

		drawer.setFractal(fractal);


		startBackgroundTask();
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

	/**
	 * Creates a new bitmap in newBitmap that is later used as new bitmap before
	 * the next calculation is started.
	 * @param width The new width
	 * @param height The new height
	 * @return true if creating the new bitmap was successful.
	 */
	public boolean prepareSetSize(int width, int height) {
		// FIXME assertInUIThread
		try {
			newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

			if(!drawer.prepareSetSize(newBitmap)) {
				// clean up, drawer failed
				newBitmap = null;
				return false;
			}

			edit(new Runnable() {
				@Override
				public void run() {
					applyNewSize();
				}
			});

			return true;
		} catch(OutOfMemoryError e) {
			e.printStackTrace();
			return false;
		}
	}

	public void applyNewSize() {
		// FIXME assertInUIThread();
		// FIXME assertNotRunning();

		if(newBitmap != null) {
			// there might very theoretically be multiple
			// calls before the calculation stops,
			// therefore check for null.
			this.bitmap = this.newBitmap;
			this.newBitmap = null;

			drawer.applyNewSize();

			fireNewBitmapCreated();
		}
	}

	@Deprecated
	public void setSize(int width, int height) {

		edit(new Runnable() {
			@Override
			public void run() {
				applyNewSize();
				setSizeUnsafe(width, height);
			}
		});
	}

	@Deprecated
	private boolean setSizeUnsafe(int width, int height) {
		// this might not be run from UI-thread
		try {
			Log.d(getClass().getName(), "Trying to set image size to " + width + "x" + height);

			Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

			Log.d(getClass().getName(), "Bitmap successfully created");

			drawer.updateBitmap(newBitmap);

			Log.d(getClass().getName(), "Bitmap updated in drawer");

			// so far, everything worked, hence update fields
			this.bitmap = newBitmap;
			this.width = width;
			this.height = height;

			Commons.uiRun(new Runnable() {
				@Override
				public void run() {
					for(BitmapFragmentListener listener : listeners) {
						listener.newBitmapCreated(bitmap, BitmapFragment.this);
					}
				}
			});

			return true;
		} catch(OutOfMemoryError | NullPointerException e) {
            // FIXME!!! Potential crash!
            // New in Java 7. Weird syntax...
			Log.d(getClass().getName(), "Out of memory");
			Commons.uiRun(new Runnable() {
				public void run() {
					DialogHelper.error(getActivity(), "Image too large...");
				}
			});
			return false;
		}
	}

	public void setScale(Scale sc) {
		edit(new Runnable() {
			public void run() {
				setScaleUnsafe(sc);
			}
		});
	}

	public void setScaleRelative(Scale sc) {
		edit(new Runnable() {
			public void run() {
				setScaleUnsafe(fractal.scale().relative(sc));
			}
		});
	}

	private void setScaleUnsafe(Scale sc) {
		fractal = fractal.copyNewScale(sc);
		drawer.setScale(sc); // not necessary to update the whole fractal.
	}

	public void setFractal(Fractal f) {
		edit(new Runnable() {
			public void run() {
				setFractalUnsafe(f);
			}
		});
	}

	private void setFractalUnsafe(Fractal f) {
		// f must have been parsed and compiled.
		this.fractal = f;
		drawer.setFractal(this.fractal);
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

			// FIXME Make more flexible! Editors can set whether a rerun is needed.
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

		for(BitmapFragmentListener listener : listeners) {
			listener.drawerStarted(this);
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
	 * @return bitmap of this bitmap fragment. Might be null if no
	 * bitmap has been generated yet.
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


}
