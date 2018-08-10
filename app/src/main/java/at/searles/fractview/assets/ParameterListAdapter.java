package at.searles.fractview.assets;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

import at.searles.fractal.FractalExternData;
import at.searles.fractal.FractviewInstructionSet;
import at.searles.fractal.ParserInstance;
import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.Parameters;
import at.searles.fractal.entries.ParametersEntry;
import at.searles.meelan.compiler.Ast;

class ParameterListAdapter extends BaseAdapter {

    private final Activity activity;
    private ArrayList<ParametersEntry> allEntries;
    private ArrayList<ParametersEntry> activeEntries;

    ParameterListAdapter(Activity activity, Parameters parameters) {
        this.activity = activity;
        initEntries(parameters);
    }

    private void initEntries(Parameters parameters) {
        this.allEntries = new ArrayList<>();
        this.activeEntries = new ArrayList<>();

        ParametersEntry empty = new ParametersEntry("Default", "Default values", new Parameters());
        ParametersEntry current = new ParametersEntry("Current", "Current parameters", parameters);

        this.allEntries.add(empty);
        this.allEntries.add(current);

        // TODO: Read
//
//        for (ParameterEntry entry : ParameterEntry.entries(context.getAssets())) {
//            // Only those that work out of the box
//            if (inFractal.parameters().containsAll(entry.parameters.keySet())) {
//                entries.add(entry);
//            }
//        }
    }

    public void updateEntries(String sourceCode) {
        this.activeEntries.clear();

        Ast ast = ParserInstance.get().parseSource(sourceCode);

        FractalExternData data = FractalExternData.empty();

        ast.compile(FractviewInstructionSet.get(), data);

        for(ParametersEntry entry : allEntries) {
            boolean isMatch = true;

            // if all in entry are in 'data'
            for(ParameterKey key : entry.data) {
                FractalExternData.Entry dataEntry = data.entry(key.id);

                if(dataEntry == null || !key.equals(dataEntry.key)) {
                    isMatch = false;
                    break;
                }
            }

            if(isMatch) {
                activeEntries.add(entry);
            }
        }
    }

    @Override
    public int getCount() {
        return activeEntries.size();
    }

    @Override
    public ParametersEntry getItem(int position) {
        return activeEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO
        return null;
    }
}
