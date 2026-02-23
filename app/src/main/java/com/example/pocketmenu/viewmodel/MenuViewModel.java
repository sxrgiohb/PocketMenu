package com.example.pocketmenu.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketmenu.data.model.Leftover;
import com.example.pocketmenu.data.model.Menu;
import com.example.pocketmenu.data.model.Recipe;
import com.example.pocketmenu.data.model.WeeklyMenuTemplate;
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
                            checkIfWeekLoadComplete(completedDays, totalDays, weekDates, assignmentsByDay);
                            return;
                        }
                        AtomicInteger completedMenus = new AtomicInteger(0);
                        List<MenuAssignment> dayAssignments = new ArrayList<>();
                        for (Menu menu : menus) {
                            fetchAssignmentForMenu(menu, dayAssignments, completedMenus, menus.size(), () -> {
                                assignmentsByDay.put(dayIndex, dayAssignments);
                                checkIfWeekLoadComplete(completedDays, totalDays, weekDates, assignmentsByDay);
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        errorMessage.postValue("Error cargando día " + (dayIndex + 1) + ": " + e.getMessage());
                        assignmentsByDay.put(dayIndex, new ArrayList<>());
                        checkIfWeekLoadComplete(completedDays, totalDays, weekDates, assignmentsByDay);
                    });
        }
    }

    private void fetchAssignmentForMenu(Menu menu,
                                        List<MenuAssignment> dayAssignments,
                                        AtomicInteger completedMenus,
                                        int totalMenus,
                                        Runnable onAllMenusDone) {
        recipeRepository.getRecipeById(menu.getRecipeId(), new RecipeRepository.OnRecipeFound() {

            @Override
            public void onFound(Recipe recipe) {
                leftoverRepository.getActiveLeftoverByRecipe(recipe.getId(),
                        new LeftoverRepository.OnLeftoverFound() {

                            @Override
                            public void onFound(Leftover leftover) {
                                Leftover associated = leftover.getSourceMenuId() != null
                                        && leftover.getSourceMenuId().equals(menu.getId())
                                        ? leftover : null;
                                if (associated != null
                                        && !LeftoverRepository.isStillValid(associated, new Date())) {
                                    associated = null;
                                }
                                addAndCheck(new MenuAssignment(menu, recipe, associated));
                            }

                            @Override
                            public void onNotFound() {
                                addAndCheck(new MenuAssignment(menu, recipe, null));
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
    public void loadValidLeftovers() {
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
                    recipeRepository.getRecipeById(leftover.getRecipeId(),
                            new RecipeRepository.OnRecipeFound() {
                                @Override
                                public void onFound(Recipe recipe) {
                                    synchronized (result) {
                                        result.add(new LeftoverWithRecipe(leftover, recipe));
                                    }
                                    if (pending.decrementAndGet() == 0)
                                        validLeftovers.postValue(result);
                                }
                                @Override
                                public void onNotFound() {
                                    if (pending.decrementAndGet() == 0)
                                        validLeftovers.postValue(result);
                                }
                                @Override
                                public void onFailure(Exception e) {
                                    if (pending.decrementAndGet() == 0)
                                        validLeftovers.postValue(result);
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
    public void assignRecipeToDay(Recipe recipe, Date day, boolean isPerishable, int validDays) {
        Date normalizedDay = normalizeDate(day);
        int usedPortions = 1;
        int leftoverPortions = recipe.getPortion() - usedPortions;
        Menu menu = new Menu(null, recipe.getId(), normalizedDay, usedPortions, recipe.getName(), false);

        menuRepository.addMenu(menu, new MenuRepository.OnMenuAdded() {
            @Override
            public void onSuccess(String menuId) {
                if (leftoverPortions > 0) {
                    createLeftover(recipe, menuId, normalizedDay, leftoverPortions, isPerishable, validDays);
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
                null,
                recipe.getId(),
                sourceMenuId,
                portions,
                isPerishable,
                assignDate,
                isPerishable ? validDays : 0
        );
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
        Menu menu = new Menu(null, recipe.getId(), normalizedDay, 1, recipe.getName(), false);

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
                                    errorMessage.postValue("Error eliminando sobra: " + e.getMessage());
                                }
                            });
                } else {
                    leftoverRepository.updateLeftover(leftover.getId(), leftover,
                            new LeftoverRepository.LeftoverCallback() {
                                @Override public void onSuccess() { reloadCurrentWeek(); }
                                @Override public void onFailure(Exception e) {
                                    errorMessage.postValue("Error actualizando sobra: " + e.getMessage());
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
                Leftover leftover = assignment.getLeftover();
                if (leftover != null && leftover.getId() != null) {
                    leftoverRepository.deleteLeftover(leftover.getId(),
                            new LeftoverRepository.LeftoverCallback() {
                                @Override public void onSuccess() { reloadCurrentWeek(); }
                                @Override public void onFailure(Exception e) {
                                    errorMessage.postValue("Menú eliminado pero error en sobras: " + e.getMessage());
                                    reloadCurrentWeek();
                                }
                            });
                } else {
                    reloadCurrentWeek();
                }
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue("Error eliminando asignación: " + e.getMessage());
            }
        });
    }

    // ===========================
    // FAVORITOS
    // ===========================
    public void saveCurrentWeekAsFavorite(String name) {
        List<DayMenuWrapper> currentDays = weekDays.getValue();
        if (currentDays == null || currentDays.isEmpty()) {
            errorMessage.setValue("No hay menú para guardar.");
            return;
        }
        List<com.example.pocketmenu.data.model.WeeklyMenuItem> items = new ArrayList<>();
        for (DayMenuWrapper day : currentDays) {
            for (MenuAssignment assignment : day.getAssignments()) {
                items.add(new com.example.pocketmenu.data.model.WeeklyMenuItem(
                        day.getDayOfWeek(),
                        assignment.getRecipe().getId(),
                        assignment.getMenu().getUsedPortions()));
            }
        }
        WeeklyMenuTemplate template = new WeeklyMenuTemplate(null, null, name, true, items);
        templateRepository.addTemplate(template, new WeeklyMenuTemplateRepository.WeeklyMenuCallback() {
            @Override public void onSuccess() { }
            @Override public void onFailure(Exception e) {
                errorMessage.postValue("Error guardando favorito: " + e.getMessage());
            }
        });
    }

    public void applyTemplate(WeeklyMenuTemplate template) {
        Date monday = selectedWeekStart.getValue();
        if (monday == null || template.getItems() == null) return;
        List<Date> weekDates = getWeekDates(monday);
        for (com.example.pocketmenu.data.model.WeeklyMenuItem item : template.getItems()) {
            int dayIndex = item.getDayOfWeek() - 1;
            if (dayIndex < 0 || dayIndex >= weekDates.size()) continue;
            Menu menu = new Menu(null, item.getRecipeId(), weekDates.get(dayIndex),
                    item.getPortions(), "", false);
            menuRepository.addMenu(menu, new MenuRepository.MenuCallback() {
                @Override public void onSuccess() { reloadCurrentWeek(); }
                @Override public void onFailure(Exception e) {
                    errorMessage.postValue("Error aplicando template: " + e.getMessage());
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