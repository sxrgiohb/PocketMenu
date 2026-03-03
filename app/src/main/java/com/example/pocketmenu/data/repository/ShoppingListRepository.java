package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.Menu;
import com.example.pocketmenu.data.model.ShoppingListItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShoppingListRepository {

    public static final String COLLECTION_PATH = "SHOPPING_LIST_ITEMS";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ShoppingListRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String getUserId() {
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return null;
    }

    // ===========================
    // WEEK ID UTILS
    // ===========================

    public static String getWeekId(Date date) {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.setMinimalDaysInFirstWeek(4);
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setTime(date);
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        int year = cal.get(Calendar.YEAR);
        return String.format(Locale.getDefault(), "%04d-W%02d", year, week);
    }

    public static Date getMondayFromWeekId(String weekId) {
        String[] parts = weekId.split("-W");
        int year = Integer.parseInt(parts[0]);
        int week = Integer.parseInt(parts[1]);

        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.setMinimalDaysInFirstWeek(4);
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.WEEK_OF_YEAR, week);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    // ===========================
    // CALLBACKS
    // ===========================

    public interface ShoppingListCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnItemAdded {
        void onSuccess(String itemId);
        void onFailure(Exception e);
    }

    public interface OnItemsLoaded {
        void onLoaded(List<ShoppingListItem> items);
        void onFailure(Exception e);
    }

    public interface OnMenusLoaded {
        void onLoaded(List<Menu> menus);
        void onFailure(Exception e);
    }

    // ===========================
    // SHOPPING LIST ITEMS
    // ===========================

    public void addItem(ShoppingListItem item, OnItemAdded callback) {
        String uid = getUserId();
        if (uid == null) return;
        item.setUserId(uid);
        db.collection(COLLECTION_PATH)
                .add(item)
                .addOnSuccessListener(ref -> {
                    if (callback != null) callback.onSuccess(ref.getId());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void addItems(List<ShoppingListItem> items, ShoppingListCallback callback) {
        if (items == null || items.isEmpty()) {
            if (callback != null) callback.onSuccess();
            return;
        }
        String uid = getUserId();
        if (uid == null) return;

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        for (ShoppingListItem item : items) {
            item.setUserId(uid);
            batch.set(db.collection(COLLECTION_PATH).document(), item);
        }
        batch.commit()
                .addOnSuccessListener(a -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void getItemsByWeekId(String weekId, OnItemsLoaded callback) {
        String uid = getUserId();
        if (uid == null) return;
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("weekId", weekId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (callback != null)
                        callback.onLoaded(snap.toObjects(ShoppingListItem.class));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void getItemsByWeekIds(List<String> weekIds, OnItemsLoaded callback) {
        String uid = getUserId();
        if (uid == null) return;
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereIn("weekId", weekIds)
                .get()
                .addOnSuccessListener(snap -> {
                    if (callback != null)
                        callback.onLoaded(snap.toObjects(ShoppingListItem.class));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void updateItem(ShoppingListItem item, ShoppingListCallback callback) {
        if (item.getId() == null) return;
        db.collection(COLLECTION_PATH)
                .document(item.getId())
                .set(item)
                .addOnSuccessListener(a -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void deleteItem(String itemId, ShoppingListCallback callback) {
        db.collection(COLLECTION_PATH)
                .document(itemId)
                .delete()
                .addOnSuccessListener(a -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void deleteItemsByWeekId(String weekId, ShoppingListCallback callback) {
        String uid = getUserId();
        if (uid == null) return;
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("weekId", weekId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        if (callback != null) callback.onSuccess();
                        return;
                    }
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    snap.getDocuments().forEach(doc -> batch.delete(doc.getReference()));
                    batch.commit()
                            .addOnSuccessListener(a -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void deleteRecipeItemsByWeekId(String weekId, ShoppingListCallback callback) {
        String uid = getUserId();
        if (uid == null) return;
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("weekId", weekId)
                .whereEqualTo("isExtra", false)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        if (callback != null) callback.onSuccess();
                        return;
                    }
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    snap.getDocuments().forEach(doc -> batch.delete(doc.getReference()));
                    batch.commit()
                            .addOnSuccessListener(a -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void deletePastWeeks(ShoppingListCallback callback) {
        String uid = getUserId();
        if (uid == null) return;

        String currentWeekId = getWeekId(new Date());

        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        if (callback != null) callback.onSuccess();
                        return;
                    }
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    boolean hasDeletions = false;
                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : snap.getDocuments()) {
                        String weekId = doc.getString("weekId");
                        if (weekId != null && weekId.compareTo(currentWeekId) < 0) {
                            batch.delete(doc.getReference());
                            hasDeletions = true;
                        }
                    }
                    if (!hasDeletions) {
                        if (callback != null) callback.onSuccess();
                        return;
                    }
                    batch.commit()
                            .addOnSuccessListener(a -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // ===========================
    // MENUS (lectura para generación de lista)
    // ===========================

    public void getMainMenusByDateRange(Date from, Date to, OnMenusLoaded callback) {
        String uid = getUserId();
        if (uid == null) return;
        db.collection(MenuRepository.COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("isFromLeftover", false)
                .whereGreaterThanOrEqualTo("date", from)
                .whereLessThanOrEqualTo("date", to)
                .get()
                .addOnSuccessListener(snap -> {
                    if (callback != null)
                        callback.onLoaded(snap.toObjects(Menu.class));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }
}