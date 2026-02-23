package com.example.pocketmenu.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.Leftover;
import com.example.pocketmenu.data.model.Recipe;
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

    public LeftoverSelectAdapter(OnLeftoverSelectedListener listener) {
        this.listener = listener;
    }

    public void setItems(List<LeftoverWithRecipe> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leftover_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeftoverWithRecipe item = items.get(position);
        holder.textName.setText(item.getRecipe().getName());

        String dateStr = item.getLeftover().getFirstAssignedDate() != null
                ? sdf.format(item.getLeftover().getFirstAssignedDate()) : "?";
        holder.textInfo.setText(item.getLeftover().getRemainingPortions()
                + " ración(es) · del " + dateStr);

        holder.itemView.setOnClickListener(v -> listener.onLeftoverSelected(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_leftover_select_name);
            textInfo = itemView.findViewById(R.id.text_leftover_select_info);
        }
    }
}