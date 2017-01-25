package at.searles.fractview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

import at.searles.fractview.fractal.Fractal;

/**
 * This activity is used to load parameter sets from
 * the Presets class. It receives a program and shows only
 * presets that fit the program (maybe receiving a fractal
 * is better). Via a checkbox it can be decided whether
 * the parameters should be merged or reset.
 */

public class PresetParametersActivity extends Activity {

    Fractal inFractal;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorite_layout);

        Intent intent = getIntent();
        this.inFractal = intent.getParcelableExtra("fractal"); // FIXME test whether this is preserved on termination

        // and since it is sorted, use it to write label-map.
        ListView lv = (ListView) findViewById(R.id.bookmarkListView);

        List<AssetsHelper.ParametersAsset> entries = AssetsHelper.parameterEntries(getAssets());

        final FavoritesAdapter adapter = new FavoritesAdapter(this, entries);

        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                AssetsHelper.ParametersAsset entry = entries.get(index);

                // Fetch checkbox status to know whether we should merge or keep
                // original.

                //returnFractal(new Fractal(inFractal);
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
        data.putExtra("fractal", f);
        setResult(1, data);
        finish();
    }
}
