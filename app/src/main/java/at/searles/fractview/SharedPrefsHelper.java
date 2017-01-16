package at.searles.fractview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

/**
 * Helper for Shared Preferences
 */
public class SharedPrefsHelper {

    /**
     * @param key The key for the provided shared preferences
     * @return a String stored in sharedPreferences
     */
    public static String loadFromSharedPref(Context context, String prefsName, String key) {
        SharedPreferences prefs = context.getSharedPreferences(
                prefsName,
                Context.MODE_PRIVATE);

        return prefs.getString(key, null);
    }

    public static void saveToSharedPref(Context context, String prefsName, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(
                prefsName,
                Context.MODE_PRIVATE);

        if(prefs.contains(key)) {
            // Check whether this pref should really be overwritten

            // todo should I create  a new dialog fragment for yes/no?

            AlertDialog.Builder yesNoBuilder = new AlertDialog.Builder(context);
            yesNoBuilder.setIcon(android.R.drawable.ic_delete);
            yesNoBuilder.setTitle("Overwrite entry " + key + "?");

            yesNoBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putString(key, value);
                    edit.apply();
                    dialogInterface.dismiss();
                }
            });

            yesNoBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    dialogInterface.dismiss();
                }
            });

            yesNoBuilder.show();
        } else {
            // does not exist yet.
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(key, value);
            edit.apply();
        }
    }
}
