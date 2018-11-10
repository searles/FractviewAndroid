package at.searles.fractview.assets;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import at.searles.fractview.R;

class ParametersViewHolder extends RecyclerView.ViewHolder {
    ImageView iconView;
    TextView titleView;

    ParametersViewHolder(View view, AssetsListAdapter adapter) {
        super(view);

        titleView = view.findViewById(R.id.titleView);
        iconView = view.findViewById(R.id.iconView);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.selectChildAt(getAdapterPosition());
            }
        });

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                adapter.showContextAt(getAdapterPosition());
                return true;
            }
        });
    }
}
