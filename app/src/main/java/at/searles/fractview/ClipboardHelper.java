package at.searles.fractview;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import at.searles.fractview.fractal.Fractal;

/**
 * Created by searles on 17.01.17.
 */

public class ClipboardHelper {
    public static void copyFractal(Context context, Fractal fractal) {
        try {
            String export = fractal.toJSON().toString(2);
            copy(context, export);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "ERROR: Could not create JSON", Toast.LENGTH_LONG).show();
        }
    }

    public static Fractal pasteFractal(Context context) {
        CharSequence pasteText = ClipboardHelper.paste(context);

        if(pasteText != null) {
            try {
                return Fractal.fromJSON(new JSONObject(pasteText.toString()));
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(context, "ERROR: Could not parse content of clipboard", Toast.LENGTH_LONG).show();
            }
        }

        return null;
    }



    public static void copy(Context context, String copyText) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("fractview", copyText);
        clipboard.setPrimaryClip(clip);
    }

    /**
     *
     * @return null if clipboard was empty
     */
    public static CharSequence paste(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(context.CLIPBOARD_SERVICE);

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
