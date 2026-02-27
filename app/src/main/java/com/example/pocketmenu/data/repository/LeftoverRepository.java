package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.Leftover;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LeftoverRepository {

    public static final String COLLECTION_PATH = "LEFTOVERS";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public LeftoverRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String getUserId() {
        return auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : null;
    }

    public interface LeftoverCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnLeftoverFound {
        void onFound(Leftover leftover);
        void onNotFound();
        void onFailure(Exception e);
    }

    public interface OnLeftoversLoaded {
        void onLoaded(List<Leftover> leftovers);
        void onFailure(Exception e);
    }

    public Query getLeftoversQuery() {
        String uid = getUserId();
        if (uid == null) return db.collection(COLLECTION_PATH).limit(0);
        return db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .orderBy("firstAssignedDate");
    }

    public Query getValidPerishableLeftoversQuery() {
        String uid = getUserId();
        if (uid == null) return db.collection(COLLECTION_PATH).limit(0);
        Date now = new Date();
        return db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("perishable", true)
                .whereGreaterThanOrEqualTo("expirationDate", now)
                .orderBy("expirationDate");
    }

    public Query getNonPerishableLeftoversQuery() {
        String uid = getUserId();
        if (uid == null) return db.collection(COLLECTION_PATH).limit(0);
        return db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("perishable", false)
                .orderBy("firstAssignedDate");
    }


    public void getLeftoversByRecipe(String recipeId, OnLeftoversLoaded callback) {
        String uid = getUserId();
        if (uid == null) {
            callback.onFailure(new Exception("Usuario no autenticado"));
            return;
        }
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("recipeId", recipeId)
                .get()
                .addOnSuccessListener(snap ->
                        callback.onLoaded(snap.toObjects(Leftover.class)))
                .addOnFailureListener(callback::onFailure);
    }

    public void getActiveLeftoverByRecipe(String recipeId, OnLeftoverFound callback) {
        String uid = getUserId();
        if (uid == null) {
            callback.onFailure(new Exception("Usuario no autenticado"));
            return;
        }
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("recipeId", recipeId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty())
                        callback.onFound(snap.getDocuments().get(0).toObject(Leftover.class));
                    else
                        callback.onNotFound();
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getValidLeftovers(OnLeftoversLoaded callback) {
        String uid = getUserId();
        if (uid == null) {
            callback.onFailure(new Exception("Usuario no autenticado"));
            return;
        }
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    android.util.Log.d("LEFTOVERS_RAW", "Documentos en Firestore: " + snap.size());
                    for (Leftover l : snap.toObjects(Leftover.class)) {
                        android.util.Log.d("LEFTOVERS_RAW", "Raw: recipeId=" + l.getRecipeId()
                                + " perishable=" + l.getPerishable()
                                + " validDays=" + l.getValidDays()
                                + " firstAssignedDate=" + l.getFirstAssignedDate()
                                + " isStillValid=" + isStillValid(l, new Date()));
                    }
                    List<Leftover> valid = new ArrayList<>();
                    Date now = new Date();
                    for (Leftover l : snap.toObjects(Leftover.class)) {
                        if (isStillValid(l, now)) valid.add(l);
                    }
                    callback.onLoaded(valid);
                })
                .addOnFailureListener(callback::onFailure);
    }


    public void getLeftoversByDateRange(Date from, Date to, OnLeftoversLoaded callback) {
        String uid = getUserId();
        if (uid == null) {
            callback.onFailure(new Exception("Usuario no autenticado"));
            return;
        }
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereGreaterThanOrEqualTo("firstAssignedDate", from)
                .whereLessThanOrEqualTo("firstAssignedDate", to)
                .get()
                .addOnSuccessListener(snap ->
                        callback.onLoaded(snap.toObjects(Leftover.class)))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteLeftoversByDateRange(Date from, Date to, LeftoverCallback callback) {
        String uid = getUserId();
        if (uid == null) return;
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereGreaterThanOrEqualTo("firstAssignedDate", from)
                .whereLessThanOrEqualTo("firstAssignedDate", to)
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

    public void addLeftover(Leftover leftover, LeftoverCallback callback) {
        String uid = getUserId();
        if (uid == null) {
            if (callback != null) callback.onFailure(new Exception("Usuario no autenticado"));
            return;
        }
        leftover.setUserId(uid);
        db.collection(COLLECTION_PATH)
                .add(leftover)
                .addOnSuccessListener(ref -> {
                    leftover.setId(ref.getId());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    public void updateLeftover(String leftoverId, Leftover leftover, LeftoverCallback callback) {
        db.collection(COLLECTION_PATH).document(leftoverId).set(leftover)
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    public void deleteLeftover(String leftoverId, LeftoverCallback callback) {
        db.collection(COLLECTION_PATH).document(leftoverId).delete()
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    public static boolean isStillValid(Leftover leftover, Date now) {
        if (!leftover.getPerishable()) return true;
        if (leftover.getFirstAssignedDate() == null) return true;
        long msPerDay = 24L * 60 * 60 * 1000;
        long expirationMs = leftover.getFirstAssignedDate().getTime()
                + (long) leftover.getValidDays() * msPerDay;
        return expirationMs >= now.getTime();
    }

    public void deleteExpiredPerishableLeftovers(LeftoverCallback callback) {
        String uid = getUserId();
        if (uid == null) return;
        Date now = new Date();
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("perishable", true)
                .get()
                .addOnSuccessListener(snap -> {
                    List<com.google.firebase.firestore.DocumentSnapshot> expired = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        Leftover l = doc.toObject(Leftover.class);
                        if (l != null && !isStillValid(l, now)) {
                            expired.add(doc);
                        }
                    }
                    if (expired.isEmpty()) {
                        if (callback != null) callback.onSuccess();
                        return;
                    }
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : expired) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(a -> { if (callback != null) callback.onSuccess(); })
                            .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
                })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    public void deleteLeftoversBySourceMenuId(String sourceMenuId, LeftoverCallback callback) {
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
}