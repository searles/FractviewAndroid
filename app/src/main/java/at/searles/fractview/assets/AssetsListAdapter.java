package at.searles.fractview.assets;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.searles.fractal.data.FractalData;
import at.searles.fractview.R;

/**
 * Groups are source code
 * Parameters are children.
 */
public class AssetsListAdapter extends RecyclerView.Adapter {

    private static final int SOURCE_TYPE = 0;
    private static final int PARAMETER_TYPE = 1;

    private static final String SOURCE_ENTRY_FILE = "sources.json";
    private static final String PARAMETERS_ENTRY_FILE = "parameters.json";

    private SelectAssetActivity parent;

    /**
     * All sources that are defined in assets
     */
    private SourceAsset[] sourceEntries;

    /**
     * All parameters that are defined in assets.
     */
    private ParameterAsset[] parameterAssets;

    private ArrayList<SourceAsset> groups;
    private Map<String, List<ParameterAsset>> groupCache; // key is group filename
    private int openGroupPosition;
    private List<ParameterAsset> openGroup;

    AssetsListAdapter(SelectAssetActivity parent, FractalData current) {
        this.parent = parent;
        initAssets();

        // TODO do sth with current.
    }

    private void initSourceEntries() {
        if(sourceEntries == null) {
            // load from json
            Gson gson = new Gson();

            try(JsonReader reader = new JsonReader(new InputStreamReader(parent.getAssets().open(SOURCE_ENTRY_FILE)))) {
                sourceEntries = gson.fromJson(reader, SourceAsset[].class); // contains the whole reviews list
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initParameterEntries() {
        // TODO read json
    }

    private void initAssets() {
        // read json data
        initSourceEntries();
        initParameterEntries();

        // First, add current, then add assets
        groups = new ArrayList<>();
        groupCache = new HashMap<>();
        openGroupPosition = -1;

        groups.add(new SourceAsset("Current", "Current value", "TODO")); // todo current source

        groups.addAll(Arrays.asList(sourceEntries));
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
        SourceAsset sourceEntry = getGroupEntryAt(position);

        if(sourceEntry != null) {
            bindSourceView((SourceViewHolder) holder, sourceEntry);

            return;
        }

        ParameterAsset parametersEntry = getChildEntryAt(position);
        bindParamView((ParametersViewHolder) holder, parametersEntry);
        // it is a parameter
    }

    private void bindSourceView(SourceViewHolder holder, SourceAsset sourceEntry) {
        holder.titleView.setText(sourceEntry.title);
        holder.descriptionView.setText(sourceEntry.description);

        // null for icon is fine.
        holder.iconView.setImageBitmap(sourceEntry.icon(parent.getAssets()));
    }

    private void bindParamView(ParametersViewHolder holder, ParameterAsset parametersEntry) {
        holder.titleView.setText(parametersEntry.description);
        holder.iconView.setImageBitmap(parametersEntry.icon(parent.getAssets()));
    }

    @Override
    public int getItemCount() {
        return groups.size() + (openGroup != null ? openGroup.size() : 0);
    }

    void toggleGroupAt(int position) {
        SourceAsset source = getGroupEntryAt(position);

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
        this.openGroup = groupCache.computeIfAbsent(source.title, k -> fetchParametersForParent(source)); // FIXME pass source along.

        notifyItemRangeInserted(openGroupPosition + 1, openGroup.size());
    }

    void selectChildAt(int position) {
        String source = getGroupEntryAt(openGroupPosition).source(parent.getAssets());
        Map<String, Object> parameters = getChildEntryAt(position).data;

        FractalData.Builder builder = new FractalData.Builder().setSource(source);

        parameters.forEach(builder::addParameter);

        parent.returnFractal(builder.commit());
     }

    void showContextAt(int position) {
        // TODO Long click on child
    }

    private List<ParameterAsset> fetchParametersForParent(SourceAsset parent) {
        // fetch fresh.
        List<ParameterAsset> list = new ArrayList<>();

        list.add(new ParameterAsset("Default", "Default values of this source", Collections.emptyMap()));

        // FIXME add others

        return list;
    }

    /**
     * returns null if it does not exist
     */
    ParameterAsset getChildEntryAt(int position) {
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
    SourceAsset getGroupEntryAt(int position) {
        if(openGroupPosition == -1 || position <= openGroupPosition) {
            return groups.get(position);
        }

        int childPosition = position - openGroupPosition - 1;

        if(childPosition < openGroup.size()) {
            return null;
        }

        return groups.get(position - openGroup.size());
    }
}
