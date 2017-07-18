package at.searles.fractview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.Map;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalEntry;
import at.searles.fractal.android.BundleAdapter;
import at.searles.meelan.CompileException;

/**
 * This activity fetches a fractal and overrides its data/parameters
 * with a preset value, then it returns the new fractal. Data
 * are always merged here.
 */
public class ParametersListActivity extends Activity {

    static public final String FRACTAL_LABEL = "fractal";

    private Fractal currentFractal;

    private ParameterAdapter adapter;

    // FIXME: First current, then assets, then elements stored in shared preference.
    // FIXME: Show only those, that are compilable with source


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preset_parameters);

        Intent intent = getIntent();
        this.currentFractal = BundleAdapter.bundleToFractal(intent.getBundleExtra(FRACTAL_LABEL));

        initEntries();

        L8istView lv = (ListView) findViewById(R.id.bookmarkListView);



        // parse fractal
        try {
            inFractal.parse();
        } catch (CompileException e) {
            e.printStackTrace();
        }

        final FractalEntryListAdapter adapter = new FractalEntryListAdapter(this);

        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                Fractal outFractal = adapter.getFractal(index);

                Intent data = new Intent();

                data.putExtra(FRACTAL_LABEL, BundleAdapter.fractalToBundle(outFractal));
                setResult(1, data);
                finish();
            }
        });

        Button closeButton = (Button) findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // end this activity.
                ParametersListActivity.this.finish();
            }
        });
    }

    private void initEntries() {
        // Step 1: Parse sourceCode of currentFractal (this one will remain unchanged)
        currentFractal.parse();

        /*
         * Entry 0: Current.
         * Entry 1-n: Presets. Options allow to edit. If edited, then 'current' is set to '[name] (modified)'
         * Entry n+1-m: Parameter sets in preferences.
         * Since not all parameter sets will be useful, they are filtered out. For this purpose,
         * the inFractal's source code is parsed and it is checked whether for all parameters in
         * the current parameter map there exists one corresponding entry in the default data.
         */
    }

    private class ParameterSetAdapter extends FractalEntryListAdapter {
        @Override
        public Fractal getFractal(int index) {
            Map<String, Fractal.Parameter> parameterMap;

            FractalEntry entry;

            entry = entry.parameterMap = Commons.merge(parameterMap, inFractal.parameterMap());
            new Fractal( inFractal.sourceCode(), parameterMap);
        }

        @Override
        public String getTitle(int position) {
        }

        @Override
        public Bitmap getIcon(int position) {
        }

        @Override
        public String getDescription(int position) {
        }

        @Override
        public void showOptions(int position) {
            // Edit

            // Rename/Delete if comes from shared preferences

            // Copy to Clipboard
        }

        @Override
        public int getCount() {
        }
    }
}
