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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ShoppingListViewModel extends ViewModel {

    private final ShoppingListRepository shoppingListRepository;
    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;

    private final MutableLiveData<List<WeeklyShoppingList>> monthlyShoppingLists
            = new MutableLiveData<>();
    private final MutableLiveData<List<Product>> productSuggestions
            = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private String activeStoreFilter = null;
    private String activeCategoryFilter = null;
    private boolean weekViewMode = false;
    private List<WeeklyShoppingList> unfilteredLists = new ArrayList<>();

    public ShoppingListViewModel() {
        shoppingListRepository = new ShoppingListRepository();
        productRepository = new ProductRepository();
        recipeRepository = new RecipeRepository();
    }

    public LiveData<List<WeeklyShoppingList>> getMonthlyShoppingLists() {
        return monthlyShoppingLists;
    }
    public LiveData<List<Product>> getProductSuggestions() { return productSuggestions; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public List<WeeklyShoppingList> getUnfilteredLists() { return unfilteredLists; }

    // ===========================
    // CARGA Y GENERACIÓN AL ENTRAR AL FRAGMENT
    // ===========================

    public void loadCurrentMonth() {
        isLoading.setValue(true);

        // 1. Limpiar semanas pasadas
        shoppingListRepository.deletePastWeeks(
                new ShoppingListRepository.ShoppingListCallback() {
                    @Override
                    public void onSuccess() {
                        List<String> weekIds = getWeekIdsForCurrentMonth();
                        if (weekIds.isEmpty()) {
                            unfilteredLists = new ArrayList<>();
                            monthlyShoppingLists.postValue(new ArrayList<>());
                            isLoading.postValue(false);
                            return;
                        }
                        // 2. Regenerar ingredientes de recetas para cada semana del mes
                        regenerateAllWeeks(weekIds);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue("Error limpiando listas antiguas: "
                                + e.getMessage());
                        isLoading.postValue(false);
                    }
                });
    }

    // Regenera los items de receta para todas las semanas y luego carga
    private void regenerateAllWeeks(List<String> weekIds) {
        AtomicInteger pending = new AtomicInteger(weekIds.size());

        for (String weekId : weekIds) {
            Date monday = ShoppingListRepository.getMondayFromWeekId(weekId);
            Date sunday = getSunday(monday);

            // Borrar items de receta de esa semana y regenerar desde los menús
            shoppingListRepository.deleteRecipeItemsByWeekId(weekId,
                    new ShoppingListRepository.ShoppingListCallback() {
                        @Override
                        public void onSuccess() {
                            shoppingListRepository.getMainMenusByDateRange(monday, sunday,
                                    new ShoppingListRepository.OnMenusLoaded() {
                                        @Override
                                        public void onLoaded(List<Menu> menus) {
                                            if (menus.isEmpty()) {
                                                checkAllWeeksDone(pending, weekIds);
                                            } else {
                                                fetchAndSaveIngredients(menus, weekId,
                                                        () -> checkAllWeeksDone(pending, weekIds));
                                            }
                                        }
                                        @Override
                                        public void onFailure(Exception e) {
                                            errorMessage.postValue("Error leyendo menús: "
                                                    + e.getMessage());
                                            checkAllWeeksDone(pending, weekIds);
                                        }
                                    });
                        }
                        @Override
                        public void onFailure(Exception e) {
                            errorMessage.postValue("Error regenerando semana: "
                                    + e.getMessage());
                            checkAllWeeksDone(pending, weekIds);
                        }
                    });
        }
    }

    private void checkAllWeeksDone(AtomicInteger pending, List<String> weekIds) {
        if (pending.decrementAndGet() == 0) {
            // Todas las semanas regeneradas, ahora cargar y mostrar
            shoppingListRepository.getItemsByWeekIds(weekIds,
                    new ShoppingListRepository.OnItemsLoaded() {
                        @Override
                        public void onLoaded(List<ShoppingListItem> items) {
                            unfilteredLists = groupItemsByWeek(items, weekIds);
                            applyCurrentView();
                            isLoading.postValue(false);
                        }
                        @Override
                        public void onFailure(Exception e) {
                            errorMessage.postValue("Error cargando lista: " + e.getMessage());
                            isLoading.postValue(false);
                        }
                    });
        }
    }

    private void fetchAndSaveIngredients(List<Menu> menus, String weekId, Runnable onDone) {
        AtomicInteger pending = new AtomicInteger(menus.size());
        Map<String, Double> quantityMap = new HashMap<>();
        Map<String, ShoppingListItem> itemMap = new LinkedHashMap<>();

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
                                                ingredient.getQuantity(), Double::sum);
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

    private void checkAndSave(AtomicInteger pending,
                              Map<String, Double> quantityMap,
                              Map<String, ShoppingListItem> itemMap,
                              Runnable onDone) {
        if (pending.decrementAndGet() == 0) {
            List<ShoppingListItem> finalItems = new ArrayList<>();
            for (Map.Entry<String, ShoppingListItem> entry : itemMap.entrySet()) {
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

    // ===========================
    // VISTA SEMANAL / MENSUAL
    // ===========================

    public void setWeekViewMode(boolean weekOnly) {
        this.weekViewMode = weekOnly;
        applyCurrentView();
    }

    private void applyCurrentView() {
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

        if (activeStoreFilter == null && activeCategoryFilter == null) {
            monthlyShoppingLists.setValue(new ArrayList<>(base));
            return;
        }

        List<WeeklyShoppingList> filtered = new ArrayList<>();
        for (WeeklyShoppingList week : base) {
            List<ShoppingListItem> filteredItems = new ArrayList<>();
            for (ShoppingListItem item : week.getItems()) {
                boolean matchesStore = activeStoreFilter == null
                        || activeStoreFilter.equalsIgnoreCase(item.getStore());
                boolean matchesCategory = activeCategoryFilter == null
                        || activeCategoryFilter.equalsIgnoreCase(item.getCategory());
                if (matchesStore && matchesCategory) filteredItems.add(item);
            }
            filtered.add(new WeeklyShoppingList(
                    week.getWeekId(), week.getMonday(), filteredItems));
        }
        monthlyShoppingLists.setValue(filtered);
    }

    // ===========================
    // FILTROS
    // ===========================

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

    public String getActiveStoreFilter() { return activeStoreFilter; }
    public String getActiveCategoryFilter() { return activeCategoryFilter; }

    // ===========================
    // CHECKBOX
    // ===========================

    public void toggleItemChecked(ShoppingListItem item) {
        item.setChecked(!item.isChecked());
        shoppingListRepository.updateItem(item,
                new ShoppingListRepository.ShoppingListCallback() {
                    @Override
                    public void onSuccess() { loadCurrentMonth(); }
                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue("Error actualizando item: " + e.getMessage());
                    }
                });
    }

    // ===========================
    // PRODUCTOS EXTRA
    // ===========================

    public void addExtraItem(ShoppingListItem item, boolean isNewProduct, Product product) {
        if (isNewProduct) {
            productRepository.addProduct(product,
                    new ProductRepository.OnProductAdded() {
                        @Override
                        public void onSuccess(String productId) { saveExtraItem(item); }
                        @Override
                        public void onFailure(Exception e) {
                            errorMessage.postValue("Error guardando producto: "
                                    + e.getMessage());
                        }
                    });
        } else {
            saveExtraItem(item);
        }
    }

    private void saveExtraItem(ShoppingListItem item) {
        shoppingListRepository.addItem(item,
                new ShoppingListRepository.OnItemAdded() {
                    @Override
                    public void onSuccess(String itemId) { loadCurrentMonth(); }
                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue("Error añadiendo producto: " + e.getMessage());
                    }
                });
    }

    public void deleteExtraItem(String itemId) {
        shoppingListRepository.deleteItem(itemId,
                new ShoppingListRepository.ShoppingListCallback() {
                    @Override
                    public void onSuccess() { loadCurrentMonth(); }
                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue("Error eliminando producto: " + e.getMessage());
                    }
                });
    }

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

    // ===========================
    // UTILIDADES
    // ===========================

    private List<WeeklyShoppingList> groupItemsByWeek(List<ShoppingListItem> items,
                                                      List<String> weekIds) {
        Map<String, List<ShoppingListItem>> byWeek = new LinkedHashMap<>();
        for (String weekId : weekIds) byWeek.put(weekId, new ArrayList<>());
        for (ShoppingListItem item : items) {
            if (byWeek.containsKey(item.getWeekId()))
                byWeek.get(item.getWeekId()).add(item);
        }
        List<WeeklyShoppingList> result = new ArrayList<>();
        for (Map.Entry<String, List<ShoppingListItem>> entry : byWeek.entrySet()) {
            Date monday = ShoppingListRepository.getMondayFromWeekId(entry.getKey());
            result.add(new WeeklyShoppingList(entry.getKey(), monday, entry.getValue()));
        }
        return result;
    }

    private List<String> getWeekIdsForCurrentMonth() {
        List<String> weekIds = new ArrayList<>();
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.setMinimalDaysInFirstWeek(4);
        cal.setFirstDayOfWeek(Calendar.MONDAY);

        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? -6 : Calendar.MONDAY - dow;
        cal.add(Calendar.DAY_OF_MONTH, diff);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 4; i++) {
            String weekId = ShoppingListRepository.getWeekId(cal.getTime());
            if (!weekIds.contains(weekId)) weekIds.add(weekId);
            cal.add(Calendar.DAY_OF_MONTH, 7);
        }
        return weekIds;
    }

    private Date getSunday(Date monday) {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.setTime(monday);
        cal.add(Calendar.DAY_OF_MONTH, 6);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTime();
    }
}