package com.example.pocketmenu.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketmenu.data.model.Ingredient;
import com.example.pocketmenu.data.model.Menu;
import com.example.pocketmenu.data.model.Product;
import com.example.pocketmenu.data.model.Recipe;
import com.example.pocketmenu.data.model.ShoppingListItem;
import com.example.pocketmenu.data.model.auxiliar.WeeklyShoppingList;
import com.example.pocketmenu.data.repository.ProductRepository;
import com.example.pocketmenu.data.repository.RecipeRepository;
import com.example.pocketmenu.data.repository.ShoppingListRepository;
import com.example.pocketmenu.utils.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ShoppingListViewModel extends ViewModel {

    private final ShoppingListRepository shoppingListRepository;
    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;

    private final MutableLiveData<List<WeeklyShoppingList>> monthlyShoppingLists = new MutableLiveData<>();
    private final MutableLiveData<List<Product>> productSuggestions = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private String activeStoreFilter = null;
    private String activeCategoryFilter = null;
    private boolean weekViewMode = false;
    // Canonical month payload before filters / single-week masking
    private List<WeeklyShoppingList> unfilteredLists = new ArrayList<>();

    // Constructor
    public ShoppingListViewModel() {
        shoppingListRepository = new ShoppingListRepository();
        productRepository = new ProductRepository();
        recipeRepository = new RecipeRepository();
    }

    // LiveData getters
    public LiveData<List<WeeklyShoppingList>> getMonthlyShoppingLists() { return monthlyShoppingLists; }
    public LiveData<List<Product>> getProductSuggestions() { return productSuggestions; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    // Raw grouping for dialogs that ignore active filters (e.g. pickers); see ShoppingListFragment
    public List<WeeklyShoppingList> getUnfilteredLists() { return unfilteredLists; }

    // Drops stale ISO weeks locally, regenerates ingredient rollups per week id in-range
    public void loadCurrentMonth() {
        shoppingListRepository.deletePastWeeks(
                new ShoppingListRepository.ShoppingListCallback() {
                    @Override
                    public void onSuccess() {
                        // Format: YYYY-Wxx
                        List<String> weekIds = getWeekIdsForCurrentMonth();
                        if (weekIds.isEmpty()) {
                            unfilteredLists = new ArrayList<>();
                            monthlyShoppingLists.postValue(new ArrayList<>());
                            return;
                        }
                        regenerateAllWeeks(weekIds);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue("Error limpiando listas antiguas: " + e.getMessage());
                    }
                });
    }

    private void regenerateAllWeeks(List<String> weekIds) {
        AtomicInteger pending = new AtomicInteger(weekIds.size());

        for (String weekId : weekIds) {

            Date monday = ShoppingListRepository.getMondayFromWeekId(weekId);
            Date sunday = DateUtils.getSunday(monday);
            // Delete all items
            shoppingListRepository.deleteRecipeItemsByWeekId(weekId,
                    new ShoppingListRepository.ShoppingListCallback() {
                        @Override
                        public void onSuccess() {
                            // Retrieves all menus within the date range
                            shoppingListRepository.getMainMenusByDateRange(monday, sunday,
                                    new ShoppingListRepository.OnMenusLoaded() {
                                        @Override
                                        public void onLoaded(List<Menu> menus) {
                                            if (menus.isEmpty()) {
                                                checkAllWeeksDone(pending, weekIds);
                                            } else {
                                                // Ingredients extraction
                                                fetchAndSaveIngredients(menus, weekId, () -> checkAllWeeksDone(pending, weekIds));
                                            }
                                        }
                                        @Override
                                        public void onFailure(Exception e) {
                                            errorMessage.postValue("Error leyendo menús: " + e.getMessage());
                                            checkAllWeeksDone(pending, weekIds);
                                        }
                                    });
                        }
                        @Override
                        public void onFailure(Exception e) {
                            errorMessage.postValue("Error regenerando semana: " + e.getMessage());
                            checkAllWeeksDone(pending, weekIds);
                        }
                    });
        }
    }

    // Loads batched ShoppingList rows after every regeneration completes
    private void checkAllWeeksDone(AtomicInteger pending, List<String> weekIds) {
        if (pending.decrementAndGet() == 0) {
            shoppingListRepository.getItemsByWeekIds(weekIds,
                    new ShoppingListRepository.OnItemsLoaded() {
                        @Override
                        public void onLoaded(List<ShoppingListItem> items) {
                            unfilteredLists = groupItemsByWeek(items, weekIds);
                            applyCurrentView();
                        }
                        @Override
                        public void onFailure(Exception e) {
                            errorMessage.postValue("Error cargando lista: " + e.getMessage());
                        }
                    });
        }
    }

    // Fetches each recipe asynchronously, merges duplicate ingredient keys and persists the data
    private void fetchAndSaveIngredients(List<Menu> menus, String weekId, Runnable onDone) {
        AtomicInteger pending = new AtomicInteger(menus.size());
        Map<String, Double> quantityMap = new HashMap<>(); // ingredient||unit -> quantity
        Map<String, ShoppingListItem> itemMap = new LinkedHashMap<>(); // ingredient||unit -> ShoppingListItem

        for (Menu menu : menus) {
            recipeRepository.getRecipeById(menu.getRecipeId(),
                    new RecipeRepository.OnRecipeFound() {
                        @Override
                        public void onFound(Recipe recipe) {
                            if (recipe.getIngredients() != null) {
                                synchronized (quantityMap) {
                                    for (Ingredient ingredient : recipe.getIngredients()) {
                                        String key = ingredient.getName()
                                                .toLowerCase().trim()
                                                + "||"
                                                + (ingredient.getUnit() != null
                                                ? ingredient.getUnit().toLowerCase().trim()
                                                : "");
                                        quantityMap.merge(key,
                                                ingredient.getQuantity(), Double::sum); // Add quantity for same key values
                                        if (!itemMap.containsKey(key)) {
                                            itemMap.put(key, new ShoppingListItem(
                                                    null, weekId,
                                                    ingredient.getName(),
                                                    ingredient.getQuantity(),
                                                    ingredient.getUnit(),
                                                    ingredient.getCategory(),
                                                    ingredient.getStore(),
                                                    recipe.getId()
                                            ));
                                        }
                                    }
                                }
                            }
                            checkAndSave(pending, quantityMap, itemMap, onDone);
                        }
                        @Override
                        public void onNotFound() {
                            checkAndSave(pending, quantityMap, itemMap, onDone);
                        }
                        @Override
                        public void onFailure(Exception e) {
                            checkAndSave(pending, quantityMap, itemMap, onDone);
                        }
                    });
        }
    }

    // Flattens aggregated map quantities into ShoppingListItem
    private void checkAndSave(AtomicInteger pending,
                              Map<String, Double> quantityMap,
                              Map<String, ShoppingListItem> itemMap,
                              Runnable onDone) {
        if (pending.decrementAndGet() == 0) {
            List<ShoppingListItem> finalItems = new ArrayList<>();
            for (Map.Entry<String, ShoppingListItem> entry : itemMap.entrySet()) {
                // Adds the total quantity for each ingredient
                entry.getValue().setQuantity(quantityMap.get(entry.getKey()));
                finalItems.add(entry.getValue());
            }
            shoppingListRepository.addItems(finalItems,
                    new ShoppingListRepository.ShoppingListCallback() {
                        @Override public void onSuccess() { onDone.run(); }
                        @Override public void onFailure(Exception e) {
                            errorMessage.postValue("Error guardando lista: " + e.getMessage());
                            onDone.run();
                        }
                    });
        }
    }

    // True = this week; false = entire month
    public void setWeekViewMode(boolean weekOnly) {
        this.weekViewMode = weekOnly;
        applyCurrentView();
    }

    // Decides the type of list to display
    private void applyCurrentView() {
        // List before filters
        List<WeeklyShoppingList> base;

        if (weekViewMode) {
            String currentWeekId = ShoppingListRepository.getWeekId(new Date());
            base = new ArrayList<>();
            for (WeeklyShoppingList week : unfilteredLists) {
                if (week.getWeekId().equals(currentWeekId)) {
                    base.add(week);
                    break;
                }
            }
        } else {
            base = unfilteredLists;
        }
        // Shortcut if no filters
        if (activeStoreFilter == null && activeCategoryFilter == null) {
            monthlyShoppingLists.setValue(new ArrayList<>(base));
            return;
        }
        // If filters are active, apply them
        List<WeeklyShoppingList> filtered = new ArrayList<>();
        for (WeeklyShoppingList week : base) {
            List<ShoppingListItem> filteredItems = new ArrayList<>();
            for (ShoppingListItem item : week.getItems()) {
                boolean matchesStore = activeStoreFilter == null || activeStoreFilter.equalsIgnoreCase(item.getStore());
                boolean matchesCategory = activeCategoryFilter == null || activeCategoryFilter.equalsIgnoreCase(item.getCategory());
                if (matchesStore && matchesCategory) filteredItems.add(item);
            }
            filtered.add(new WeeklyShoppingList(
                    week.getWeekId(), week.getMonday(),
                    filteredItems.isEmpty() ? null : filteredItems));
        }
        monthlyShoppingLists.setValue(filtered);
    }

    // Category/store chip filters mutate internal state then call applyCurrentView
    public void setStoreFilter(String store) {
        this.activeStoreFilter = store;
        applyCurrentView();
    }

    public void setCategoryFilter(String category) {
        this.activeCategoryFilter = category;
        applyCurrentView();
    }

    public void clearFilters() {
        this.activeStoreFilter = null;
        this.activeCategoryFilter = null;
        applyCurrentView();
    }

    // Persists checked flag flip (local object already toggled beforehand)
    public void toggleItemChecked(ShoppingListItem item) {
        // Inverts value
        item.setChecked(!item.isChecked());
        shoppingListRepository.updateItem(item,
                new ShoppingListRepository.ShoppingListCallback() {
                    @Override
                    public void onSuccess() {
                        List<WeeklyShoppingList> current = monthlyShoppingLists.getValue();
                        if (current != null) {
                            monthlyShoppingLists.setValue(current);
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue("Error actualizando item: " + e.getMessage());
                    }
                });
    }

    // Add extra product
    public void addExtraItem(ShoppingListItem item, boolean isNewProduct, Product product) {
        if (isNewProduct) {
            productRepository.addProduct(product,
                    new ProductRepository.OnProductAdded() {
                        @Override
                        public void onSuccess(String productId) { saveExtraItem(item); }
                        @Override
                        public void onFailure(Exception e) {
                            errorMessage.postValue("Error guardando producto: " + e.getMessage());
                        }
                    });
        } else {
            saveExtraItem(item);
        }
    }

    // Persist extra product
    private void saveExtraItem(ShoppingListItem item) {
        shoppingListRepository.addItem(item,
                new ShoppingListRepository.OnItemAdded() {
                    @Override
                    public void onSuccess(String itemId) {
                        item.setId(itemId);
                        for (WeeklyShoppingList week : unfilteredLists) {
                            if (week.getWeekId().equals(item.getWeekId())) {
                                week.getItems().add(item);
                                break;
                            }
                        }
                        applyCurrentView();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue("Error añadiendo producto: " + e.getMessage());
                    }
                });
    }

    // Removes extra product
    public void deleteExtraItem(String itemId) {
        shoppingListRepository.deleteItem(itemId,
                new ShoppingListRepository.ShoppingListCallback() {
                    @Override
                    public void onSuccess() {
                        for (WeeklyShoppingList week : unfilteredLists) {
                            week.getItems().removeIf(i -> itemId.equals(i.getId()));
                        }
                        applyCurrentView();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue("Error eliminando producto: " + e.getMessage());
                    }
                });
    }

    // Product autocomplete
    public void searchProductSuggestions(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            productSuggestions.setValue(new ArrayList<>());
            return;
        }
        productRepository.searchProductsByName(prefix.trim(),
                new ProductRepository.OnProductsLoaded() {
                    @Override
                    public void onLoaded(List<Product> products) {
                        productSuggestions.postValue(products);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        productSuggestions.postValue(new ArrayList<>());
                    }
                });
    }

    // Stable week ordering
    private List<WeeklyShoppingList> groupItemsByWeek(List<ShoppingListItem> items,
                                                      List<String> weekIds) {
        Map<String, List<ShoppingListItem>> byWeek = new LinkedHashMap<>(); // weekId -> items
        // Preinitialize the map with ordered and empty lists
        for (String weekId : weekIds) byWeek.put(weekId, new ArrayList<>());
        for (ShoppingListItem item : items) {
            if (byWeek.containsKey(item.getWeekId()))
                byWeek.get(item.getWeekId()).add(item);
        }
        List<WeeklyShoppingList> result = new ArrayList<>();
        for (Map.Entry<String, List<ShoppingListItem>> entry : byWeek.entrySet()) {
            // Transforms weekId into a date
            Date monday = ShoppingListRepository.getMondayFromWeekId(entry.getKey());
            // Creates the object and adds it to the result
            result.add(new WeeklyShoppingList(entry.getKey(), monday, entry.getValue()));
        }
        return result;
    }
    // Returns a list of ISO weeks for the actual weeks and the three following
    private List<String> getWeekIdsForCurrentMonth() {
        return DateUtils.getCurrentAndNextWeekIds(4);
    }
}