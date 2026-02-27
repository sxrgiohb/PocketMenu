package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.Menu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.Date;

public class MenuRepository {

    public static final String COLLECTION_PATH = "MENUS";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public MenuRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String getUserId() {
        return auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : null;
    }

    public interface MenuCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnMenuAdded {
        void onSuccess(String menuId);
        void onFailure(Exception e);
    }

    public interface OnMenuFound {
        void onFound(Menu menu);
        void onNotFound();
        void onFailure(Exception e);
    }

    public Query getMenusQuery() {
        String uid = getUserId();
        if (uid == null) return db.collection(COLLECTION_PATH).limit(0);
        return db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .orderBy("date");
    }

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

    public void updateMenu(String menuId, Menu menu, MenuCallback callback) {
        db.collection(COLLECTION_PATH).document(menuId).set(menu)
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    public void deleteMenu(String menuId, MenuCallback callback) {
        db.collection(COLLECTION_PATH).document(menuId).delete()
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    /**
     * Elimina todos los menus que consumieron sobras de un menu concreto.
     * Usado al eliminar una receta principal para limpiar también
     * las asignaciones de sus sobras.
     */
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
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
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

    /**
     * Elimina todos los menus de una semana concreta.
     * Usado al aplicar una plantilla favorita para reemplazar la semana.
     */
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
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(a -> { if (callback != null) callback.onSuccess(); })
                            .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
                })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

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
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(a -> { if (callback != null) callback.onSuccess(); })
                            .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
                })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }
}