package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.Leftover;
import com.example.pocketmenu.data.model.WeeklyMenuTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class WeeklyMenuTemplateRepository {

    public static final String COLLECTION_PATH = "WEEKLY_MENU_TEMPLATES";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public WeeklyMenuTemplateRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String getUserId() {
        return auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : null;
    }

    public interface WeeklyMenuCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnWeeklyMenuFound {
        void onFound(WeeklyMenuTemplate template);
        void onNotFound();
        void onFailure(Exception e);
    }

    // GET ALL TEMPLATES (Query)
    public Query getTemplatesQuery() {
        String uid = getUserId();
        if (uid == null) return db.collection(COLLECTION_PATH).limit(0);

        return db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .orderBy("name");
    }

    // FIND TEMPLATE BY ID
    public void getTemplateById(String templateId, OnWeeklyMenuFound callback) {
        db.collection(COLLECTION_PATH)
                .document(templateId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        WeeklyMenuTemplate template = doc.toObject(WeeklyMenuTemplate.class);
                        callback.onFound(template);
                    } else {
                        callback.onNotFound();
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ADD TEMPLATE
    public void addTemplate(WeeklyMenuTemplate template, WeeklyMenuCallback callback) {
        String uid = getUserId();
        if (uid == null) {
            if (callback != null) callback.onFailure(new Exception("Usuario no autenticado"));
            return;
        }
        template.setUserId(uid);

        db.collection(COLLECTION_PATH)
                .add(template)
                .addOnSuccessListener(ref -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // UPDATE TEMPLATE
    public void updateTemplate(String templateId, WeeklyMenuTemplate template, WeeklyMenuCallback callback) {
        db.collection(COLLECTION_PATH)
                .document(templateId)
                .set(template)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // DELETE TEMPLATE
    public void deleteTemplate(String templateId, WeeklyMenuCallback callback) {
        db.collection(COLLECTION_PATH)
                .document(templateId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }
}