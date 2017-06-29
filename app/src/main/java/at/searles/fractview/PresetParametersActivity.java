package at.searles.fractview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import at.searles.fractview.fractal.Fractal;
import at.searles.math.Scale;
import at.searles.meelan.CompileException;

/**
 * This activity is used to load parameter sets from
 * the Presets class. It receives a program and shows only
 * presets that fit the program (maybe receiving a fractal
 * is better). Via a checkbox it can be decided whether
 * the parameters should be merged or reset.
 */

public class PresetParametersActivity extends Activity {

    private static final FractalLabel USE_DEFAULTS = new FractalLabel() {
        @Override
        public String title() {
            return "Use default values";
        }

        @Override
        public Bitmap icon() {
            return null;
        }

        @Override
        public String description() {
            return "Uses the current defaults (may merge current parameters)";
        }
    };

    private static final FractalLabel MERGE_DEFAULTS = new FractalLabel() {
        @Override
        public String title() {
            return "Use current parameters";
        }

        @Override
        public Bitmap icon() {
            return null;
        }

        @Override
        public String description() {
            return "Uses the current parameters, merged with defaults";
        }
    };

    Fractal inFractal;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preset_parameters);

        Intent intent = getIntent();
        this.inFractal = intent.getParcelableExtra("fractal"); // FIXME test whether this is preserved on termination

        CheckBox mergeCheckBox = (CheckBox) findViewById(R.id.mergeCheckBox);

        ListView lv = (ListView) findViewById(R.id.bookmarkListView);

        // fetch assets
        List<AssetsHelper.ParametersAsset> assets = AssetsHelper.parameterEntries(getAssets());

        // entries contain a first empty dummy
        List<FractalLabel> entries = new ArrayList<>(assets.size() + 2);

        // first, add the 'keep'-entry
        entries.add(USE_DEFAULTS);
        entries.add(MERGE_DEFAULTS);

        // parse fractal
        try {
            inFractal.parse();
        } catch (CompileException e) {
            e.printStackTrace();
        }

        // take all parameterIds, and only if a preset
        // is fully contained, it is accepted.
        TreeSet<String> ids = new TreeSet<>();

        for(String id : inFractal.parameters()) {
            ids.add(id);
        }

        // fixme label!
        next_asset: for(AssetsHelper.ParametersAsset entry : AssetsHelper.parameterEntries(getAssets())) {

            for (String id : entry.parameters.keySet()) {
                if (!id.startsWith("__")) {
                    if (!ids.contains(id)) {
                        continue next_asset; // next entry in for loop...
                    }
                }
            }

            // everything is contained
            entries.add(entry);
        }

        final FractalEntryAdapter adapter = new FractalEntryAdapter(this);
        adapter.setData(entries);

        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                Map<String, Fractal.Parameter> parameterMap;
                Scale scale;

                if(index == 0 || index == 1) {
                    // Use defaults, therefore use empty parameters.
                    parameterMap = new HashMap<String, Fractal.Parameter>();
                    // use default
                    scale = AssetsHelper.DEFAULT_SCALE;
                } else {
                    AssetsHelper.ParametersAsset entry = (AssetsHelper.ParametersAsset) entries.get(index);
                    scale = entry.scale == null ? AssetsHelper.DEFAULT_SCALE : entry.scale;
                    parameterMap = entry.parameters;
                }

                // use old scale if merge is checked.
                if(mergeCheckBox.isChecked() || index == 1) {
                    scale = inFractal.scale();
                    parameterMap = Commons.merge(parameterMap, inFractal.parameterMap());
                }

                Fractal retVal = new Fractal(scale, inFractal.sourceCode(), parameterMap);

                returnFractal(retVal);
            }
        });

        Button closeButton = (Button) findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // end this activity.
                PresetParametersActivity.this.finish();
            }
        });
    }

    void returnFractal(Fractal f) {
        Intent data = new Intent();

        Log.d("PPA", "returned fractal is " + f);
        data.putExtra("fractal", f);
        setResult(1, data);
        finish();
    }
}
