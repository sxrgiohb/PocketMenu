package com.example.pocketmenu.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketmenu.data.model.Leftover;
import com.example.pocketmenu.data.model.Menu;
import com.example.pocketmenu.data.model.Recipe;
import com.example.pocketmenu.data.model.UnassignedLeftover;
import com.example.pocketmenu.data.model.WeeklyMenuTemplate;
import com.example.pocketmenu.data.model.WeeklyMenuItem;
import com.example.pocketmenu.data.model.auxiliar.DayMenuWrapper;
import com.example.pocketmenu.data.model.auxiliar.LeftoverWithRecipe;
import com.example.pocketmenu.data.model.auxiliar.MenuAssignment;
import com.example.pocketmenu.data.repository.LeftoverRepository;
import com.example.pocketmenu.data.repository.MenuRepository;
import com.example.pocketmenu.data.repository.RecipeRepository;
import com.example.pocketmenu.data.repository.WeeklyMenuTemplateRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MenuViewModel extends ViewModel {

    public interface OnTemplateApplied {
        void onComplete(int unassignedPortions);
    }

    private final MenuRepository menuRepository;
    private final LeftoverRepository leftoverRepository;
    private final WeeklyMenuTemplateRepository templateRepository;
    private final RecipeRepository recipeRepository;

    private final MutableLiveData<List<DayMenuWrapper>> weekDays = new MutableLiveData<>();
    private final MutableLiveData<Date> selectedWeekStart = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Recipe>> allRecipes = new MutableLiveData<>();
    private final MutableLiveData<List<LeftoverWithRecipe>> validLeftovers = new MutableLiveData<>();

    public MenuViewModel() {
        menuRepository = new MenuRepository();
        leftoverRepository = new LeftoverRepository();
        templateRepository = new WeeklyMenuTemplateRepository();
        recipeRepository = new RecipeRepository();
        selectedWeekStart.setValue(getMonday(new Date()));
        loadWeek(getMonday(new Date()));
        menuRepository.deleteMenusOlderThan(15, null);
        leftoverRepository.deleteExpiredPerishableLeftovers(null);
    }

    public LiveData<List<DayMenuWrapper>> getWeekDays() { return weekDays; }
    public LiveData<Date> getSelectedWeekStart() { return selectedWeekStart; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<List<Recipe>> getAllRecipes() { return allRecipes; }
    public LiveData<List<LeftoverWithRecipe>> getValidLeftovers() { return validLeftovers; }

    // ===========================
    // SELECCIÓN DE SEMANA
    // ===========================
    public void selectWeek(Date anyDayInWeek) {
        Date monday = getMonday(anyDayInWeek);
        selectedWeekStart.setValue(monday);
        loadWeek(monday);
    }

    // ===========================
    // CARGA DE LA SEMANA
    // ===========================
    public void loadWeek(Date monday) {
        isLoading.setValue(true);
        List<Date> weekDates = getWeekDates(monday);
        int totalDays = weekDates.size();
        AtomicInteger completedDays = new AtomicInteger(0);
        Map<Integer, List<MenuAssignment>> assignmentsByDay = new HashMap<>();

        for (int i = 0; i < totalDays; i++) {
            final int dayIndex = i;
            final Date dayDate = weekDates.get(i);

            menuRepository.getMenusByDateQuery(dayDate)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<Menu> menus = querySnapshot.toObjects(Menu.class);
                        if (menus.isEmpty()) {
                            assignmentsByDay.put(dayIndex, new ArrayList<>());
                            checkIfWeekLoadComplete(completedDays, totalDays,
                                    weekDates, assignmentsByDay);
                            return;
                        }
                        AtomicInteger completedMenus = new AtomicInteger(0);
                        List<MenuAssignment> dayAssignments = new ArrayList<>();
                        for (Menu menu : menus) {
                            fetchAssignmentForMenu(menu, dayAssignments,
                                    completedMenus, menus.size(), () -> {
                                        assignmentsByDay.put(dayIndex, dayAssignments);
                                        checkIfWeekLoadComplete(completedDays, totalDays,
                                                weekDates, assignmentsByDay);
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        errorMessage.postValue("Error cargando día "
                                + (dayIndex + 1) + ": " + e.getMessage());
                        assignmentsByDay.put(dayIndex, new ArrayList<>());
                        checkIfWeekLoadComplete(completedDays, totalDays,
                                weekDates, assignmentsByDay);
                    });
        }
    }

    private void fetchAssignmentForMenu(Menu menu,
                                        List<MenuAssignment> dayAssignments,
                                        AtomicInteger completedMenus,
                                        int totalMenus,
                                        Runnable onAllMenusDone) {
        recipeRepository.getRecipeById(menu.getRecipeId(),
                new RecipeRepository.OnRecipeFound() {
                    @Override
                    public void onFound(Recipe recipe) {
                        leftoverRepository.getLeftoversByRecipe(recipe.getId(),
                                new LeftoverRepository.OnLeftoversLoaded() {
                                    @Override
                                    public void onLoaded(List<Leftover> leftovers) {
                                        Leftover associated = null;
                                        for (Leftover leftover : leftovers) {
                                            if (leftover.getSourceMenuId() != null
                                                    && leftover.getSourceMenuId().equals(menu.getId())
                                                    && LeftoverRepository.isStillValid(leftover, new Date())) {
                                                associated = leftover;
                                                break;
                                            }
                                        }
                                        addAndCheck(new MenuAssignment(menu, recipe, associated));
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        addAndCheck(new MenuAssignment(menu, recipe, null));
                                    }

                                    private void addAndCheck(MenuAssignment assignment) {
                                        synchronized (dayAssignments) {
                                            dayAssignments.add(assignment);
                                        }
                                        checkMenusDone(completedMenus, totalMenus, onAllMenusDone);
                                    }
                                });
                    }

                    @Override
                    public void onNotFound() {
                        checkMenusDone(completedMenus, totalMenus, onAllMenusDone);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue("Error cargando receta: " + e.getMessage());
                        checkMenusDone(completedMenus, totalMenus, onAllMenusDone);
                    }
                });
    }

    private void checkMenusDone(AtomicInteger counter, int total, Runnable onDone) {
        if (counter.incrementAndGet() >= total) onDone.run();
    }

    private void checkIfWeekLoadComplete(AtomicInteger completedDays,
                                         int totalDays,
                                         List<Date> weekDates,
                                         Map<Integer, List<MenuAssignment>> assignmentsByDay) {
        if (completedDays.incrementAndGet() >= totalDays) {
            List<DayMenuWrapper> result = new ArrayList<>();
            for (int i = 0; i < totalDays; i++) {
                result.add(new DayMenuWrapper(
                        i + 1,
                        weekDates.get(i),
                        assignmentsByDay.getOrDefault(i, new ArrayList<>())));
            }
            weekDays.postValue(result);
            isLoading.postValue(false);
        }
    }

    // ===========================
    // CARGA DE RECETAS PARA EL BUSCADOR
    // ===========================
    public void loadAllRecipes() {
        recipeRepository.getRecipesQuery(null)
                .get()
                .addOnSuccessListener(snap ->
                        allRecipes.postValue(snap.toObjects(Recipe.class)))
                .addOnFailureListener(e ->
                        errorMessage.postValue("Error cargando recetas: " + e.getMessage()));
    }

    // ===========================
    // CARGA DE SOBRAS VÁLIDAS
    // ===========================
    public void loadValidLeftovers(Date beforeDate) {
        leftoverRepository.getValidLeftovers(new LeftoverRepository.OnLeftoversLoaded() {
            @Override
            public void onLoaded(List<Leftover> leftovers) {
                if (leftovers.isEmpty()) {
                    validLeftovers.postValue(new ArrayList<>());
                    return;
                }

                List<LeftoverWithRecipe> result = new ArrayList<>();
                AtomicInteger pending = new AtomicInteger(leftovers.size());

                for (Leftover leftover : leftovers) {
                    if (leftover.getSourceMenuId() != null) {
                        Date assignedDate = leftover.getFirstAssignedDate();
                        if (assignedDate == null || !assignedDate.before(beforeDate)) {
                            if (pending.decrementAndGet() == 0)
                                validLeftovers.postValue(new ArrayList<>(result));
                            continue;
                        }
                    }

                    if (leftover.getPerishable() && leftover.getValidDays() > 0) {
                        Date assignedDate = leftover.getFirstAssignedDate();
                        if (assignedDate != null) {
                            long msPerDay = 24L * 60 * 60 * 1000;
                            long expirationMs = assignedDate.getTime()
                                    + (long) leftover.getValidDays() * msPerDay;
                            if (beforeDate.getTime() > expirationMs) {
                                if (pending.decrementAndGet() == 0)
                                    validLeftovers.postValue(new ArrayList<>(result));
                                continue;
                            }
                        }
                    }

                    recipeRepository.getRecipeById(leftover.getRecipeId(),
                            new RecipeRepository.OnRecipeFound() {
                                @Override
                                public void onFound(Recipe recipe) {
                                    synchronized (result) {
                                        result.add(new LeftoverWithRecipe(leftover, recipe));
                                    }
                                    if (pending.decrementAndGet() == 0)
                                        validLeftovers.postValue(new ArrayList<>(result));
                                }
                                @Override
                                public void onNotFound() {
                                    if (pending.decrementAndGet() == 0)
                                        validLeftovers.postValue(new ArrayList<>(result));
                                }
                                @Override
                                public void onFailure(Exception e) {
                                    if (pending.decrementAndGet() == 0)
                                        validLeftovers.postValue(new ArrayList<>(result));
                                }
                            });
                }
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Error cargando sobras: " + e.getMessage());
            }
        });
    }

    // ===========================
    // ASIGNAR RECETA A UN DÍA
    // ===========================
    public void assignRecipeToDay(Recipe recipe, Date day,
                                  boolean isPerishable, int validDays) {
        Date normalizedDay = normalizeDate(day);
        int usedPortions = 1;
        int leftoverPortions = recipe.getPortion() - usedPortions;

        Menu menu = new Menu(null, recipe.getId(), normalizedDay,
                usedPortions, recipe.getName(), false,
                false, null, null, isPerishable, validDays);

        menuRepository.addMenu(menu, new MenuRepository.OnMenuAdded() {
            @Override
            public void onSuccess(String menuId) {
                if (leftoverPortions > 0) {
                    createLeftover(recipe, menuId, normalizedDay,
                            leftoverPortions, isPerishable, validDays);
                } else {
                    reloadCurrentWeek();
                }
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Error asignando receta: " + e.getMessage());
            }
        });
    }

    private void createLeftover(Recipe recipe, String sourceMenuId, Date assignDate,
                                int portions, boolean isPerishable, int validDays) {
        Leftover leftover = new Leftover(
                null, recipe.getId(), sourceMenuId, portions,
                isPerishable, assignDate, isPerishable ? validDays : 0);
        leftoverRepository.addLeftover(leftover, new LeftoverRepository.LeftoverCallback() {
            @Override public void onSuccess() { reloadCurrentWeek(); }
            @Override public void onFailure(Exception e) {
                errorMessage.postValue("Error creando sobras: " + e.getMessage());
            }
        });
    }

    // ===========================
    // USAR SOBRAS EN UN DÍA
    // ===========================
    public void assignLeftoverToDay(Leftover leftover, Recipe recipe, Date day) {
        if (leftover.getRemainingPortions() <= 0) {
            errorMessage.setValue("No quedan raciones disponibles de esta sobra.");
            return;
        }
        Date normalizedDay = normalizeDate(day);

        Menu menu = new Menu(null, recipe.getId(), normalizedDay, 1,
                recipe.getName(), false, true,
                leftover.getRecipeId(), leftover.getSourceMenuId(),
                leftover.getPerishable(), leftover.getValidDays());

        menuRepository.addMenu(menu, new MenuRepository.OnMenuAdded() {
            @Override
            public void onSuccess(String menuId) {
                int newRemaining = leftover.getRemainingPortions() - 1;
                leftover.setRemainingPortions(newRemaining);
                if (newRemaining <= 0) {
                    leftoverRepository.deleteLeftover(leftover.getId(),
                            new LeftoverRepository.LeftoverCallback() {
                                @Override public void onSuccess() { reloadCurrentWeek(); }
                                @Override public void onFailure(Exception e) {
                                    errorMessage.postValue("Error eliminando sobra: "
                                            + e.getMessage());
                                }
                            });
                } else {
                    leftoverRepository.updateLeftover(leftover.getId(), leftover,
                            new LeftoverRepository.LeftoverCallback() {
                                @Override public void onSuccess() { reloadCurrentWeek(); }
                                @Override public void onFailure(Exception e) {
                                    errorMessage.postValue("Error actualizando sobra: "
                                            + e.getMessage());
                                }
                            });
                }
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Error asignando sobra: " + e.getMessage());
            }
        });
    }

    // ===========================
    // ELIMINAR ASIGNACIÓN
    // ===========================
    public void removeAssignmentFromDay(MenuAssignment assignment) {
        String menuId = assignment.getMenu().getId();
        if (menuId == null) return;

        menuRepository.deleteMenu(menuId, new MenuRepository.MenuCallback() {
            @Override
            public void onSuccess() {
                if (assignment.getMenu().isFromLeftover()) {
                    restoreLeftoverPortion(assignment);
                } else {
                    leftoverRepository.deleteLeftoversBySourceMenuId(menuId,
                            new LeftoverRepository.LeftoverCallback() {
                                @Override
                                public void onSuccess() {
                                    menuRepository.deleteMenusBySourceMenuId(menuId,
                                            new MenuRepository.MenuCallback() {
                                                @Override public void onSuccess() {
                                                    reloadCurrentWeek();
                                                }
                                                @Override public void onFailure(Exception e) {
                                                    errorMessage.postValue(
                                                            "Error eliminando asignaciones de sobras: "
                                                                    + e.getMessage());
                                                    reloadCurrentWeek();
                                                }
                                            });
                                }
                                @Override
                                public void onFailure(Exception e) {
                                    errorMessage.postValue("Error eliminando sobras: "
                                            + e.getMessage());
                                    reloadCurrentWeek();
                                }
                            });
                }
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Error eliminando asignación: " + e.getMessage());
            }
        });
    }

    private void restoreLeftoverPortion(MenuAssignment assignment) {
        String sourceRecipeId = assignment.getMenu().getSourceRecipeId();
        String sourceMenuId = assignment.getMenu().getSourceMenuId();
        boolean perishable = assignment.getMenu().isLeftoverPerishable();
        int validDays = assignment.getMenu().getLeftoverValidDays();

        if (sourceRecipeId == null) {
            reloadCurrentWeek();
            return;
        }

        leftoverRepository.getLeftoversByRecipe(sourceRecipeId,
                new LeftoverRepository.OnLeftoversLoaded() {
                    @Override
                    public void onLoaded(List<Leftover> leftovers) {
                        Leftover target = null;
                        for (Leftover l : leftovers) {
                            if (sourceMenuId != null
                                    && sourceMenuId.equals(l.getSourceMenuId())) {
                                target = l;
                                break;
                            }
                        }

                        if (target != null) {
                            target.setRemainingPortions(target.getRemainingPortions() + 1);
                            leftoverRepository.updateLeftover(target.getId(), target,
                                    new LeftoverRepository.LeftoverCallback() {
                                        @Override public void onSuccess() { reloadCurrentWeek(); }
                                        @Override public void onFailure(Exception e) {
                                            errorMessage.postValue("Error restaurando sobra: "
                                                    + e.getMessage());
                                            reloadCurrentWeek();
                                        }
                                    });
                        } else {
                            if (sourceMenuId == null) {
                                reloadCurrentWeek();
                                return;
                            }
                            menuRepository.getMenuById(sourceMenuId,
                                    new MenuRepository.OnMenuFound() {
                                        @Override
                                        public void onFound(Menu sourceMenu) {
                                            Leftover restored = new Leftover(
                                                    null,
                                                    sourceRecipeId,
                                                    sourceMenuId,
                                                    1,
                                                    perishable,
                                                    sourceMenu.getDate(),
                                                    perishable ? validDays : 0
                                            );
                                            leftoverRepository.addLeftover(restored,
                                                    new LeftoverRepository.LeftoverCallback() {
                                                        @Override public void onSuccess() { reloadCurrentWeek(); }
                                                        @Override public void onFailure(Exception e) {
                                                            errorMessage.postValue(
                                                                    "Error restaurando sobra: "
                                                                            + e.getMessage());
                                                            reloadCurrentWeek();
                                                        }
                                                    });
                                        }
                                        @Override public void onNotFound() { reloadCurrentWeek(); }
                                        @Override public void onFailure(Exception e) {
                                            reloadCurrentWeek();
                                        }
                                    });
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue("Error buscando sobra original: " + e.getMessage());
                        reloadCurrentWeek();
                    }
                });
    }

    // ===========================
    // GUARDAR SEMANA COMO FAVORITA
    // ===========================
    public void saveCurrentWeekAsFavorite(String name) {
        List<DayMenuWrapper> currentDays = weekDays.getValue();
        if (currentDays == null || currentDays.isEmpty()) {
            errorMessage.setValue("No hay menú para guardar.");
            return;
        }

        Date monday = selectedWeekStart.getValue();
        if (monday == null) return;

        List<WeeklyMenuItem> items = new ArrayList<>();
        for (DayMenuWrapper day : currentDays) {
            for (MenuAssignment assignment : day.getAssignments()) {
                if (assignment.getMenu().isFromLeftover()) {
                    items.add(new WeeklyMenuItem(
                            day.getDayOfWeek(),
                            assignment.getRecipe().getId(),
                            assignment.getMenu().getSourceRecipeId(),
                            assignment.getMenu().getUsedPortions()
                    ));
                } else {
                    Leftover generated = assignment.getLeftover();
                    boolean perishable;
                    int validDays;
                    if (generated != null) {
                        perishable = generated.getPerishable();
                        validDays = generated.getValidDays();
                    } else {
                        perishable = assignment.getMenu().isLeftoverPerishable();
                        validDays = assignment.getMenu().getLeftoverValidDays();
                    }
                    items.add(new WeeklyMenuItem(
                            day.getDayOfWeek(),
                            assignment.getRecipe().getId(),
                            assignment.getMenu().getUsedPortions(),
                            perishable,
                            validDays
                    ));
                }
            }
        }

        // Recopilar Menus principales para calcular sobras sin asignar
        List<MenuAssignment> mainAssignments = new ArrayList<>();
        for (DayMenuWrapper day : currentDays) {
            for (MenuAssignment assignment : day.getAssignments()) {
                if (!assignment.getMenu().isFromLeftover()) {
                    mainAssignments.add(assignment);
                }
            }
        }

        if (mainAssignments.isEmpty()) {
            saveTemplate(name, items, new ArrayList<>());
            return;
        }

        AtomicInteger pending = new AtomicInteger(mainAssignments.size());
        List<UnassignedLeftover> unassignedSync = new ArrayList<>();

        for (MenuAssignment mainAssignment : mainAssignments) {
            String menuId = mainAssignment.getMenu().getId();
            boolean perishable = mainAssignment.getMenu().isLeftoverPerishable();
            int validDays = mainAssignment.getMenu().getLeftoverValidDays();
            String recipeId = mainAssignment.getRecipe().getId();
            int usedPortions = mainAssignment.getMenu().getUsedPortions();

            recipeRepository.getRecipeById(recipeId, new RecipeRepository.OnRecipeFound() {
                @Override
                public void onFound(Recipe recipe) {
                    int totalPortions = recipe.getPortion();

                    // Contar sobras de este menu consumidas dentro de la semana
                    int consumedAsLeftoverThisWeek = 0;
                    for (DayMenuWrapper day : currentDays) {
                        for (MenuAssignment a : day.getAssignments()) {
                            if (a.getMenu().isFromLeftover()
                                    && menuId != null
                                    && menuId.equals(a.getMenu().getSourceMenuId())) {
                                consumedAsLeftoverThisWeek++;
                            }
                        }
                    }

                    // Raciones sin asignar = total - usadas el día principal - consumidas como sobra esta semana
                    int unassignedPortions = totalPortions - usedPortions - consumedAsLeftoverThisWeek;

                    if (unassignedPortions > 0) {
                        synchronized (unassignedSync) {
                            unassignedSync.add(new UnassignedLeftover(
                                    recipeId,
                                    unassignedPortions,
                                    perishable,
                                    perishable ? validDays : 0
                            ));
                        }
                    }

                    if (pending.decrementAndGet() == 0) {
                        saveTemplate(name, items, unassignedSync);
                    }
                }

                @Override
                public void onNotFound() {
                    if (pending.decrementAndGet() == 0) {
                        saveTemplate(name, items, unassignedSync);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    errorMessage.postValue("Error leyendo receta: " + e.getMessage());
                    if (pending.decrementAndGet() == 0) {
                        saveTemplate(name, items, unassignedSync);
                    }
                }
            });
        }
    }

    private void saveTemplate(String name, List<WeeklyMenuItem> items,
                              List<UnassignedLeftover> unassigned) {
        WeeklyMenuTemplate template = new WeeklyMenuTemplate(
                null, null, name, true, items, unassigned);
        templateRepository.addTemplate(template,
                new WeeklyMenuTemplateRepository.WeeklyMenuCallback() {
                    @Override public void onSuccess() { }
                    @Override public void onFailure(Exception e) {
                        errorMessage.postValue("Error guardando favorito: " + e.getMessage());
                    }
                });
    }

    // ===========================
    // APLICAR PLANTILLA FAVORITA
    // ===========================
    public void applyTemplate(WeeklyMenuTemplate template, OnTemplateApplied callback) {
        Date monday = selectedWeekStart.getValue();
        if (monday == null || template.getItems() == null) return;

        List<Date> weekDates = getWeekDates(monday);
        Date sunday = weekDates.get(6);
        isLoading.setValue(true);

        menuRepository.deleteMenusByDateRange(monday, sunday,
                new MenuRepository.MenuCallback() {
                    @Override
                    public void onSuccess() {
                        leftoverRepository.deleteLeftoversByDateRange(monday, sunday,
                                new LeftoverRepository.LeftoverCallback() {
                                    @Override
                                    public void onSuccess() {
                                        List<WeeklyMenuItem> mainItems = new ArrayList<>();
                                        List<WeeklyMenuItem> leftoverItems = new ArrayList<>();
                                        for (WeeklyMenuItem item : template.getItems()) {
                                            if (item.isLeftover()) leftoverItems.add(item);
                                            else mainItems.add(item);
                                        }
                                        applyMainItems(mainItems, leftoverItems,
                                                weekDates, template, callback);
                                    }
                                    @Override
                                    public void onFailure(Exception e) {
                                        errorMessage.postValue("Error limpiando sobras: "
                                                + e.getMessage());
                                        isLoading.postValue(false);
                                    }
                                });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue("Error limpiando menus: " + e.getMessage());
                        isLoading.postValue(false);
                    }
                });
    }

    private void applyMainItems(List<WeeklyMenuItem> mainItems,
                                List<WeeklyMenuItem> leftoverItems,
                                List<Date> weekDates,
                                WeeklyMenuTemplate template,
                                OnTemplateApplied callback) {
        if (mainItems.isEmpty()) {
            applyLeftoverItems(leftoverItems, new HashMap<>(), weekDates, template, callback);
            return;
        }

        Map<String, Leftover> generatedLeftovers = new HashMap<>();
        AtomicInteger pending = new AtomicInteger(mainItems.size());

        for (WeeklyMenuItem item : mainItems) {
            int dayIndex = item.getDayOfWeek() - 1;
            if (dayIndex < 0 || dayIndex >= weekDates.size()) {
                if (pending.decrementAndGet() == 0)
                    applyLeftoverItems(leftoverItems, generatedLeftovers,
                            weekDates, template, callback);
                continue;
            }

            Date dayDate = weekDates.get(dayIndex);

            Menu menu = new Menu(null, item.getRecipeId(), dayDate,
                    item.getPortions(), "", false,
                    false, null, null, item.isPerishable(), item.getValidDays());

            menuRepository.addMenu(menu, new MenuRepository.OnMenuAdded() {
                @Override
                public void onSuccess(String menuId) {
                    recipeRepository.getRecipeById(item.getRecipeId(),
                            new RecipeRepository.OnRecipeFound() {
                                @Override
                                public void onFound(Recipe recipe) {
                                    int leftoverPortions =
                                            recipe.getPortion() - item.getPortions();
                                    if (leftoverPortions > 0) {
                                        Leftover leftover = new Leftover(
                                                null,
                                                recipe.getId(),
                                                menuId,
                                                leftoverPortions,
                                                item.isPerishable(),
                                                dayDate,
                                                item.isPerishable() ? item.getValidDays() : 0);
                                        leftoverRepository.addLeftover(leftover,
                                                new LeftoverRepository.LeftoverCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        synchronized (generatedLeftovers) {
                                                            generatedLeftovers.put(
                                                                    recipe.getId(), leftover);
                                                        }
                                                        if (pending.decrementAndGet() == 0)
                                                            applyLeftoverItems(leftoverItems,
                                                                    generatedLeftovers,
                                                                    weekDates, template, callback);
                                                    }
                                                    @Override
                                                    public void onFailure(Exception e) {
                                                        errorMessage.postValue(
                                                                "Error creando sobra: "
                                                                        + e.getMessage());
                                                        if (pending.decrementAndGet() == 0)
                                                            applyLeftoverItems(leftoverItems,
                                                                    generatedLeftovers,
                                                                    weekDates, template, callback);
                                                    }
                                                });
                                    } else {
                                        if (pending.decrementAndGet() == 0)
                                            applyLeftoverItems(leftoverItems,
                                                    generatedLeftovers,
                                                    weekDates, template, callback);
                                    }
                                }
                                @Override public void onNotFound() {
                                    if (pending.decrementAndGet() == 0)
                                        applyLeftoverItems(leftoverItems,
                                                generatedLeftovers, weekDates, template, callback);
                                }
                                @Override public void onFailure(Exception e) {
                                    if (pending.decrementAndGet() == 0)
                                        applyLeftoverItems(leftoverItems,
                                                generatedLeftovers, weekDates, template, callback);
                                }
                            });
                }
                @Override
                public void onFailure(Exception e) {
                    errorMessage.postValue("Error creando menu: " + e.getMessage());
                    if (pending.decrementAndGet() == 0)
                        applyLeftoverItems(leftoverItems, generatedLeftovers,
                                weekDates, template, callback);
                }
            });
        }
    }

    private void applyLeftoverItems(List<WeeklyMenuItem> leftoverItems,
                                    Map<String, Leftover> generatedLeftovers,
                                    List<Date> weekDates,
                                    WeeklyMenuTemplate template,
                                    OnTemplateApplied callback) {
        if (leftoverItems.isEmpty()) {
            applyUnassignedLeftovers(template, weekDates, callback);
            return;
        }

        AtomicInteger pending = new AtomicInteger(leftoverItems.size());

        for (WeeklyMenuItem item : leftoverItems) {
            int dayIndex = item.getDayOfWeek() - 1;
            if (dayIndex < 0 || dayIndex >= weekDates.size()) {
                if (pending.decrementAndGet() == 0)
                    applyUnassignedLeftovers(template, weekDates, callback);
                continue;
            }

            Date dayDate = weekDates.get(dayIndex);
            Leftover leftover = generatedLeftovers.get(item.getSourceRecipeId());

            if (leftover == null) {
                if (pending.decrementAndGet() == 0)
                    applyUnassignedLeftovers(template, weekDates, callback);
                continue;
            }

            Menu menu = new Menu(null, item.getRecipeId(), dayDate, 1,
                    "", false, true, item.getSourceRecipeId(), leftover.getSourceMenuId(),
                    leftover.getPerishable(), leftover.getValidDays());

            menuRepository.addMenu(menu, new MenuRepository.OnMenuAdded() {
                @Override
                public void onSuccess(String menuId) {
                    int newRemaining = leftover.getRemainingPortions() - 1;
                    leftover.setRemainingPortions(newRemaining);
                    if (newRemaining <= 0) {
                        leftoverRepository.deleteLeftover(leftover.getId(),
                                new LeftoverRepository.LeftoverCallback() {
                                    @Override public void onSuccess() {
                                        if (pending.decrementAndGet() == 0)
                                            applyUnassignedLeftovers(
                                                    template, weekDates, callback);
                                    }
                                    @Override public void onFailure(Exception e) {
                                        if (pending.decrementAndGet() == 0)
                                            applyUnassignedLeftovers(
                                                    template, weekDates, callback);
                                    }
                                });
                    } else {
                        leftoverRepository.updateLeftover(leftover.getId(), leftover,
                                new LeftoverRepository.LeftoverCallback() {
                                    @Override public void onSuccess() {
                                        if (pending.decrementAndGet() == 0)
                                            applyUnassignedLeftovers(
                                                    template, weekDates, callback);
                                    }
                                    @Override public void onFailure(Exception e) {
                                        if (pending.decrementAndGet() == 0)
                                            applyUnassignedLeftovers(
                                                    template, weekDates, callback);
                                    }
                                });
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    errorMessage.postValue("Error creando menu de sobra: " + e.getMessage());
                    if (pending.decrementAndGet() == 0)
                        applyUnassignedLeftovers(template, weekDates, callback);
                }
            });
        }
    }

    private void applyUnassignedLeftovers(WeeklyMenuTemplate template,
                                          List<Date> weekDates,
                                          OnTemplateApplied callback) {
        List<UnassignedLeftover> unassigned = template.getUnassignedLeftovers();
        if (unassigned == null || unassigned.isEmpty()) {
            reloadCurrentWeek();
            if (callback != null) callback.onComplete(0);
            return;
        }

        Date monday = weekDates.get(0);
        int totalUnassignedPortions = 0;
        for (UnassignedLeftover u : unassigned)
            totalUnassignedPortions += u.getRemainingPortions();
        final int totalForCallback = totalUnassignedPortions;

        AtomicInteger pending = new AtomicInteger(unassigned.size());

        for (UnassignedLeftover u : unassigned) {
            Leftover leftover = new Leftover(
                    null, u.getRecipeId(), null,
                    u.getRemainingPortions(), u.isPerishable(),
                    monday, u.isPerishable() ? u.getValidDays() : 0);
            leftoverRepository.addLeftover(leftover,
                    new LeftoverRepository.LeftoverCallback() {
                        @Override
                        public void onSuccess() {
                            if (pending.decrementAndGet() == 0) {
                                reloadCurrentWeek();
                                if (callback != null) callback.onComplete(totalForCallback);
                            }
                        }
                        @Override
                        public void onFailure(Exception e) {
                            errorMessage.postValue("Error creando sobra sin asignar: "
                                    + e.getMessage());
                            if (pending.decrementAndGet() == 0) {
                                reloadCurrentWeek();
                                if (callback != null) callback.onComplete(totalForCallback);
                            }
                        }
                    });
        }
    }

    // ===========================
    // UTILIDADES
    // ===========================
    public void reloadCurrentWeek() {
        Date monday = selectedWeekStart.getValue();
        if (monday != null) loadWeek(monday);
    }

    private Date normalizeDate(Date date) {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getMonday(Date anyDay) {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.setTime(anyDay);
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -6);
        } else {
            cal.add(Calendar.DAY_OF_MONTH,
                    -(cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY));
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private List<Date> getWeekDates(Date monday) {
        List<Date> dates = new ArrayList<>();
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.setTime(monday);
        for (int i = 0; i < 7; i++) {
            dates.add(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dates;
    }
}