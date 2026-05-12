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
import com.example.pocketmenu.utils.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MenuViewModel extends ViewModel {

    // Finished applying a favorite; argument is total portions left unassigned on the schedule
    public interface OnTemplateApplied {
        void onComplete(int unassignedPortions);
    }

    // Data sources used to compose menu rows with recipe + leftover information.
    private final MenuRepository menuRepository;
    private final LeftoverRepository leftoverRepository;
    private final WeeklyMenuTemplateRepository templateRepository;
    private final RecipeRepository recipeRepository;

    private final MutableLiveData<List<DayMenuWrapper>> weekDays = new MutableLiveData<>();
    private final MutableLiveData<Date> selectedWeekStart = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<List<LeftoverWithRecipe>> validLeftovers = new MutableLiveData<>();
    private final MutableLiveData<List<WeeklyMenuTemplate>> favoriteTemplates = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> favoriteTemplateRecipeNames = new MutableLiveData<>();
    private final MutableLiveData<Boolean> favoriteTemplatesLoading = new MutableLiveData<>();

    // Constructor. The default week is current week (Monday)
    public MenuViewModel() {
        menuRepository = new MenuRepository();
        leftoverRepository = new LeftoverRepository();
        templateRepository = new WeeklyMenuTemplateRepository();
        recipeRepository = new RecipeRepository();
        selectedWeekStart.setValue(DateUtils.getMonday(new Date()));
        loadWeek(DateUtils.getMonday(new Date()));
    }

    // LiveData getters
    public LiveData<List<DayMenuWrapper>> getWeekDays() { return weekDays; }
    public LiveData<Date> getSelectedWeekStart() { return selectedWeekStart; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<List<Recipe>> getAllRecipes() { return recipeRepository.getRecipesLiveData(); }
    public LiveData<List<LeftoverWithRecipe>> getValidLeftovers() { return validLeftovers; }
    public LiveData<List<WeeklyMenuTemplate>> getFavoriteTemplates() { return favoriteTemplates; }
    public LiveData<Map<String, String>> getFavoriteTemplateRecipeNames() { return favoriteTemplateRecipeNames; }
    public LiveData<Boolean> getFavoriteTemplatesLoading() { return favoriteTemplatesLoading; }

    // Any date in the week → Monday baseline, then reload
    public void selectWeek(Date anyDayInWeek) {
        Date monday = DateUtils.getMonday(anyDayInWeek);
        selectedWeekStart.setValue(monday);
        loadWeek(monday);
    }

    // One Firestore fetch per weekday; attaches recipe plus matching leftover when present
    public void loadWeek(Date monday) {
        List<Date> weekDates = DateUtils.getWeekDates(monday);
        int totalDays = weekDates.size();
        AtomicInteger completedDays = new AtomicInteger(0);
        // Firestore loads the day and the menu assignment out of order
        Map<Integer, List<MenuAssignment>> assignmentsByDay = new HashMap<>();

        for (int i = 0; i < totalDays; i++) {
            final int dayIndex = i;
            final Date dayDate = weekDates.get(i);

            menuRepository.getMenusByDateQuery(dayDate)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        // Checks every day
                        List<Menu> menus = querySnapshot.toObjects(Menu.class);
                        if (menus.isEmpty()) {
                            assignmentsByDay.put(dayIndex, new ArrayList<>());
                            checkIfWeekLoadComplete(completedDays, totalDays,
                                    weekDates, assignmentsByDay);
                            return;
                        }
                        // Checks every menu in a day
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

    // Fires one async chain: recipe by id → leftovers for recipe → MenuAssignment
    private void fetchAssignmentForMenu(Menu menu,
                                        List<MenuAssignment> dayAssignments,
                                        AtomicInteger completedMenus,
                                        int totalMenus,
                                        Runnable onAllMenusDone) {
        // Search recipe by id
        recipeRepository.getRecipeById(menu.getRecipeId(),
                new RecipeRepository.OnRecipeFound() {
                    @Override
                    public void onFound(Recipe recipe) {
                        // Search leftovers for recipe
                        leftoverRepository.getLeftoversByRecipe(recipe.getId(),
                                new LeftoverRepository.OnLeftoversLoaded() {
                                    @Override
                                    public void onLoaded(List<Leftover> leftovers) {
                                        Leftover associated = null;
                                        for (Leftover leftover : leftovers) {
                                            if (leftover.getSourceMenuId() != null // Letover has a reference to the menu id
                                                    && leftover.getSourceMenuId().equals(menu.getId()) // The references are equal
                                                    && LeftoverRepository.isStillValid(leftover, new Date())) { // The leftover is still valid
                                                associated = leftover;
                                                break;
                                            }
                                        }
                                        // Creates the menu assignment and adds it to the list
                                        addAndCheck(new MenuAssignment(menu, recipe, associated));
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        addAndCheck(new MenuAssignment(menu, recipe, null));
                                    }

                                    private void addAndCheck(MenuAssignment assignment) {
                                        // Controls the addition to the day's assignments
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

    // Counts async menu fetches finished for one day
    private void checkMenusDone(AtomicInteger counter, int total, Runnable onDone) {
        if (counter.incrementAndGet() >= total) onDone.run();
    }

    // When all seven days finished, publish ordered weekDays and clear loading
    private void checkIfWeekLoadComplete(AtomicInteger completedDays,
                                         int totalDays,
                                         List<Date> weekDates,
                                         Map<Integer, List<MenuAssignment>> assignmentsByDay) {
        if (completedDays.incrementAndGet() >= totalDays) {
            List<DayMenuWrapper> result = new ArrayList<>();
            for (int i = 0; i < totalDays; i++) {
                // Orders the days by date
                result.add(new DayMenuWrapper(
                        i + 1,
                        weekDates.get(i),
                        assignmentsByDay.getOrDefault(i, new ArrayList<>())));
            }
            weekDays.postValue(result);
        }
    }

    // Triggers RecipeRepository live query (used by recipe picker dialog)
    public void loadAllRecipes() {
        recipeRepository.getRecipes(null);
    }

    // Picker list: filters leftovers by assignment rules and leftover repository date helpers
    public void loadValidLeftovers(Date beforeDate) {
        leftoverRepository.getValidLeftovers(new LeftoverRepository.OnLeftoversLoaded() {
            @Override
            public void onLoaded(List<Leftover> leftovers) {
                // No leftovers found
                if (leftovers.isEmpty()) {
                    validLeftovers.postValue(new ArrayList<>());
                    return;
                }

                List<LeftoverWithRecipe> result = new ArrayList<>();
                AtomicInteger pending = new AtomicInteger(leftovers.size());

                // Leftovers created from a main recipe
                for (Leftover leftover : leftovers) {
                    // First assignment day
                    if (leftover.getSourceMenuId() != null) {
                        Date assignedDate = leftover.getFirstAssignedDate();
                        // Main recipe
                        if (assignedDate == null || assignedDate.after(beforeDate)) {
                            if (pending.decrementAndGet() == 0)
                                validLeftovers.postValue(new ArrayList<>(result));
                            continue;
                        }
                    }

                    // Assignable that day
                    if (!LeftoverRepository.isAssignableOnDay(leftover, beforeDate)) {
                        if (pending.decrementAndGet() == 0)
                            validLeftovers.postValue(new ArrayList<>(result));
                        continue;
                    }

                    // Query
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
                                    // Decrements and discards missing leftovers
                                    if (pending.decrementAndGet() == 0)
                                        validLeftovers.postValue(new ArrayList<>(result));
                                }
                                @Override
                                public void onFailure(Exception e) {
                                    // Decrements and discards missing leftovers
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

    // One plated portion on the calendar; persists extra portions as a leftover doc when needed
    public void assignRecipeToDay(Recipe recipe,
                                  Date day,
                                  boolean isPerishable,
                                  int validDays) {
        Date normalizedDay = DateUtils.normalizeDate(day);
        // First assignment of the recipe
        int usedPortions = 1;
        int leftoverPortions = recipe.getPortion() - usedPortions;

        Menu menu = new Menu(null,
                recipe.getId(),
                normalizedDay,
                usedPortions,
                recipe.getName(),
                false,
                false,
                null,
                null,
                isPerishable,
                validDays);

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

    // Persists leftover after primary meal is assigned
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

    // Menu row from consumed leftover: decrements or deletes leftover document
    public void assignLeftoverToDay(Leftover leftover, Recipe recipe, Date day) {
        if (leftover.getRemainingPortions() <= 0) {
            errorMessage.setValue("No quedan raciones disponibles de esta sobra.");
            return;
        }
        Date normalizedDay = DateUtils.normalizeDate(day);

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

    // Delete plated row; leftover-based rows bump stock back, normal rows purge child leftovers/menus
    public void removeAssignmentFromDay(MenuAssignment assignment) {
        String menuId = assignment.getMenu().getId();
        if (menuId == null) return;

        menuRepository.deleteMenu(menuId, new MenuRepository.MenuCallback() {
            @Override
            public void onSuccess() {
                // Makes the leftover available again
                if (assignment.getMenu().isFromLeftover()) {
                    restoreLeftoverPortion(assignment);
                } else {
                    // Deletes the leftovers assigned to the main recipe
                    leftoverRepository.deleteLeftoversBySourceMenuId(menuId,
                            new LeftoverRepository.LeftoverCallback() {
                                @Override
                                public void onSuccess() {
                                    // Deletes the main recipe
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

    // Undo one leftover meal: bump existing Leftover or recreate from source menu metadata
    private void restoreLeftoverPortion(MenuAssignment assignment) {
        String sourceRecipeId = assignment.getMenu().getSourceRecipeId();
        String sourceMenuId = assignment.getMenu().getSourceMenuId();
        boolean perishable = assignment.getMenu().isLeftoverPerishable();
        int validDays = assignment.getMenu().getLeftoverValidDays();

        if (sourceRecipeId == null) {
            reloadCurrentWeek();
            return;
        }
        // Search all leftovers whose recipeId matches the source recipe
        leftoverRepository.getLeftoversByRecipe(sourceRecipeId,
                new LeftoverRepository.OnLeftoversLoaded() {
                    @Override
                    public void onLoaded(List<Leftover> leftovers) {
                        // Target is used to identify if the leftover was already deleted
                        Leftover target = null;
                        for (Leftover l : leftovers) {
                            if (sourceMenuId != null && sourceMenuId.equals(l.getSourceMenuId())) {
                                target = l;
                                break;
                            }
                        }
                        // Existing leftover
                        if (target != null) {
                            // Adds one portion to the leftover
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
                            // Deleted leftover
                            if (sourceMenuId == null) {
                                reloadCurrentWeek();
                                return;
                            }
                            menuRepository.getMenuById(sourceMenuId,
                                    new MenuRepository.OnMenuFound() {
                                        @Override
                                        // Creates and persists a new leftover from the source menu
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

    // Builds a reusable week template snapshot
    public void saveCurrentWeekAsFavorite(String name) {
        List<DayMenuWrapper> currentDays = weekDays.getValue();
        if (currentDays == null || currentDays.isEmpty()) {
            errorMessage.setValue("No hay menú para guardar.");
            return;
        }
        // Build a list of weekly menu items
        List<WeeklyMenuItem> items = new ArrayList<>();
        for (DayMenuWrapper day : currentDays) {
            for (MenuAssignment assignment : day.getAssignments()) {
                // Menu is a leftover
                if (assignment.getMenu().isFromLeftover()) {
                    items.add(new WeeklyMenuItem(
                            day.getDayOfWeek(),
                            assignment.getRecipe().getId(),
                            assignment.getMenu().getSourceRecipeId(),
                            assignment.getMenu().getUsedPortions()
                    ));
                } else {
                    // Menu is a main recipe
                    Leftover generated = assignment.getLeftover(); // Used to obtain perishable and valiDays
                    boolean perishable;
                    int validDays;
                    if (generated != null) {
                        // Leftover still exists in Firestore
                        perishable = generated.getPerishable();
                        validDays = generated.getValidDays();
                    } else {
                        // Leftover was deleted. Data retrieved from denormalized menu
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

        // Filters out leftovers from main recipes
        List<MenuAssignment> mainAssignments = new ArrayList<>();
        for (DayMenuWrapper day : currentDays) {
            for (MenuAssignment assignment : day.getAssignments()) {
                if (!assignment.getMenu().isFromLeftover()) {
                    mainAssignments.add(assignment);
                }
            }
        }

        // No main recipes found
        if (mainAssignments.isEmpty()) {
            saveTemplate(name, items, new ArrayList<>());
            return;
        }

        // Calculate unassigned leftovers
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
                    int consumedAsLeftoverThisWeek = 0;
                    // Counts consumed leftovers during the current week
                    for (DayMenuWrapper day : currentDays) {
                        for (MenuAssignment a : day.getAssignments()) {
                            if (a.getMenu().isFromLeftover()
                                    && menuId != null
                                    && menuId.equals(a.getMenu().getSourceMenuId())) { // Points to the same menu and not another one of the same recipe
                                consumedAsLeftoverThisWeek++;
                            }
                        }
                    }
                    /* Example:
                    Recipe A: total Portions 4
                    used Portions 1
                    consumed as leftovers 2
                    -----------------------
                    unassigned portions x
                    x = 4 - 1 - 2
                    */
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

    // Loads templates for FavoriteTemplatesDialog and resolves recipe names used in rows
    public void loadFavoriteTemplatesForDialog() {
        favoriteTemplatesLoading.setValue(true);
        templateRepository.getAllTemplates(new WeeklyMenuTemplateRepository.OnTemplatesLoaded() {
            @Override
            public void onLoaded(List<WeeklyMenuTemplate> templates) {
                favoriteTemplates.postValue(templates);
                resolveFavoriteTemplateRecipeNames(templates);
            }

            @Override
            public void onFailure(Exception e) {
                favoriteTemplatesLoading.postValue(false);
                favoriteTemplates.postValue(new ArrayList<>());
                favoriteTemplateRecipeNames.postValue(new HashMap<>());
                errorMessage.postValue("Error cargando plantillas: " + e.getMessage());
            }
        });
    }

    // Deletes one favorite template and reloads list for the dialog.
    public void deleteFavoriteTemplate(String templateId) {
        templateRepository.deleteTemplate(templateId, new WeeklyMenuTemplateRepository.WeeklyMenuCallback() {
            @Override
            public void onSuccess() {
                loadFavoriteTemplatesForDialog();
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Error eliminando plantilla: " + e.getMessage());
            }
        });
    }

    private void resolveFavoriteTemplateRecipeNames(List<WeeklyMenuTemplate> templates) {
        Map<String, String> emptyMap = new HashMap<>();
        if (templates == null || templates.isEmpty()) {
            favoriteTemplateRecipeNames.postValue(emptyMap);
            favoriteTemplatesLoading.postValue(false);
            return;
        }
        // Adds all recipe ids to a list
        List<String> recipeIds = new ArrayList<>();
        for (WeeklyMenuTemplate template : templates) {
            if (template.getItems() == null) continue;
            for (WeeklyMenuItem item : template.getItems()) {
                if (item.getRecipeId() != null && !recipeIds.contains(item.getRecipeId())) {
                    recipeIds.add(item.getRecipeId());
                }
                if (item.isLeftover()
                        && item.getSourceRecipeId() != null
                        && !recipeIds.contains(item.getSourceRecipeId())) {
                    recipeIds.add(item.getSourceRecipeId());
                }
            }
        }

        if (recipeIds.isEmpty()) {
            favoriteTemplateRecipeNames.postValue(emptyMap);
            favoriteTemplatesLoading.postValue(false);
            return;
        }

        // Adds all recipe names to a map
        Map<String, String> names = new HashMap<>();
        AtomicInteger pending = new AtomicInteger(recipeIds.size());

        for (String recipeId : recipeIds) {
            recipeRepository.getRecipeById(recipeId, new RecipeRepository.OnRecipeFound() {
                @Override
                public void onFound(Recipe recipe) {
                    synchronized (names) {
                        names.put(recipeId, recipe.getName());
                    }
                    finishIfDone();
                }

                @Override
                public void onNotFound() {
                    finishIfDone();
                }

                @Override
                public void onFailure(Exception e) {
                    finishIfDone();
                }

                private void finishIfDone() {
                    if (pending.decrementAndGet() == 0) {
                        favoriteTemplateRecipeNames.postValue(names);
                        favoriteTemplatesLoading.postValue(false);
                    }
                }
            });
        }
    }

    // Writes template document to Firestore
    private void saveTemplate(String name, List<WeeklyMenuItem> items, List<UnassignedLeftover> unassigned) {
        WeeklyMenuTemplate template = new WeeklyMenuTemplate(null, null, name, true, items, unassigned);
        templateRepository.addTemplate(template, new WeeklyMenuTemplateRepository.WeeklyMenuCallback() {
            @Override public void onSuccess() { }
            @Override public void onFailure(Exception e) {
                errorMessage.postValue("Error guardando favorito: " + e.getMessage());
            }
        });
    }

    // Wipes visible week storage, then replay template
    public void applyTemplate(WeeklyMenuTemplate template, OnTemplateApplied callback) {
        Date monday = selectedWeekStart.getValue();
        if (monday == null || template.getItems() == null) return;

        List<Date> weekDates = DateUtils.getWeekDates(monday);
        Date sunday = weekDates.get(6);

        // Delete all menus from the selected week
        menuRepository.deleteMenusByDateRange(monday, sunday,
                new MenuRepository.MenuCallback() {
                    @Override
                    public void onSuccess() {
                        leftoverRepository.deleteLeftoversByDateRange(monday, sunday,
                                new LeftoverRepository.LeftoverCallback() {
                                    @Override
                                    public void onSuccess() {
                                        // Iterates the template and filters it
                                        List<WeeklyMenuItem> mainItems = new ArrayList<>();
                                        List<WeeklyMenuItem> leftoverItems = new ArrayList<>();
                                        for (WeeklyMenuItem item : template.getItems()) {
                                            if (item.isLeftover()) leftoverItems.add(item);
                                            else mainItems.add(item);
                                        }
                                        // Apply the template
                                        applyMainItems(mainItems, leftoverItems, weekDates, template, callback);
                                    }
                                    @Override
                                    public void onFailure(Exception e) {
                                        errorMessage.postValue("Error limpiando sobras: " + e.getMessage());
                                    }
                                });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.postValue("Error limpiando menus: " + e.getMessage());
                    }
                });
    }

    // Step one: create each non-leftover template row and leftover documents for extra portions
    private void applyMainItems(List<WeeklyMenuItem> mainItems,
                                List<WeeklyMenuItem> leftoverItems,
                                List<Date> weekDates,
                                WeeklyMenuTemplate template,
                                OnTemplateApplied callback) {
        if (mainItems.isEmpty()) {
            applyLeftoverItems(leftoverItems, new HashMap<>(), weekDates, template, callback);
            return;
        }
        // Data connection between step one and two (recipeID, leftover)
        Map<String, Leftover> generatedLeftovers = new HashMap<>();
        AtomicInteger pending = new AtomicInteger(mainItems.size());

        for (WeeklyMenuItem item : mainItems) {
            // Obtains one day between 1 and 7 and adapts it
            int dayIndex = item.getDayOfWeek() - 1;
            if (dayIndex < 0 || dayIndex >= weekDates.size()) {
                if (pending.decrementAndGet() == 0)
                    applyLeftoverItems(leftoverItems, generatedLeftovers,
                            weekDates, template, callback);
                continue;
            }

            // Creates a menu using the template items
            Date dayDate = weekDates.get(dayIndex);
            Menu menu = new Menu(null, item.getRecipeId(), dayDate,
                    item.getPortions(), "", false,
                    false, null, null, item.isPerishable(), item.getValidDays());

            // Persists the main recipe
            menuRepository.addMenu(menu, new MenuRepository.OnMenuAdded() {
                @Override
                public void onSuccess(String menuId) {
                    // Obtain the amount of portions
                    recipeRepository.getRecipeById(item.getRecipeId(),
                            new RecipeRepository.OnRecipeFound() {
                                @Override
                                public void onFound(Recipe recipe) {
                                    // Calculates the leftover portions
                                    int leftoverPortions = recipe.getPortion() - item.getPortions();
                                    if (leftoverPortions > 0) {
                                        // Creates a leftover and persists it
                                        Leftover leftover = new Leftover(
                                                null, recipe.getId(), menuId,
                                                leftoverPortions, item.isPerishable(),
                                                dayDate,
                                                item.isPerishable() ? item.getValidDays() : 0);
                                        leftoverRepository.addLeftover(leftover,
                                                new LeftoverRepository.LeftoverCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        // Adds the leftover to the map
                                                        synchronized (generatedLeftovers) {
                                                            generatedLeftovers.put(recipe.getId(), leftover);
                                                        }
                                                        if (pending.decrementAndGet() == 0)
                                                            applyLeftoverItems(leftoverItems,
                                                                    generatedLeftovers,
                                                                    weekDates, template, callback);
                                                    }
                                                    @Override
                                                    public void onFailure(Exception e) {
                                                        errorMessage.postValue("Error creando sobra: " + e.getMessage());
                                                        if (pending.decrementAndGet() == 0)
                                                            applyLeftoverItems(leftoverItems,
                                                                    generatedLeftovers,
                                                                    weekDates, template, callback);
                                                    }
                                                });
                                    } else {
                                        if (pending.decrementAndGet() == 0)
                                            applyLeftoverItems(leftoverItems,
                                                    generatedLeftovers, weekDates, template, callback);
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

    // Step two: consume template leftover rows using Leftovers created in applyMainItems
    private void applyLeftoverItems(List<WeeklyMenuItem> leftoverItems,
                                    Map<String, Leftover> generatedLeftovers,
                                    List<Date> weekDates,
                                    WeeklyMenuTemplate template,
                                    OnTemplateApplied callback) {
        if (leftoverItems.isEmpty()) {
            applyUnassignedLeftovers(template, callback);
            return;
        }

        AtomicInteger pending = new AtomicInteger(leftoverItems.size());

        for (WeeklyMenuItem item : leftoverItems) {
            // Obtains one day between 1 and 7 and adapts it
            int dayIndex = item.getDayOfWeek() - 1;
            if (dayIndex < 0 || dayIndex >= weekDates.size()) {
                if (pending.decrementAndGet() == 0)
                    applyUnassignedLeftovers(template, callback);
                continue;
            }

            Date dayDate = weekDates.get(dayIndex);
            // Obtains the leftover from the map
            Leftover leftover = generatedLeftovers.get(item.getSourceRecipeId());

            if (leftover == null) {
                if (pending.decrementAndGet() == 0)
                    applyUnassignedLeftovers(template, callback);
                continue;
            }
            // Creates the menu for the leftover
            Menu menu = new Menu(null, item.getRecipeId(), dayDate, 1,
                    "", false, true, item.getSourceRecipeId(), leftover.getSourceMenuId(),
                    leftover.getPerishable(), leftover.getValidDays());
            // Persists the menu
            menuRepository.addMenu(menu, new MenuRepository.OnMenuAdded() {
                @Override
                public void onSuccess(String menuId) {
                    // Decrements the leftover portions
                    int newRemaining = leftover.getRemainingPortions() - 1;
                    leftover.setRemainingPortions(newRemaining);
                    // Delete the leftover if no portions remain
                    if (newRemaining <= 0) {
                        leftoverRepository.deleteLeftover(leftover.getId(),
                                new LeftoverRepository.LeftoverCallback() {
                                    @Override public void onSuccess() {
                                        if (pending.decrementAndGet() == 0)
                                            applyUnassignedLeftovers(template, callback);
                                    }
                                    @Override public void onFailure(Exception e) {
                                        if (pending.decrementAndGet() == 0)
                                            applyUnassignedLeftovers(template, callback);
                                    }
                                });
                    } else {
                        // Updates the leftover if there are still portions
                        leftoverRepository.updateLeftover(leftover.getId(), leftover,
                                new LeftoverRepository.LeftoverCallback() {
                                    @Override public void onSuccess() {
                                        if (pending.decrementAndGet() == 0)
                                            applyUnassignedLeftovers(template, callback);
                                    }
                                    @Override public void onFailure(Exception e) {
                                        if (pending.decrementAndGet() == 0)
                                            applyUnassignedLeftovers(template, callback);
                                    }
                                });
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    errorMessage.postValue("Error creando menu de sobra: " + e.getMessage());
                    if (pending.decrementAndGet() == 0)
                        applyUnassignedLeftovers(template, callback);
                }
            });
        }
    }

    // Step three
    private void applyUnassignedLeftovers(WeeklyMenuTemplate template,
                                          OnTemplateApplied callback) {
        int totalListed = 0;
        List<UnassignedLeftover> unassigned = template.getUnassignedLeftovers();
        if (unassigned != null) {
            for (UnassignedLeftover u : unassigned) {
                totalListed += u.getRemainingPortions();
            }
        }

        reloadCurrentWeek();
        if (callback != null) {
            callback.onComplete(totalListed);
        }
    }

    // Refreshes UI state after changes
    public void reloadCurrentWeek() {
        Date monday = selectedWeekStart.getValue();
        if (monday != null) loadWeek(monday);
    }
}