package at.searles.fractview.parameters.dialogs;

import android.graphics.Color;

import java.util.Optional;

public class ColorParser {
    private String[] colorNames = {
            "red", "blue",
            "green", "black", "white", "gray",
            "cyan", "magenta", "yellow", "lightgray",
            "darkgray", "grey", "lightgrey", "darkgrey",
            "aqua", "fuchsia", "lime", "maroon",
            "navy", "olive", "purple", "silver",
            "teal"
    };
    
    public String[] colorNames() {
        return colorNames;
    }
    
    public Optional<Integer> parseColor(String colorString) {
        // normalize
        colorString = colorString.toLowerCase().replace(" ", "");

        try {
            return Optional.of(Color.parseColor(colorString));
        } catch (IllegalArgumentException ignore) {
            return Optional.empty();
        }
    }
    
}
