package at.searles.fractview.assets;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Created by searles on 24.01.17.
 */

public class AssetsHelper {
    /**
     * Try to read content of assets-folder
     * @param am The asset manager that should be used
     * @param title The filename to be read (+ .fv extension)
     * @return The content of the file as a string, null in case of an error
     */
    public static String readSourcecode(AssetManager am, String title) {
        BufferedReader reader = null;
        String program = null;

        try(InputStream is = am.open(String.format("sources/%s.fv", title))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Reads an icon from assets
     * @param am Asset Manager to access it
     * @param iconFilename Filename of the icon.
     * @return null if there is no such file. The error message is logged
     */
    public static Bitmap readIcon(AssetManager am, String iconFilename) {
        if(iconFilename == null) return null;

        try(InputStream is = am.open("icons/" + iconFilename)) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
