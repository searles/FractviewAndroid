package at.searles.fractview.bitmap;

/**
 * This class represents jobs that can be started from bitmap fragment if it is idle.
 * For this purpose, they are scheduled using scheduleIdleJob, but they can also
 * be started independent from a bitmap fragment (eg if the bitmap is saved without
 * waiting for the rendering to finish). In the latter case, scheduling a job will
 * not restart it but still wait until it has finished before the next job is started.
 * (if the job is already finished)
 */
public abstract class IdleJob {

    private final boolean restartDrawing;
    private Callback callback;
    private Status status;

    public enum Status { PENDING, RUNNING, FINISHED };

    public interface Callback {
        /**
         * This method is always called if a job finished.
         */
        void jobIsFinished(IdleJob job);
    }

    protected IdleJob(boolean restartDrawing) {
        this.callback = null;
        this.status = Status.PENDING;
        this.restartDrawing = restartDrawing;
    }

    public boolean restartDrawing() {
        return restartDrawing;
    }

    public void startJob() {
        if(this.status == Status.PENDING) {
            this.status = Status.RUNNING;

            // do actual start.
            onStart();
        }
    }

    public boolean isFinished() {
        return this.status == Status.FINISHED;
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    protected abstract void onStart();

    public final void onFinished() {
        // must be called from ui thread

        this.status = Status.FINISHED;

        if(callback != null) {
            callback.jobIsFinished(this);
            callback = null;
        }
    }

    public void setCallback(Callback callback) {
        // in ui thread

        if(status == Status.FINISHED) {
            callback.jobIsFinished(this);
        } else {
            this.callback = callback;
        }
    }

    public static IdleJob editor(Runnable runnable) {
        return new IdleJob(true) {
            protected void onStart() {
                runnable.run();
                onFinished();
            }
        };
    }
}

