package at.searles.fractview;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import at.searles.fractal.Fractal;
import at.searles.fractal.gson.Serializers;
import at.searles.fractview.ui.DialogHelper;

/**
 * Created by searles on 17.01.17.
 */

public class ClipboardHelper {
    public static void copyFractal(Context context, Fractal fractal) {
        String export = Serializers.serializer().toJson(fractal);
        copy(context, export);
    }

    public static Fractal pasteFractal(Context context) {
        CharSequence pasteText = ClipboardHelper.paste(context);

        if(pasteText != null) {
            return Serializers.serializer().fromJson(pasteText.toString(), Fractal.class);
        } else {
            DialogHelper.error(context, "Clipboard is empty");
            return null;
        }
    }



    public static void copy(Context context, String copyText) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("fractview", copyText);
        clipboard.setPrimaryClip(clip);
    }

    /**
     *
     * @return null if clipboard was empty
     */
    public static CharSequence paste(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        if(!clipboard.hasPrimaryClip()) {
            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_LONG).show();
            return null;
        }

        /*if(!clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            Toast.makeText(this, "Clipboard does not contain text", Toast.LENGTH_LONG).show();
            return false;
        }*/

        return clipboard.getPrimaryClip().getItemAt(0).getText();
    }
}
