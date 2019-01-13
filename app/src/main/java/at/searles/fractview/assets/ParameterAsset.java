package at.searles.fractview.assets;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Deserialization of data via FractalDataAdapter.deserializeParameters!
 */
public class ParameterAsset {
    @SerializedName("title")
    public String title;
    @SerializedName("description")
    public String description;
    @SerializedName("icon")
    public String iconFilename;
    @SerializedName("data")
    public Map<String, Object> data;

    private transient Bitmap icon;

    ParameterAsset() {}

    ParameterAsset(String title, String description, Map<String, Object> data) {
        this.title = title;
        this.description = description;
        this.data = data;
    }

    Bitmap icon(AssetManager am) {
        if (icon == null && iconFilename != null) {
            icon = AssetsHelper.readIcon(am, iconFilename);
        }

        return icon;
    }
}
