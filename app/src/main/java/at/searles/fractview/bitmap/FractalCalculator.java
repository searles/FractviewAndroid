package at.searles.fractview.bitmap;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.LinkedList;

import at.searles.fractal.Fractal;
import at.searles.fractview.fractal.DrawerContext;
import at.searles.fractview.fractal.DrawerListener;
import at.searles.fractview.provider.CalculatorWrapper;

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
 *     * Communication DrawerContext <=> BMF:
 *         BMF fetches progress from drawerContext
 *         BMF sends cancel-request to drawerContext
 *         BMF can check whether DrawerContext is finished
 *         BMF can block thread until drawerContext is finished (for edit)
 *
 *
 *
 * An Editor
 */
public class FractalCalculator implements DrawerListener, Fractal.Listener {

	/**
	 * Basic parameters
	 */
	private int height, width;

	/**
	 * The drawerContext does the actual drawing operation
	 */
	private DrawerContext drawerContext;

	/**
	 * The state of the FractalCalculator
	 */
	private Status status;

	/**
	 * The job-dequeue.
	 */
	private LinkedList<IdleJob> jobQueue = new LinkedList<>();

	/**
	 * Set by IdleJob. If true, then the drawerContext will restart after
	 * finishing the job queue.
	 */
	private boolean triggerStart = false;

	private CalculatorWrapper parent;

	public void initializeFractal(Fractal fractal) {
		this.drawerContext.setFractal(fractal);
		fractal.addListener(this);
	}

	// if true, the drawing is currently running and the drawerContext should
	// not be modified. Only modified in UI thread
	public enum Status {
		RUNNING,      // drawerContext is running
		IDLE          // waiting for IdleJobs
	}

	public FractalCalculator(CalculatorWrapper parent) {
		this.parent = parent;
		this.status = Status.IDLE;
	}

	// =============================================================
	// ====== Managing the drawerContext ==================================
	// =============================================================

	public void setDrawerContext(int width, int height, DrawerContext drawerContext) {
		this.drawerContext = drawerContext;
		this.drawerContext.init();
		this.drawerContext.setListener(this);

		// create bitmap
		DrawerContext.Alloc bitmapAlloc = createBitmapAlloc(width, height);

		while (bitmapAlloc == null) {
			width /= 2;
			height /= 2;

			bitmapAlloc = createBitmapAlloc(width, height);
		}

		setBitmapAlloc(bitmapAlloc);
	}

	public void initializeRunLoop() {
		// first call.
		startBackgroundTask();
	}

	public float progress() {
		return drawerContext.progress();
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
			drawerContext.cancel();
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

		drawerContext.start();

		parent.onCalculatorStarted();
	}

	public void destroy() {
		if (drawerContext != null) {
			drawerContext.cancel();
			drawerContext = null;
		}
	}

	@Override
	public void drawingUpdated(boolean firstUpdate) {
		// not in the UI thread!
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				if(firstUpdate) {
					parent.onPreviewGenerated();
				} else {
					parent.onBitmapUpdated();
				}
			}
		});
	}

	@Override
	public void drawingFinished() {
		// Not in the UI Thread.
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				Log.d(getClass().getName(), "setting status to idle and notifying listeners");
				status = Status.IDLE;
				parent.onDrawerFinished();
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
						// drawerContext needs to know
						drawerContext.setFractal(fractal);
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
		DrawerContext.Alloc bitmapAlloc = createBitmapAlloc(width, height);

		if(bitmapAlloc == null) {
			return false;
		}

		addIdleJob(IdleJob.editor(
				new Runnable() {
					@Override
					public void run() {
						FractalCalculator.this.setBitmapAlloc(bitmapAlloc);
					}
				}), true, true);

		return true;
	}

	/**
	 * Creates a new bitmap in newBitmap that is later used as new bitmap before
	 * the next calculation is started.
	 * @param width The new width
	 * @param height The new height
	 * @return null if out of mem
	 */
	private DrawerContext.Alloc createBitmapAlloc(int width, int height) {
		try {
			Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			return drawerContext.createBitmapAlloc(newBitmap);
		} catch(OutOfMemoryError e) {
			return null;
		}
	}

	private void setBitmapAlloc(DrawerContext.Alloc alloc) {
		// there might very theoretically be multiple
		// calls before the calculation stops,
		// therefore check for null.
		drawerContext.setBitmapAlloc(alloc);

		this.width = alloc.bitmap.getWidth();
		this.height = alloc.bitmap.getHeight();

		parent.onNewBitmapCreated();
	}

	// =============================================================
	// ====== Some get functions ===================================
	// =============================================================

	/**
	 * @return bitmap of this bitmap fragment. Might be null if no
	 * bitmap has been generated yet.
	 */
    public Bitmap bitmap() {
        return drawerContext.bitmap();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
