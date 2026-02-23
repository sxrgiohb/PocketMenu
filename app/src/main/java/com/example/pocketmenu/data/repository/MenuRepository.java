package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.Menu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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

    // ===========================
    // CALLBACKS
    // ===========================
    public interface MenuCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Devuelve el ID del documento creado por Firestore
    public interface OnMenuAdded {
        void onSuccess(String menuId);
        void onFailure(Exception e);
    }

    public interface OnMenuFound {
        void onFound(Menu menu);
        void onNotFound();
        void onFailure(Exception e);
    }

    // ===========================
    // QUERY MENUS POR USUARIO
    // ===========================
    public Query getMenusQuery() {
        String uid = getUserId();
        if (uid == null) return db.collection(COLLECTION_PATH).limit(0);
        return db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .orderBy("date");
    }

    // ===========================
    // QUERY POR FECHA
    // ===========================
    public Query getMenusByDateQuery(java.util.Date date) {
        String uid = getUserId();
        if (uid == null) return db.collection(COLLECTION_PATH).limit(0);
        return db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("date", date)
                .orderBy("name");
    }

    // ===========================
    // FIND MENU BY ID
    // ===========================
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

    // ===========================
    // ADD MENU — versión original, mantenida por compatibilidad
    // ===========================
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

    // ===========================
    // ADD MENU — devuelve el ID generado por Firestore
    // Usar esta cuando se necesite el ID para crear Leftovers
    // ===========================
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

    // ===========================
    // UPDATE MENU
    // ===========================
    public void updateMenu(String menuId, Menu menu, MenuCallback callback) {
        db.collection(COLLECTION_PATH).document(menuId).set(menu)
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    // ===========================
    // DELETE MENU
    // ===========================
    public void deleteMenu(String menuId, MenuCallback callback) {
        db.collection(COLLECTION_PATH).document(menuId).delete()
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }
}