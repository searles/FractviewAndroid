package at.searles.fractview.parameters;

/**
 * Created by searles on 27.02.18.
 */

public class SaveDialogFragment {
    /************************************
     Input: BitmapFragment that should be saved.

     1. Dialog [Share Image / Save Image to Gallery / Set Image as Wallpaper]
     1.1 if Save Image to Gallery, enter a filename.

     2. Create save-task
     AsyncTask saveJob = new AsyncTask() {
     preRun() {
     dismissSaveNowDialog();

     if(!isCancelled()) {
     createSavingDialogFragment();
     }
     }

     runBG() {
     if(!isCancelled()) {
     save();
     }
     }

     postRun() {
     dismissSavingDialogFragment();

     if(!isCancelled()) {
     if share run share activity.
     }
     }
     }

     if(bf.isRunning()) {
     create dialogfragment with save instantly/cancel

     void onSaveInstantly() {
     saveJob.execute();
     }

     void onCancel() {
     saveJob.cancel();
     }
     }

     bitmapFragment.scheduleJob(saveJob, true);

     3. Register as idlejob

     */
}
