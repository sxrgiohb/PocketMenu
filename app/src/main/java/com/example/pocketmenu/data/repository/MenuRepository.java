package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.Menu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.Calendar;
import java.util.Date;

// Firestore access for meal assignments: one document per planned meal.
public class MenuRepository {

    public static final String COLLECTION_PATH = "MENUS";

    // Firebase instances
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    // Constructor
    public MenuRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String getUserId() {
        return auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : null;
    }

    // Generic interface
    public interface MenuCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Returns the new document id
    public interface OnMenuAdded {
        void onSuccess(String menuId);
        void onFailure(Exception e);
    }

    // Search interface
    public interface OnMenuFound {
        void onFound(Menu menu);
        void onNotFound();
        void onFailure(Exception e);
    }

    // Query for a single calendar day (used by MenuViewModel when building the week).
    public Query getMenusByDateQuery(Date date) {
        String uid = getUserId();
        if (uid == null) return db.collection(COLLECTION_PATH).limit(0);
        return db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("date", date);
    }

    public void getMenuById(String menuId, OnMenuFound callback) {
        db.collection(COLLECTION_PATH)
                .document(menuId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) callback.onFound(doc.toObject(Menu.class));
                    else callback.onNotFound();
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void addMenu(Menu menu, MenuCallback callback) {
        addMenu(menu, new OnMenuAdded() {
            @Override public void onSuccess(String menuId) {
                if (callback != null) callback.onSuccess();
            }
            @Override public void onFailure(Exception e) {
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    public void addMenu(Menu menu, OnMenuAdded callback) {
        String uid = getUserId();
        if (uid == null) {
            if (callback != null) callback.onFailure(new Exception("Usuario no autenticado"));
            return;
        }
        menu.setUserId(uid);
        db.collection(COLLECTION_PATH)
                .add(menu)
                .addOnSuccessListener(ref -> {
                    menu.setId(ref.getId());
                    if (callback != null) callback.onSuccess(ref.getId());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void deleteMenu(String menuId, MenuCallback callback) {
        db.collection(COLLECTION_PATH).document(menuId).delete()
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    // Deletes meals that were cooked from leftovers tied to a source menu
    public void deleteMenusBySourceMenuId(String sourceMenuId, MenuCallback callback) {
        String uid = getUserId();
        if (uid == null) return;
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("sourceMenuId", sourceMenuId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        if (callback != null) callback.onSuccess();
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        batch.delete(doc.getReference());
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

    // Wipes all menus whose date falls in the given range (inclusive)
    // Used when applying a favorite template over the current week.
    public void deleteMenusByDateRange(Date from, Date to, MenuCallback callback) {
        String uid = getUserId();
        if (uid == null) return;
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereGreaterThanOrEqualTo("date", from)
                .whereLessThanOrEqualTo("date", to)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        if (callback != null) callback.onSuccess();
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(a -> { if (callback != null) callback.onSuccess(); })
                            .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
                })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    // Removes menus older than x days from today
    public void deleteMenusOlderThan(int days, MenuCallback callback) {
        String uid = getUserId();
        if (uid == null) return;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date cutoffDate = cal.getTime();

        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereLessThan("date", cutoffDate)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        if (callback != null) callback.onSuccess();
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(a -> { if (callback != null) callback.onSuccess(); })
                            .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
                })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }
}
