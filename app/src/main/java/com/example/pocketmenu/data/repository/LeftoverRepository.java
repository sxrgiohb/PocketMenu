package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.Leftover;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//Firestore access for Leftover documents (extra portions linked to a source menu).
public class LeftoverRepository {

    public static final String COLLECTION_PATH = "LEFTOVERS";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    // Constructor
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


    /** All leftover rows sharing a recipe id (MenuViewModel joins with menus). */
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

    // All leftovers for the user, filtered in memory to those still valid
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
                    List<Leftover> valid = new ArrayList<>();
                    Date now = new Date();
                    for (Leftover l : snap.toObjects(Leftover.class)) {
                        if (isStillValid(l, now)) valid.add(l);
                    }
                    callback.onLoaded(valid);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Clears leftovers whose first assignment falls in the week being replaced.
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

    private static long expirationEndMillis(Leftover leftover) {
        long msPerDay = 24L * 60 * 60 * 1000;
        return leftover.getFirstAssignedDate().getTime()
                + (long) leftover.getValidDays() * msPerDay;
    }

    // Perishable safety check at a given instant (also used by cleanup jobs)
    public static boolean isStillValid(Leftover leftover, Date referenceTime) {
        if (!leftover.getPerishable()) return true;
        if (leftover.getFirstAssignedDate() == null) return true;
        return expirationEndMillis(leftover) >= referenceTime.getTime();
    }

    // Controls that a leftover can be assigned to a given day
    public static boolean isAssignableOnDay(Leftover leftover, Date dayInstant) {
        if (!leftover.getPerishable()) return true;
        if (leftover.getFirstAssignedDate() == null) return true;
        if (leftover.getValidDays() <= 0) return true;
        return expirationEndMillis(leftover) >= dayInstant.getTime();
    }

    // Deletes perishable rows that are no longer valid (expired)
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

    // Removes leftovers created from a specific source menu id
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
