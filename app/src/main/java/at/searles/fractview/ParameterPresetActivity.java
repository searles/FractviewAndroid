package at.searles.fractview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalEntry;
import at.searles.fractal.android.BundleAdapter;
import at.searles.meelan.CompileException;

/**
 * This activity fetches a fractal and overrides its data/parameters
 * with a preset value, then it returns the new fractal. Data
 * are always merged here.
 */
public class ParameterPresetActivity extends Activity {

    static public final String FRACTAL_LABEL = "fractal";

    private Fractal inFractal;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preset_parameters);

        Intent intent = getIntent();
        this.inFractal = BundleAdapter.bundleToFractal(intent.getBundleExtra(FRACTAL_LABEL));

        ListView lv = (ListView) findViewById(R.id.bookmarkListView);

        // fetch assets
        List<AssetsHelper.ParametersAsset> assets = AssetsHelper.parameterEntries(getAssets());

        // entries contain a first empty dummy
        List<FractalEntry> entries = new ArrayList<>(assets.size() + 1);

        // first, add the 'keep'-entry
        entries.add(new FractalEntry("Current", null, inFractal, ""));

        // parse fractal
        try {
            inFractal.parse();
        } catch (CompileException e) {
            e.printStackTrace();
        }

        // take all parameterIds, and only if a preset
        // is fully contained, it is accepted.
        HashSet<Fractal.Parameter> ids = new TreeSet<>();

        for(String id : inFractal.parameters()) {
            ids.add(id);
        }

        final FractalEntryListAdapter adapter = new FractalEntryListAdapter(this);

        // find all assets whose parameters matches the program's
        for(FractalEntry entry : AssetsHelper.parameterEntries(getAssets())) {
            if(!inFractal.compatibleParameters(entry.fractal())) {
                adapter.add(entry);
            }
        }

        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                Map<String, Fractal.Parameter> parameterMap;

                FractalEntry entry;

                if(index == 0 || index == 1) {
                    // Use defaults, therefore use empty parameters.
                    parameterMap = new HashMap<>();
                } else {
                    FractalEntry entry = entries.get(index);
                }

                entry = entry.parameterMap = Commons.merge(parameterMap, inFractal.parameterMap());

                Fractal retVal = new Fractal( inFractal.sourceCode(), parameterMap);

                returnFractal(retVal);
            }
        });

        Button closeButton = (Button) findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // end this activity.
                ParameterPresetActivity.this.finish();
            }
        });
    }

    private void returnFractal(Fractal f) {
        Intent data = new Intent();

        data.putExtra(FRACTAL_LABEL, BundleAdapter.fractalToBundle(f));
        setResult(1, data);
        finish();
    }
}
