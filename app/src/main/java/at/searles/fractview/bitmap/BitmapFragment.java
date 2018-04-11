package at.searles.fractview.bitmap;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import at.searles.fractal.Drawer;
import at.searles.fractal.DrawerListener;
import at.searles.fractal.Fractal;
import at.searles.fractview.MainActivity;
import at.searles.fractview.bitmap.ui.BitmapFragmentAccessor;
import at.searles.fractview.fractal.FractalProviderListener;
import at.searles.fractview.renderscript.RenderScriptFragment;
import at.searles.fractview.renderscript.RenderScriptListener;
import at.searles.fractview.saving.SaveInBackgroundFragment;

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
public class BitmapFragment extends Fragment implements DrawerListener, RenderScriptListener, BitmapFragmentAccessor, FractalProviderListener {

	private static final String WIDTH_LABEL = "width";
	private static final String HEIGHT_LABEL = "height";

	// conservative defaults if there are no valid values available...
	private static final int DEFAULT_WIDTH = 800;
	private static final int DEFAULT_HEIGHT = 480;

	// Allocations and various data
	private int height, width;

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
	 * The state of the BitmapFragment
	 */
	private Status status;

	/**
	 * The job-dequeue.
	 */
	private LinkedList<IdleJob> jobQueue = new LinkedList<>();

	/**
	 * Set by IdleJob. If true, then the drawer will restart after
	 * finishing the job queue.
	 */
	private boolean triggerStart = false;

	/**
	 * Initial fractal. It is only used during initialization.
	 */
	private Fractal initialFractal;

	/**
	 * Listeners
	 */
	private List<BitmapFragmentListener> listeners = new LinkedList<>();

	// if true, the drawing is currently running and the drawer should
	// not be modified. Only modified in UI thread
	public enum Status {
		INITIALIZING,
		RUNNING,      // drawer is running
		IDLE          // waiting for IdleJobs
	}

	/*
	 * Life cycle:
	 * Initializing, until an initial fractal is provided and a drawer is set.
	 * Running: The drawer is calculating the bitmap. Ready to accept edit-jobs.
	 * Idle: Ready to accept edit-jobs.
	 * Processing: Ready to accept edit-jobs.
	 */

	public static BitmapFragment newInstance(int width, int height) {
		// set initial size
		Bundle bundle = new Bundle();

		bundle.putInt(WIDTH_LABEL, width);
		bundle.putInt(HEIGHT_LABEL, height);

		BitmapFragment ft = new BitmapFragment();
		ft.setArguments(bundle);

		return ft;
	}

	public BitmapFragment() {
		this.status = Status.INITIALIZING;
	}

	public boolean isInitializing() {
		return status == Status.INITIALIZING;
	}

	/**
	 * This is used eg for SaveInBackgroundFragments that attach themselves
	 * to this fragment to access the bitmap
	 * @param childFragment The fragment to be attached
	 * @param tag The tag for the fragment manager.
	 */
	public void registerFragmentAsChild(Fragment childFragment, String tag) {
		FragmentManager fm = getChildFragmentManager();
		FragmentTransaction fragmentTransaction = fm.beginTransaction();
		fragmentTransaction.add(childFragment, tag);
		fragmentTransaction.commit();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true); // preserve this one on rotation

		Log.d(getClass().getName(), "onCreate");

		this.width = getArguments().getInt(WIDTH_LABEL);
		this.height = getArguments().getInt(HEIGHT_LABEL);

		// set up logic
		RenderScriptFragment renderScriptFragment =
				(RenderScriptFragment) getFragmentManager().findFragmentByTag(MainActivity.RENDERSCRIPT_FRAGMENT_TAG);

