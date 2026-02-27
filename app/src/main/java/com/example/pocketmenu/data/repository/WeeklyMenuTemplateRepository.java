package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.WeeklyMenuTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

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

    public interface OnTemplatesLoaded {
        void onLoaded(List<WeeklyMenuTemplate> templates);
        void onFailure(Exception e);
    }
    public Query getTemplatesQuery() {
        String uid = getUserId();
        if (uid == null) return db.collection(COLLECTION_PATH).limit(0);
        return db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .orderBy("name");
    }

    public void getTemplateById(String templateId, OnWeeklyMenuFound callback) {
        db.collection(COLLECTION_PATH)
                .document(templateId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) callback.onFound(doc.toObject(WeeklyMenuTemplate.class));
                    else callback.onNotFound();
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getAllTemplates(OnTemplatesLoaded callback) {
        String uid = getUserId();
        if (uid == null) {
            callback.onFailure(new Exception("Usuario no autenticado"));
            return;
        }
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .orderBy("name")
                .get()
                .addOnSuccessListener(snap ->
                        callback.onLoaded(snap.toObjects(WeeklyMenuTemplate.class)))
                .addOnFailureListener(callback::onFailure);
    }

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
                    template.setId(ref.getId());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void updateTemplate(String templateId, WeeklyMenuTemplate template,
                               WeeklyMenuCallback callback) {
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