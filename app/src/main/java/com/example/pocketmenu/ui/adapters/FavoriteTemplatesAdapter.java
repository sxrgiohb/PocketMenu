package com.example.pocketmenu.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.UnassignedLeftover;
import com.example.pocketmenu.data.model.WeeklyMenuTemplate;
import com.example.pocketmenu.data.model.WeeklyMenuItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoriteTemplatesAdapter extends
        RecyclerView.Adapter<FavoriteTemplatesAdapter.TemplateViewHolder> {

    public interface OnTemplateActionListener {
        void onApplyClicked(WeeklyMenuTemplate template);
        void onDeleteClicked(WeeklyMenuTemplate template);
    }

    private List<WeeklyMenuTemplate> allTemplates = new ArrayList<>();
    private List<WeeklyMenuTemplate> filteredTemplates = new ArrayList<>();
    private Map<String, String> recipeNames = new HashMap<>();
    private String searchText = "";
    private final OnTemplateActionListener listener;

    public FavoriteTemplatesAdapter(OnTemplateActionListener listener) {
        this.listener = listener;
    }

    public void setTemplates(List<WeeklyMenuTemplate> templates,
                             Map<String, String> recipeNames) {
        this.allTemplates = templates != null ? templates : new ArrayList<>();
        this.recipeNames = recipeNames != null ? recipeNames : new HashMap<>();
        applyFilter();
    }

    public void setSearchText(String text) {
        this.searchText = text != null ? text.toLowerCase().trim() : "";
        applyFilter();
    }

    private void applyFilter() {
        filteredTemplates = new ArrayList<>();
        for (WeeklyMenuTemplate t : allTemplates) {
            if (searchText.isEmpty()
                    || t.getName().toLowerCase().contains(searchText)) {
                filteredTemplates.add(t);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_template, parent, false);
        return new TemplateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        holder.bind(filteredTemplates.get(position));
    }

    @Override
    public int getItemCount() {
        return filteredTemplates.size();
    }

    class TemplateViewHolder extends RecyclerView.ViewHolder {

        private final TextView textName;
        private final ImageButton buttonExpand;
        private final ImageButton buttonDelete;
        private final LinearLayout layoutDetail;
        private final TextView textMonday;
        private final TextView textTuesday;
        private final TextView textWednesday;
        private final TextView textThursday;
        private final TextView textFriday;
        private final TextView textSaturday;
        private final TextView textSunday;
        private final TextView textUnassignedWarning;
        private final Button buttonApply;

        private boolean isExpanded = false;

        TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_template_name);
            buttonExpand = itemView.findViewById(R.id.button_expand_template);
            buttonDelete = itemView.findViewById(R.id.button_delete_template);
            layoutDetail = itemView.findViewById(R.id.layout_template_detail);
            textMonday = itemView.findViewById(R.id.text_monday);
            textTuesday = itemView.findViewById(R.id.text_tuesday);
            textWednesday = itemView.findViewById(R.id.text_wednesday);
            textThursday = itemView.findViewById(R.id.text_thursday);
            textFriday = itemView.findViewById(R.id.text_friday);
            textSaturday = itemView.findViewById(R.id.text_saturday);
            textSunday = itemView.findViewById(R.id.text_sunday);
            textUnassignedWarning = itemView.findViewById(R.id.text_unassigned_warning);
            buttonApply = itemView.findViewById(R.id.button_apply_template);
        }

        void bind(WeeklyMenuTemplate template) {
            textName.setText(template.getName());

            isExpanded = false;
            layoutDetail.setVisibility(View.GONE);
            buttonExpand.setImageResource(R.drawable.ic_see_more);

            View.OnClickListener toggleExpand = v -> {
                isExpanded = !isExpanded;
                layoutDetail.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                buttonExpand.setImageResource(
                        isExpanded ? R.drawable.ic_see_less : R.drawable.ic_see_more);
            };
            buttonExpand.setOnClickListener(toggleExpand);
            textName.setOnClickListener(toggleExpand);

            String[] dayLabels = {"Lunes", "Martes", "Miércoles", "Jueves",
                    "Viernes", "Sábado", "Domingo"};
            TextView[] dayViews = {textMonday, textTuesday, textWednesday,
                    textThursday, textFriday, textSaturday, textSunday};

            for (int i = 0; i < 7; i++) {
                dayViews[i].setText(dayLabels[i] + ": —");
            }

            if (template.getItems() != null) {
                for (int day = 1; day <= 7; day++) {
                    StringBuilder sb = new StringBuilder(dayLabels[day - 1] + ": ");
                    boolean hasItems = false;
                    for (WeeklyMenuItem item : template.getItems()) {
                        if (item.getDayOfWeek() == day) {
                            if (hasItems) sb.append("\n").append("   ");
                            if (item.isLeftover()) {
                                String sourceName = recipeNames.containsKey(item.getSourceRecipeId())
                                        ? recipeNames.get(item.getSourceRecipeId())
                                        : "Sobra";
                                sb.append("Sobra de ").append(sourceName);
                            } else {
                                String recipeName = recipeNames.containsKey(item.getRecipeId())
                                        ? recipeNames.get(item.getRecipeId())
                                        : item.getRecipeId();
                                sb.append(recipeName);
                            }
                            hasItems = true;
                        }
                    }
                    if (!hasItems) sb.append("—");
                    dayViews[day - 1].setText(sb.toString());
                }
            }

            List<UnassignedLeftover> unassigned = template.getUnassignedLeftovers();
            if (unassigned != null && !unassigned.isEmpty()) {
                int totalPortions = 0;
                for (UnassignedLeftover u : unassigned) {
                    totalPortions += u.getRemainingPortions();
                }
                textUnassignedWarning.setText("⚠️ " + totalPortions
                        + " ración(es) sin asignar quedarán disponibles para la semana siguiente");
                textUnassignedWarning.setVisibility(View.VISIBLE);
            } else {
                textUnassignedWarning.setVisibility(View.GONE);
            }

            buttonDelete.setOnClickListener(v -> listener.onDeleteClicked(template));
            buttonApply.setOnClickListener(v -> listener.onApplyClicked(template));
        }
    }
}