		if(!renderScriptFragment.isInitializing()) {
			// initialization done, we can already create the drawer
			initializeDrawer(renderScriptFragment);
		} else {
			// keep us informed when initialization is finished
			renderScriptFragment.addListener(this);
		}
	}

	public float progress() {
		return drawer.progress();
	}

	public boolean isRunning() {
		return status == Status.RUNNING;
	}

	// =============================================================
	// ====== Managing the job queue ===============================
	// =============================================================
	public void removeIdleJob(SaveInBackgroundFragment.SaveJob job) {
		if(!jobQueue.remove(job)) {
			Log.i(getClass().getName(), "tried to remove job " + job + " but it was not in the queue...");
		}
	}

	public void scheduleIdleJob(IdleJob job, boolean highPriority, boolean cancelRunning) {
		Log.d(getClass().getName(), "scheduleIdleJob=" + job + ", cancel=" + cancelRunning);

		// in ui thread
		if(highPriority) {
			jobQueue.addFirst(job);
		} else {
			jobQueue.addLast(job);
		}

		if(status == Status.IDLE) {
			executeNextJob();
		} else if(status == Status.RUNNING && cancelRunning) {
			drawer.cancel();
			// drawerFinished will execute the job queue.
		}

		// if status == EDIT, this job will be executed in the current run
		//   since we are in the UI thread.
		// if status == INVALID, there will be a check for idle tasks.
	}


	/**
	 * fetches a job from the job queue
	 */
	private void executeNextJob() {
		Log.d(getClass().getName(), "executeNextJob");

		// in ui thread.
		if(status != Status.IDLE) {
			throw new IllegalArgumentException("must be processing!");
		}

		if(jobQueue.isEmpty()) {
			if(triggerStart) {
				triggerStart = false;
				startBackgroundTask();
			}

			return;
		}

		IdleJob job = jobQueue.removeFirst();

		triggerStart |= job.restartDrawing();

		if(job.isFinished()) {
			Log.d(getClass().getName(), "job was already finished: " + job);
			executeNextJob();
			return;
		}

		// job seems to be doing some work in the background. We tell
		// it to call the next job when it is finished.
		job.setCallback(new IdleJob.Callback() {
			@Override
			public void jobIsFinished(IdleJob job) {
				executeNextJob();
			}
		});

		if(job.isPending()) {
			job.startJob(); // might immediately recursively call handleJobs.
		}
	}


	/**
	 * This method starts the drawing thread in the background.
	 */
	private void startBackgroundTask() {
		// this is always started from UI-thread
		// start calculation in background
		Log.d(getClass().getName(), "startBackgroundTask");

		status = Status.RUNNING;

		for(BitmapFragmentListener listener : listeners) {
			listener.drawerStarted(this);
		}

		drawer.start();
	}


	// =============================================================
	// ====== Managing the drawer ==================================
	// =============================================================

	@Override
	public void rsInitializationFinished(RenderScriptFragment fragment) {
		// in ui-thread
		initializeDrawer(fragment);
	}

	private void initializeDrawer(RenderScriptFragment fragment) {
		if(this.status != Status.INITIALIZING) {
			throw new IllegalArgumentException("bitmap fragment status already set to " + this.status);
		}

		if(fragment.isInitializing()) {
			throw new IllegalArgumentException("drawer fragment is still initializing");
		}

		if(this.drawer != null) {
			throw new IllegalArgumentException("drawer is already set");
		}

		Log.d(getClass().getName(), "initializing drawer");

		this.drawer = fragment.createDrawer();
		this.drawer.init();

		this.drawer.setListener(this);

		// create bitmap
		if(!prepareSetSize(this.width, this.height)) {
			if(!prepareSetSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)) {
				// crash on purpose
				throw new IllegalArgumentException("Cannot set minimum image size");
			}
		}

		applyNewSize();

		if(this.initialFractal != null) {
			initialLaunch();
		}
	}

	private void initializeFractal(Fractal fractal) {
		Log.d(getClass().getName(), "initialize fractal");

		this.initialFractal = fractal;

		if(this.drawer != null) {
			initialLaunch();
		}
	}

	private void initialLaunch() {
		drawer.setFractal(initialFractal);
		initialFractal = null; // not needed anymore.

		// Initialization is finished.
		startBackgroundTask();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (drawer != null) {
			drawer.cancel();
			drawer = null;
		}
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
		Log.d(getClass().getName(), "drawingFinished");

		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				Log.d(getClass().getName(), "status to idle");

				status = Status.IDLE;

				for(BitmapFragmentListener listener : listeners) {
					listener.drawerFinished(-1, BitmapFragment.this);
				}

				executeNextJob();
			}
		});
	}

	// ========================================
	// === Callback from fractalfragment ======
	// ========================================

	@Override
	public void fractalModified(Fractal fractal) {
		if(status == Status.INITIALIZING) {
			initializeFractal(fractal);
			return;
		}

		scheduleIdleJob(IdleJob.editor(
				new Runnable() {
					public void run() {
						// drawer needs to know
						drawer.setFractal(fractal);
					}
				}
		), false, true);
	}


	// ------------------------
	// Edit image size
	// ------------------------

	/**
	 * Tries to create data structures for a new bitmap of the given size and
	 * schedules a task for it. BitmapFragment status must not be 'initializing'
	 * @param width the new width of the image
	 * @param height the new height of the image
	 * @return false if the image could not be created (eg because there wasn't
	 * enough memory)
	 */
	public boolean setSize(int width, int height) {
		if(!prepareSetSize(width, height)) {
			return false;
		}

		scheduleIdleJob(IdleJob.editor(
				new Runnable() {
					@Override
					public void run() {
						BitmapFragment.this.applyNewSize();
					}
				}), true, true);

		return true;
	}

	/**
	 * Creates a new bitmap in newBitmap that is later used as new bitmap before
	 * the next calculation is started.
	 * @param width The new width
	 * @param height The new height
	 * @return true if creating the new bitmap was successful.
	 */
	private boolean prepareSetSize(int width, int height) {
		try {
			newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

			if(!drawer.prepareSetSize(newBitmap)) {
				// clean up, drawer failed
				newBitmap = null;
				return false;
			}

			return true;
		} catch(OutOfMemoryError e) {
			e.printStackTrace();
			return false;
		}
	}

	private void applyNewSize() {
		if(newBitmap != null) {
			Log.d(getClass().getName(), "new bitmap: " + newBitmap);

			// there might very theoretically be multiple
			// calls before the calculation stops,
			// therefore check for null.
			this.bitmap = this.newBitmap;
			this.newBitmap = null;

			drawer.applyNewSize();

			this.width = bitmap.getWidth();
			this.height = bitmap.getHeight();

			getArguments().putInt(WIDTH_LABEL, this.width);
			getArguments().putInt(HEIGHT_LABEL, this.height);

			for(BitmapFragmentListener l : listeners) {
				l.newBitmapCreated(BitmapFragment.this);
			}
		}
	}



	// =============================================================
	// ====== Some get functions ===================================
	// =============================================================

	/**
	 * @return bitmap of this bitmap fragment. Might be null if no
	 * bitmap has been generated yet.
	 */
    public Bitmap bitmap() {
        return bitmap;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

	// =============================================================
	// ====== Manage listener    ===================================
	// =============================================================

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


}
