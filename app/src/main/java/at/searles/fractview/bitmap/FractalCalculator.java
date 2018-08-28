package at.searles.fractview.bitmap;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalProvider;
import at.searles.fractview.fractal.Drawer;
import at.searles.fractview.fractal.DrawerListener;
import at.searles.fractview.main.InitializationFragment;

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
public class FractalCalculator implements DrawerListener, FractalProvider.Listener {

	private static final int DEFAULT_WIDTH = 640;
	private static final int DEFAULT_HEIGHT = 480;

	/**
	 * Basic parameters
	 */
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
	 * The state of the FractalCalculator
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
	 * Listeners
	 */
	private List<FractalCalculatorListener> listeners = new LinkedList<>();

	// if true, the drawing is currently running and the drawer should
	// not be modified. Only modified in UI thread
	public enum Status {
		RUNNING,      // drawer is running
		IDLE          // waiting for IdleJobs
	}

	public FractalCalculator(int width, int height, Fractal fractal, InitializationFragment initFragment) {
		this.status = Status.IDLE;

		this.width = width;
		this.height = height;

		initializeDrawer(initFragment);

		drawer.setFractal(fractal);

		// Initialization is finished.
		startBackgroundTask();
	}



	// =============================================================
	// ====== Managing the drawer ==================================
	// =============================================================

	private void initializeDrawer(InitializationFragment fragment) {

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
	public void removeIdleJob(IdleJob job) {
		if(!jobQueue.remove(job)) {
			Log.i(getClass().getName(), "tried to remove job " + job + " but it was not in the queue...");
		}
	}

	public void addIdleJob(IdleJob job, boolean highPriority, boolean cancelRunning) {
		Log.d(getClass().getName(), "addIdleJob=" + job + ", cancel=" + cancelRunning);

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
		Log.d(getClass().getName(), "checking for next job");

		// in ui thread.
		if(status != Status.IDLE) {
			throw new IllegalArgumentException("must be processing!");
		}

		if(jobQueue.isEmpty()) {
			Log.d(getClass().getName(), "no further jobs, will trigger redraw: " + triggerStart);

			if(triggerStart) {
				triggerStart = false;
				startBackgroundTask();
			}

			return;
		}

		Log.d(getClass().getName(), "fetching next job");

		IdleJob job = jobQueue.removeFirst();

		triggerStart |= job.restartDrawing();

		if(job.isFinished()) {
			Log.d(getClass().getName(), "job is already finished: " + job);
			executeNextJob();
			return;
		}

		Log.d(getClass().getName(), "setting callback for job " + job);

		// job seems to be doing some work in the background. We tell
		// it to call the next job when it is finished.
		job.setCallback(new IdleJob.Callback() {
			@Override
			public void jobIsFinished(IdleJob job) {
				Log.d(job.toString(), "Job is finally finished");
				executeNextJob();
			}
		});

		if(job.isPending()) {
			Log.d(getClass().getName(), "Starting job");
			job.startJob(); // might immediately recursively call handleJobs.
		} else {
			Log.d(getClass().getName(), "Job was already started");
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

		for(FractalCalculatorListener listener : listeners) {
			listener.drawerStarted(this);
		}

		drawer.start();
	}


	public void dispose() {
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
				for(FractalCalculatorListener listener : listeners) {
					if(firstUpdate) {
						listener.previewGenerated(FractalCalculator.this);
					} else {
						listener.bitmapUpdated(FractalCalculator.this);
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
				Log.d(getClass().getName(), "setting status to idle and notifying listeners");

				status = Status.IDLE;

				for(FractalCalculatorListener listener : listeners) {
					listener.drawerFinished(-1, FractalCalculator.this);
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
		addIdleJob(IdleJob.editor(
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
	 * schedules a task for it. FractalCalculator status must not be 'initializing'
	 * @param width the new width of the image
	 * @param height the new height of the image
	 * @return false if the image could not be created (eg because there wasn't
	 * enough memory)
	 */
	public boolean setSize(int width, int height) {
		if(!prepareSetSize(width, height)) {
			return false;
		}

		addIdleJob(IdleJob.editor(
				new Runnable() {
					@Override
					public void run() {
						FractalCalculator.this.applyNewSize();
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

			for(FractalCalculatorListener l : listeners) {
				l.newBitmapCreated(FractalCalculator.this);
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

	public void addListener(FractalCalculatorListener listener) {
		Log.d(getClass().getName(), "adding listener " + listener);
		listeners.add(listener);
	}

	public void removeListener(FractalCalculatorListener listener) {
		Log.d(getClass().getName(), "removing listener " + listener);

		if(!listeners.remove(listener)) {
			Log.e(getClass().getName(), "Trying to remove a non-existing listener: " + listener);
		}
	}


}
