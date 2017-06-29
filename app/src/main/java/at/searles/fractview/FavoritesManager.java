package at.searles.fractview;

import android.content.Context;

import at.searles.fractview.fractal.FavoriteEntry;

/**
 * Created by searles on 10.06.17.
 */

public class FavoritesManager {

    public static final String FAVORITES = "favorites";

    public static void add(Context context, String name, FavoriteEntry fav, SharedPrefsHelper.SaveMethod method) {
        String entryString;

            entryString = fav.serialize().toString();

        new SharedPrefsHelper(context, FAVORITES).add(name, entryString, method);
    }
}
