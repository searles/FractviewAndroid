package at.searles.fractview.assets;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.searles.fractal.ParserInstance;
import at.searles.fractal.data.ParameterType;
import at.searles.meelan.compiler.Ast;
import at.searles.meelan.optree.inlined.ExternDeclaration;

class SourceAsset {
    @SerializedName("title")
    String title;

    @SerializedName("file")
    String sourceFilename;

    @SerializedName("description")
    String description;

    @SerializedName("icon")
    String iconFilename;

    private transient Bitmap icon;
    private transient String source;
    private transient Ast ast;
    private transient Map<String, ParameterType> parameterTypes;

    SourceAsset() {}

    SourceAsset(String title, String description, String source) {
        this.title = title;
        this.description = description;

        // sourceFilename is not queried if source is set.
        this.source = source;
    }

    Bitmap icon(AssetManager am) {
        if (icon == null && iconFilename != null) {
            icon = AssetsHelper.readIcon(am, iconFilename);
        }

        return icon;
    }

    String source(AssetManager am) {
        if(source == null) {
            source = AssetsHelper.readSourcecode(am, sourceFilename);
        }

        return source;
    }

    Ast ast(AssetManager am) {
        if(ast == null) {
            String source = source(am);
            ast = ParserInstance.get().parseSource(source);
        }

        return ast;
    }

    Map<String, ParameterType> parameterTypes(AssetManager am) {
        if(parameterTypes == null) {
            Ast ast = ast(am);
            List<ExternDeclaration> externDeclarations = ast.traverseExternData();
            parameterTypes = new HashMap<>();
            externDeclarations.forEach(e -> parameterTypes.put(e.id, ParameterType.fromString(e.externTypeString)));
        }

        return parameterTypes;
    }
}
