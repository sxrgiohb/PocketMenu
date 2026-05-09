package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

// User product catalog for shopping-list autocomplete.
public class ProductRepository {

    public static final String COLLECTION_PATH = "PRODUCTS";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    // Constructor
    public ProductRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String getUserId() {
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return null;
    }

    public interface ProductCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnProductAdded {
        void onSuccess(String productId);
        void onFailure(Exception e);
    }

    public interface OnProductsLoaded {
        void onLoaded(List<Product> products);
        void onFailure(Exception e);
    }

    public interface OnProductFound {
        void onFound(Product product);
        void onNotFound();
        void onFailure(Exception e);
    }

    /** Persist a new product when the user adds an ingredient not in the catalog. */
    public void addProduct(Product product, OnProductAdded callback) {
        String uid = getUserId();
        if (uid == null) return;
        product.setUserId(uid);
        db.collection(COLLECTION_PATH)
                .add(product)
                .addOnSuccessListener(ref -> {
                    if (callback != null) callback.onSuccess(ref.getId());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }


    // Prefix search for the shopping list autocomplete (used by ShoppingListViewModel)
    public void searchProductsByName(String prefix, OnProductsLoaded callback) {
        String uid = getUserId();
        if (uid == null) return;
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .orderBy("name")
                .startAt(prefix)
                .endAt(prefix + '\uf8ff')
                .get()
                .addOnSuccessListener(snap -> {
                    if (callback != null)
                        callback.onLoaded(snap.toObjects(Product.class));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

}
