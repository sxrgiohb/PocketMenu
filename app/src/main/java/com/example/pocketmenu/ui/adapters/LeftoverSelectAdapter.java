package com.example.pocketmenu.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.auxiliar.LeftoverWithRecipe;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LeftoverSelectAdapter extends RecyclerView.Adapter<LeftoverSelectAdapter.ViewHolder> {

    public interface OnLeftoverSelectedListener {
        void onLeftoverSelected(LeftoverWithRecipe item);
    }

    private List<LeftoverWithRecipe> items = new ArrayList<>();
    private final OnLeftoverSelectedListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

    // Constructor
    public LeftoverSelectAdapter(OnLeftoverSelectedListener listener) {
        this.listener = listener;
    }

    // Data from MenuViewModel after loadValidLeftovers
    public void setItems(List<LeftoverWithRecipe> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leftover_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(items.get(position), sdf, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textInfo;

        ViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_leftover_select_name);
            textInfo = itemView.findViewById(R.id.text_leftover_select_info);
        }

        void bind(LeftoverWithRecipe item, SimpleDateFormat sdf, OnLeftoverSelectedListener listener) {
            textName.setText(item.getRecipe().getName());
            String dateStr = item.getLeftover().getFirstAssignedDate() != null ? sdf.format(item.getLeftover().getFirstAssignedDate()) : "Sin fecha asignada";
            textInfo.setText(item.getLeftover().getRemainingPortions() + " ración(es) · del " + dateStr);
            itemView.setOnClickListener(v -> listener.onLeftoverSelected(item));
        }
    }
}