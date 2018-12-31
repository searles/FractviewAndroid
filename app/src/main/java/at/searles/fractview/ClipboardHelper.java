package at.searles.fractview;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import at.searles.fractview.ui.DialogHelper;

/**
 * Helper class for clipboard stuff.
 */
public class ClipboardHelper {
    public static void copy(Context context, String copyText) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("fractview", copyText);

        if(clipboard == null) {
            DialogHelper.error(context, "No Clipboard-Manager found.");
            return;
        }

        clipboard.setPrimaryClip(clip);
    }

    /**
     *
     * @return null if clipboard was empty
     */
    public static CharSequence paste(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        if(clipboard == null) {
            DialogHelper.error(context, "No Clipboard-Manager found.");
            return null;
        }

        if(!clipboard.hasPrimaryClip()) {
            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_LONG).show();
            return null;
        }

        return clipboard.getPrimaryClip().getItemAt(0).getText();
    }
}
