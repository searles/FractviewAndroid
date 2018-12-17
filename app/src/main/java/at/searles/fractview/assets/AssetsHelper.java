package at.searles.fractview.assets;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.ContextThemeWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.stream.Collectors;

import at.searles.fractal.data.FractalData;

/**
 * Created by searles on 24.01.17.
 */

public class AssetsHelper {

    public static FractalData defaultFractal(ContextThemeWrapper context) {
        AssetManager am = context.getAssets();
        try(InputStream is = am.open("sources/Default.fv")) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String source = br.lines().collect(Collectors.joining("\n"));
            return new FractalData(source, Collections.emptyMap());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }



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
