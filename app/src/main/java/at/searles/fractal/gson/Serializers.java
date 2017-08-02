package at.searles.fractal.gson;

import android.graphics.Bitmap;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import at.searles.fractal.FavoriteEntry;
import at.searles.fractal.Fractal;
import at.searles.fractview.Commons;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

/**
 * This class contains all serializers for custom classes from this and other
 * projects. Json-Serialization should only use this class.
 */

public class Serializers {

    private static Gson gson = null;

    public static Gson serializer() {
        // No need for synchronization since this will usually
        // be only called from the ui thread.
        if(gson == null) {
            GsonBuilder gsonBuilder = new GsonBuilder();

            // register some types
            gsonBuilder.registerTypeAdapter(Cplx.class, new CplxAdapter());
            gsonBuilder.registerTypeAdapter(Scale.class, new ScaleAdapter());
            gsonBuilder.registerTypeAdapter(Palette.class, new PaletteAdapter());
            gsonBuilder.registerTypeAdapter(Fractal.class, new FractalAdapter());
            gsonBuilder.registerTypeAdapter(FavoriteEntry.class, new FavoriteEntryAdapter());
            gsonBuilder.registerTypeAdapter(FavoriteEntry.Collection.class, new FavoritesCollectionAdapter());

            gson = gsonBuilder.create();
        }

        return gson;
    }

    public static class CplxAdapter implements JsonDeserializer<Cplx>, JsonSerializer<Cplx> {
        @Override
        public Cplx deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonArray array = (JsonArray) json;

            double re = array.get(0).getAsDouble();
            double im = array.get(1).getAsDouble();

            return new Cplx(re, im);
        }

