package at.searles.fractview;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

import at.searles.fractal.Fractal;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

/**
 * Created by searles on 24.01.17.
 */

public class AssetsHelper {
    /**
     * Try to read content of assets-folder
     * @param am The asset manager that should be used
     * @param filename The filename to be read
     * @return The content of the file as a string, null in case of an error
     */
    public static String readSourcecode(AssetManager am, String filename) {
        BufferedReader reader = null;
        String program = null;

        try {
            reader = new BufferedReader(
                    new InputStreamReader(am.open(filename)));

            StringBuilder sb = new StringBuilder();

            String mLine;
            while ((mLine = reader.readLine()) != null) {
                //process line
                sb.append(mLine).append("\n");
            }

            program = sb.toString();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                    Log.e("PF", "close failed!");
                    e.printStackTrace();
                }
            }
        }

        return program;
    }

    /**
     * Reads an icon from assets
     * @param am Asset Manager to access it
     * @param iconFilename Filename of the icon.
     * @return null if there is no such file. The error message is logged
     */
    public static Bitmap readIcon(AssetManager am, String iconFilename) {
        if(iconFilename == null) return null;

        Bitmap icon = null;
        InputStream is = null;

        try {
            is = am.open("icons/" + iconFilename);
            icon = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
            //Toast.makeText(this, "Could not read icon", Toast.LENGTH_LONG).show();
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return icon;
    }


}
