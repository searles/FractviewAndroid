package at.searles.fractview.assets;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import at.searles.fractview.R;

class SourceViewHolder extends RecyclerView.ViewHolder {

    ImageView iconView;
    TextView titleView;
    TextView descriptionView;

    SourceViewHolder(View view, AssetsListAdapter adapter) {
        super(view);

        titleView = view.findViewById(R.id.titleView);
        descriptionView = view.findViewById(R.id.descriptionView);
        iconView = view.findViewById(R.id.iconView);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.toggleGroupAt(getAdapterPosition());
            }
        });
    }
}