        @Override
        public JsonElement serialize(Cplx cplx, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray array = new JsonArray();

            array.add(cplx.re());
            array.add(cplx.im());

            return array;
        }
    }

    public static class ScaleAdapter implements JsonDeserializer<Scale>, JsonSerializer<Scale> {
        @Override
        public Scale deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonArray array = (JsonArray) json;

            double data[] = new double[6];

            for (int i = 0; i < 6; ++i)
                data[i] = array.get(i).getAsDouble();

            return new Scale(data[0], data[1], data[2], data[3], data[4], data[5]);
        }

        @Override
        public JsonElement serialize(Scale scale, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray array = new JsonArray();

            array.add(scale.xx());
            array.add(scale.xy());
            array.add(scale.yx());
            array.add(scale.yy());
            array.add(scale.cx());
            array.add(scale.cy());

            return array;
        }
    }

    // Next palette.

    public static class PaletteAdapter implements JsonDeserializer<Palette>, JsonSerializer<Palette> {
        private static final String WIDTH_LABEL = "width";
        private static final String HEIGHT_LABEL = "height";
        private static final String COLORS_LABEL = "colors";

        @Override
        public Palette deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject object = (JsonObject) json;

            int width = object.get(WIDTH_LABEL).getAsInt();
            int height = object.get(HEIGHT_LABEL).getAsInt();

            JsonArray array = object.getAsJsonArray(COLORS_LABEL);

            int colors[] = new int[height * width];

            for (int i = 0; i < colors.length; ++i) {
                colors[i] = array.get(i).getAsInt();
            }

            return new Palette(width, height, colors);
        }

        @Override
        public JsonElement serialize(Palette palette, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();

            object.addProperty(WIDTH_LABEL, palette.width());
            object.addProperty(HEIGHT_LABEL, palette.height());

            JsonArray array = new JsonArray();

            for (int y = 0; y < palette.height(); ++y) {
                for (int x = 0; x < palette.width(); ++x) {
                    array.add(palette.argb(x, y));
                }
            }

            object.add(COLORS_LABEL, array);

            return object;
        }
    }

    // Next is FavoritesEntry

    public static class FavoriteEntryAdapter implements JsonDeserializer<FavoriteEntry>, JsonSerializer<at.searles.fractal.FavoriteEntry> {
        private static final String FRACTAL_LABEL = "fractal";
        private static final String ICON_LABEL = "icon"; // this is optional
        private static final String DESCRIPTION_LABEL = "description"; // this is optional

        @Override
        public FavoriteEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            // In older versions, key and descriptor did not exist in this object.
            JsonObject obj = (JsonObject) json;
            Fractal fractal = context.deserialize(obj.get(FRACTAL_LABEL), Fractal.class);

            Bitmap icon = null;

            JsonElement iconJson = obj.get(ICON_LABEL);

            if(iconJson != null) {
                String iconBase64 = iconJson.getAsString();
                byte[] iconBinary = Base64.decode(iconBase64, Base64.DEFAULT);
                icon = Commons.fromPNG(iconBinary);
            }

            JsonElement descriptionJson = obj.get(DESCRIPTION_LABEL);

            String description = descriptionJson == null ? null : descriptionJson.getAsString();

            return new FavoriteEntry(icon, fractal, description);
        }

        @Override
        public JsonElement serialize(FavoriteEntry entry, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();

            // encode icon byte stream as Base64
            byte[] icon = Commons.toPNG(entry.icon());
            obj.addProperty(ICON_LABEL, Base64.encodeToString(icon, Base64.DEFAULT));

            obj.add(FRACTAL_LABEL, context.serialize(entry.fractal(), Fractal.class));

            obj.addProperty(DESCRIPTION_LABEL, entry.description());

            return obj;
        }
    }

    public static class FavoritesCollectionAdapter implements JsonSerializer<FavoriteEntry.Collection>, JsonDeserializer<FavoriteEntry.Collection> {

        @Override
        public FavoriteEntry.Collection deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = (JsonObject) json;

            FavoriteEntry.Collection collection = new FavoriteEntry.Collection();

            for(Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                collection.add(entry.getKey(), context.deserialize(entry.getValue(), FavoriteEntry.class));
            }

            return collection;
        }

        @Override
        public JsonElement serialize(FavoriteEntry.Collection src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();

            for(Map.Entry<String, FavoriteEntry> entry : src.getAll()) {
                JsonElement jsonEntry = context.serialize(entry.getValue(), FavoriteEntry.class);
                object.add(entry.getKey(), jsonEntry);
            }

            return object;
        }
    }

    public static class FractalAdapter implements JsonSerializer<Fractal>, JsonDeserializer<Fractal> {
        private static final String SCALE_LABEL = "scale";
        private static final String SOURCE_LABEL = "source";

        private static final String INTS_LABEL = "ints";
        private static final String REALS_LABEL = "reals";
        private static final String CPLXS_LABEL = "cplxs";
        private static final String BOOLS_LABEL = "bools";
        private static final String EXPRS_LABEL = "exprs";
        private static final String COLORS_LABEL = "colors";
        private static final String PALETTES_LABEL = "palettes";
        private static final String SCALES_LABEL = "scales";

        private static final String DATA_LABEL = "arguments";

        @Override
        public JsonElement serialize(Fractal fractal, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject ret = new JsonObject();

            // Scale is now stored in data fields.

            JsonArray sourceArray = new JsonArray();

            for (String line : fractal.sourceCode().split("\n")) {
                sourceArray.add(line);
            }

            ret.add(SOURCE_LABEL, sourceArray);

            JsonObject ints = new JsonObject();
            JsonObject reals = new JsonObject();
            JsonObject cplxs = new JsonObject();
            JsonObject bools = new JsonObject();
            JsonObject exprs = new JsonObject();
            JsonObject colors = new JsonObject();
            JsonObject palettes = new JsonObject();
            JsonObject scales = new JsonObject();

            for(Map.Entry<String, Fractal.Parameter> pair : fractal.nonDefaultParameters()) {
                String id = pair.getKey();
                Fractal.Parameter element = pair.getValue();
                Object value = element.value();

                switch (element.type()) {
                    case Int:
                        ints.addProperty(id, (Integer) value);
                        break;
                    case Real:
                        reals.addProperty(id, (Double) value);
                        break;
                    case Cplx:
                        cplxs.add(id, context.serialize(value, Cplx.class));
                        break;
                    case Bool:
                        bools.addProperty(id, (Boolean) value);
                        break;
                    case Expr:
                        exprs.addProperty(id, (String) value);
                        break;
                    case Color:
                        colors.addProperty(id, (Integer) value);
                        break;
                    case Palette:
                        palettes.add(id, context.serialize(value, Palette.class));
                        break;
                    case Scale:
                        scales.add(id, context.serialize(value, Scale.class));
                        break;
                    default:
                        throw new IllegalArgumentException("Type not implemented: " + element.type());
                }
            }

            JsonObject data = new JsonObject();

            if (!ints.entrySet().isEmpty()) data.add(INTS_LABEL, ints);
            if (!reals.entrySet().isEmpty()) data.add(REALS_LABEL, reals);
            if (!cplxs.entrySet().isEmpty()) data.add(CPLXS_LABEL, cplxs);
            if (!bools.entrySet().isEmpty()) data.add(BOOLS_LABEL, bools);
            if (!exprs.entrySet().isEmpty()) data.add(EXPRS_LABEL, exprs);
            if (!colors.entrySet().isEmpty()) data.add(COLORS_LABEL, colors);
            if (!palettes.entrySet().isEmpty()) data.add(PALETTES_LABEL, palettes);
            if (!scales.entrySet().isEmpty()) data.add(SCALES_LABEL, scales);

            if (!data.entrySet().isEmpty()) ret.add(DATA_LABEL, data);

            return ret;
        }

        public Fractal deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            // Scale is stored as double-array
            JsonObject obj = (JsonObject) json;


            StringBuilder sourceCode = new StringBuilder();
            JsonArray sourceArray = obj.getAsJsonArray(SOURCE_LABEL);

            for (JsonElement line : sourceArray) {
                sourceCode.append(line.getAsString()).append('\n');
            }

            // Fetch data.
            Map<String, Fractal.Parameter> dataMap = new HashMap<>();

            JsonObject data = obj.getAsJsonObject(DATA_LABEL);

            if (data != null) {
                // all of them are optional.
                JsonObject ints = data.getAsJsonObject(INTS_LABEL);
                JsonObject reals = data.getAsJsonObject(REALS_LABEL);
                JsonObject cplxs = data.getAsJsonObject(CPLXS_LABEL);
                JsonObject bools = data.getAsJsonObject(BOOLS_LABEL);
                JsonObject exprs = data.getAsJsonObject(EXPRS_LABEL);
                JsonObject colors = data.getAsJsonObject(COLORS_LABEL);
                JsonObject palettes = data.getAsJsonObject(PALETTES_LABEL);
                JsonObject scales = data.getAsJsonObject(SCALES_LABEL);

                if (ints != null) for (Map.Entry<String, JsonElement> entry : ints.entrySet()) {
                    dataMap.put(entry.getKey(), new Fractal.Parameter(Fractal.Type.Int, entry.getValue().getAsInt()));
                }

                if (reals != null) for (Map.Entry<String, JsonElement> entry : reals.entrySet()) {
                    dataMap.put(entry.getKey(), new Fractal.Parameter(Fractal.Type.Real, entry.getValue().getAsDouble()));
                }

                if (cplxs != null) for (Map.Entry<String, JsonElement> entry : cplxs.entrySet()) {
                    dataMap.put(entry.getKey(), new Fractal.Parameter(Fractal.Type.Cplx, context.deserialize(entry.getValue(), Cplx.class)));
                }

                if (bools != null) for (Map.Entry<String, JsonElement> entry : bools.entrySet()) {
                    dataMap.put(entry.getKey(), new Fractal.Parameter(Fractal.Type.Bool, entry.getValue().getAsBoolean()));
                }

                if (exprs != null) for (Map.Entry<String, JsonElement> entry : exprs.entrySet()) {
                    dataMap.put(entry.getKey(), new Fractal.Parameter(Fractal.Type.Expr, entry.getValue().getAsString()));
                }

                if (colors != null) for (Map.Entry<String, JsonElement> entry : colors.entrySet()) {
                    dataMap.put(entry.getKey(), new Fractal.Parameter(Fractal.Type.Color, entry.getValue().getAsInt()));
                }

                if (palettes != null) for (Map.Entry<String, JsonElement> entry : palettes.entrySet()) {
                    dataMap.put(entry.getKey(), new Fractal.Parameter(Fractal.Type.Palette, context.deserialize(entry.getValue(), Palette.class)));
                }

                if (scales != null) for (Map.Entry<String, JsonElement> entry : scales.entrySet()) {
                    dataMap.put(entry.getKey(), new Fractal.Parameter(Fractal.Type.Scale, context.deserialize(entry.getValue(), Scale.class)));
                }
            }
            
            
            // In old versions, scale was on top. There will be
            // most likely forever Jsons with a dedicated scale
            // field. For these, read scale from here.
            JsonElement element = obj.get(SCALE_LABEL);

            if(element != null) {
                Scale scale = context.deserialize(element, Scale.class);
                dataMap.put(Fractal.SCALE_KEY, new Fractal.Parameter(Fractal.Type.Scale, scale));
            }

            return new Fractal(sourceCode.toString(), dataMap);
        }
    }
}
