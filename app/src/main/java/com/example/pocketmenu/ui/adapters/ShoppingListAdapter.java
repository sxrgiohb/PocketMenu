package com.example.pocketmenu.ui.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.ShoppingListItem;
import com.example.pocketmenu.data.model.auxiliar.WeeklyShoppingList;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShoppingListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_WEEK_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface OnShoppingListActionListener {
        void onItemChecked(ShoppingListItem item);
        void onExtraItemDeleted(ShoppingListItem item);
    }

    private static class Row {
        final int type;
        final String weekHeader;
        final ShoppingListItem item;

        Row(String weekHeader) {
            this.type = TYPE_WEEK_HEADER;
            this.weekHeader = weekHeader;
            this.item = null;
        }

        Row(ShoppingListItem item) {
            this.type = TYPE_ITEM;
            this.weekHeader = null;
            this.item = item;
        }
    }

    private List<Row> rows = new ArrayList<>();
    private final OnShoppingListActionListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Constructor
    public ShoppingListAdapter(OnShoppingListActionListener listener) {
        this.listener = listener;
    }

    public void setWeeks(List<WeeklyShoppingList> weeks) {
        rows = new ArrayList<>();
        if (weeks == null) {
            notifyDataSetChanged();
            return;
        }
        for (WeeklyShoppingList week : weeks) {
            // Show section headers only when multiple weeks are rendered together
            if (weeks.size() > 1) {
                String dateStr = week.getMonday() != null ? sdf.format(week.getMonday()) : week.getWeekId();
                rows.add(new Row("Semana del " + dateStr));
            }
            // Keep an empty placeholder row so the week still occupies visual space
            if (week.getItems().isEmpty()) {
                rows.add(new Row((ShoppingListItem) null));
            } else {
                for (ShoppingListItem item : week.getItems()) {
                    rows.add(new Row(item));
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_WEEK_HEADER) {
            View v = inflater.inflate(R.layout.item_shopping_week_header, parent, false);
            return new WeekHeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_shopping_list_item, parent, false);
            return new ItemViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        // Check that the holder is of the correct type
        if (holder instanceof WeekHeaderViewHolder) {
            ((WeekHeaderViewHolder) holder).bind(row.weekHeader);
        } else if (holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).bind(row.item);
        }
    }

    class WeekHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textWeekTitle;

        WeekHeaderViewHolder(View itemView) {
            super(itemView);
            textWeekTitle = itemView.findViewById(R.id.text_week_title);
        }

        void bind(String title) {
            textWeekTitle.setText(title);
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkbox;
        private final TextView textName;
        private final TextView textStore;
        private final TextView textQuantity;
        private final TextView textCategory;
        private final MaterialButton buttonDelete;

        ItemViewHolder(View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkbox_item);
            textName = itemView.findViewById(R.id.text_item_name);
            textStore = itemView.findViewById(R.id.text_item_store);
            textCategory = itemView.findViewById(R.id.text_item_category);
            textQuantity = itemView.findViewById(R.id.text_item_quantity);
            buttonDelete = itemView.findViewById(R.id.button_delete_item);
        }

        void bind(ShoppingListItem item) {
            if (item == null) {
                // Placeholder row for weeks without items.
                checkbox.setVisibility(View.GONE);
                textName.setVisibility(View.GONE);
                textStore.setVisibility(View.GONE);
                textCategory.setVisibility(View.GONE);
                textQuantity.setVisibility(View.GONE);
                buttonDelete.setVisibility(View.GONE);
                return;
            }

            textName.setVisibility(View.VISIBLE);
            checkbox.setVisibility(View.VISIBLE);
            textQuantity.setVisibility(View.VISIBLE);

            // Item title follows checked style changes
            textName.setText(item.getName());
            applyCheckedStyle(textName, item.isChecked());

            // ViewModel persists the new checked state
            checkbox.setChecked(item.isChecked());
            checkbox.setOnClickListener(v -> listener.onItemChecked(item));

            // Build quantity label preserving integer formatting when possible
            StringBuilder qty = new StringBuilder();
            if (item.getQuantity() > 0) {
                if (item.getQuantity() == Math.floor(item.getQuantity())) {
                    qty.append((int) item.getQuantity());
                } else {
                    qty.append(item.getQuantity());
                }
            }
            if (item.getUnit() != null && !item.getUnit().isEmpty()) {
                qty.append(" ").append(item.getUnit());
            }
            textQuantity.setText(qty.toString());

            if (item.getStore() != null && !item.getStore().isEmpty()) {
                textStore.setText("Tienda: " + item.getStore());
                textStore.setVisibility(View.VISIBLE);
            } else {
                textStore.setVisibility(View.GONE);
            }

            if (item.getCategory() != null && !item.getCategory().isEmpty()) {
                textCategory.setText("Categoría: " + item.getCategory());
                textCategory.setVisibility(View.VISIBLE);
            } else {
                textCategory.setVisibility(View.GONE);
            }

            if (item.isExtra()) {
                buttonDelete.setVisibility(View.VISIBLE);
                buttonDelete.setOnClickListener(v -> listener.onExtraItemDeleted(item));
            } else {
                buttonDelete.setVisibility(View.GONE);
            }
        }

        private void applyCheckedStyle(TextView textView, boolean checked) {
            if (checked) {
                // Items checked are crossed out
                textView.setPaintFlags(
                        textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textView.setAlpha(0.4f);
            } else {
                // Unchecked items are normal
                textView.setPaintFlags(
                        textView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                textView.setAlpha(1f);
            }
        }
    }
}