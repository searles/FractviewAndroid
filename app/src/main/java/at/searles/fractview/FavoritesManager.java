package at.searles.fractview;

import android.content.Context;

import org.json.JSONException;

import at.searles.fractview.fractal.FavoriteEntry;

/**
 * Created by searles on 10.06.17.
 */

public class FavoritesManager {

    public static final String FAVORITES = "favorites";

    public static void add(Context context, String name, FavoriteEntry fav, SharedPrefsHelper.SaveMethod method) {
        String entryString;

        try {
            entryString = fav.toJSON().toString();
        } catch (JSONException e) {
            DialogHelper.error(context, e.getMessage());
            return;
        }

        new SharedPrefsHelper(context, FAVORITES).add(name, entryString, method);
    }
}
