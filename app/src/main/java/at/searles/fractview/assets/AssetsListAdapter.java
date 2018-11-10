package at.searles.fractview.assets;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.Parameters;
import at.searles.fractal.entries.SourceEntry;
import at.searles.fractview.R;
import at.searles.meelan.compiler.Ast;

/**
 * Groups are source code
 * Parameters are children.
 */
public class AssetsListAdapter extends RecyclerView.Adapter {

    private static final int SOURCE_TYPE = 0;
    private static final int PARAMETER_TYPE = 1;

    private static final SourceEntry sourceEntries[] = new SourceEntry[] {
            // sources are in the "sources/" folder
            // icons in the "icons/" folder
            new SourceEntry("Default", "Default", "default.png"),
            new SourceEntry("Branching", "Branching", "branching.png"),
            new SourceEntry("ComplexFn", "ComplexFn", "default.png"),
            new SourceEntry("Pendulum", "Pendulum", "default.png"),
            new SourceEntry("Pendulum3", "Pendulum3", "default.png"),
            new SourceEntry("OrbitTrap", "OrbitTrap", "default.png"),
            new SourceEntry("MinMaxOrbitTrap", "MinMaxOrbitTrap", "default.png"),
            new SourceEntry("Lyapunov", "Lyapunov", "default.png"),
    };


    private AssetManager assetManager;
    private List<Source> groups;
    private Map<String, List<Param>> groupCache; // key is group filename
    private int openGroupPosition;
    private List<Param> openGroup;


    AssetsListAdapter(Context context, FractalData current) {
        this.assetManager = context.getAssets();
        initAssets();
    }

    private void initAssets() {
        // First, add current, then add assets
        groups = new ArrayList<>();
        groupCache = new HashMap<>();
        openGroupPosition = -1;

        groups.add(new Source("Current", "Current value", "TODO", null));

        for(SourceEntry entry : sourceEntries) {
            groups.add(createSource(entry));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(openGroup == null || position <= openGroupPosition || position >= openGroupPosition + 1 + openGroup.size()) {
            return SOURCE_TYPE;
        } else {
            return PARAMETER_TYPE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == SOURCE_TYPE) {
            View view = ((Activity) parent.getContext()).getLayoutInflater().inflate(R.layout.list_entry_with_icon, parent, false);
            return new SourceViewHolder(view, this);
        }

        // Parameter
        View view = ((Activity) parent.getContext()).getLayoutInflater().inflate(R.layout.list_entry_with_icon, parent, false);
        return new ParametersViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Source sourceEntry = getGroupEntryAt(position);

        if(sourceEntry != null) {
            bindSourceView((SourceViewHolder) holder, sourceEntry);

            return;
        }

        Param parametersEntry = getChildEntryAt(position);
        bindParamView((ParametersViewHolder) holder, parametersEntry);
        // it is a parameter
    }

    private void bindSourceView(SourceViewHolder holder, Source sourceEntry) {
        holder.titleView.setText(sourceEntry.title);
        holder.descriptionView.setText(sourceEntry.description);

        // null for icon is fine.
        holder.iconView.setImageBitmap(sourceEntry.icon);
    }

    private void bindParamView(ParametersViewHolder holder, Param parametersEntry) {
        holder.titleView.setText(parametersEntry.description);
        holder.iconView.setImageBitmap(parametersEntry.icon);
    }

    @Override
    public int getItemCount() {
        return groups.size() + (openGroup != null ? openGroup.size() : 0);
    }

    void toggleGroupAt(int position) {
        Source source = getGroupEntryAt(position);

        if(openGroup != null) {
            boolean sameGroup = position == openGroupPosition;
            int removedStart = openGroupPosition + 1;
            int removedCount = openGroup.size();

            openGroup = null;
            openGroupPosition = -1;

            notifyItemRangeRemoved(removedStart, removedCount);

            if(sameGroup) {
                return;
            }

            if(removedStart < position) {
                // update index
                position -= removedCount;
            }
        }

        // open group at position.

        this.openGroupPosition = position;
        this.openGroup = groupCache.computeIfAbsent(source.title, k -> fetch(source)); // FIXME pass source along.

        notifyItemRangeInserted(openGroupPosition + 1, openGroup.size());
    }

    void selectChildAt(int position) {
        // select child --> exit and return fractal

    }

    void showContextAt(int position) {
        // Long click on child
    }

    private Source createSource(SourceEntry entry) {
        String sourceCode = readSourcecode(entry.sourceFilename);
        Bitmap icon = entry.iconFilename != null ? readIcon(entry.iconFilename) : null;

        return new Source(entry.sourceFilename, entry.description, sourceCode, icon); // FIXME rename
    }

    private List<Param> fetch(Source parent) {
        // fetch fresh.
        List<Param> list = new ArrayList<>();

        list.add(new Param("Default", "Default values of this source", new Parameters(), null));

        // FIXME add others

        return list;
    }

    /**
     * returns null if it does not exist
     */
    Param getChildEntryAt(int position) {
        if(openGroupPosition == -1) {
            return null;
        }

        int childPosition = position - openGroupPosition - 1;

        if(childPosition < 0 || openGroup.size() <= childPosition) {
            return null;
        }

        return openGroup.get(childPosition);
    }

    /**
     * returns null if it does not exist
     */
    Source getGroupEntryAt(int position) {
        if(openGroupPosition == -1 || position <= openGroupPosition) {
            return groups.get(position);
        }

        int childPosition = position - openGroupPosition - 1;

        if(childPosition < openGroup.size()) {
            return null;
        }

        return groups.get(position - openGroup.size());
    }

    /**
     * Try to read content of assets-folder
     * @param title The filename to be read (+ .fv extension)
     * @return The content of the file as a string, null in case of an error
     */
    private String readSourcecode(String title) {
        try(InputStream is = assetManager.open(String.format("sources/%s.fv", title))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Reads an icon from assets
     * @param iconFilename Filename of the icon.
     * @return null if there is no such file. The error message is logged
     */
    private Bitmap readIcon(String iconFilename) {
        if(iconFilename == null) return null;

        try(InputStream is = assetManager.open("icons/" + iconFilename)) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    class ItemClickedListener {
        public void onClick(View v, int position) {
        }
    }

    class Source {
        final String title;
        final String description;
        final String source;
        final Bitmap icon;
        Ast ast;

        Source(String title, String description, String source, Bitmap icon) {
            this.title = title;
            this.description = description;
            this.source = source;
            this.icon = icon;
        }
    }

    class Param {
        final String title;
        final String description;
        final Parameters data;
        final Bitmap icon;
        FractalData fractal;

        Param(String title, String description, Parameters data, Bitmap icon) {
            this.title = title;
            this.description = description;
            this.data = data;
            this.icon = icon;
        }
    }
}
