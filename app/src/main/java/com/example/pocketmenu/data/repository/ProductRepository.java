package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProductRepository {

    public static final String COLLECTION_PATH = "PRODUCTS";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ProductRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String getUserId() {
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return null;
    }

    // Callbacks
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

    // Añadir producto
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

    // Obtener todos los productos del usuario (para autocompletado)
    public void getAllProducts(OnProductsLoaded callback) {
        String uid = getUserId();
        if (uid == null) return;
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .orderBy("name")
                .get()
                .addOnSuccessListener(snap -> {
                    if (callback != null)
                        callback.onLoaded(snap.toObjects(Product.class));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // Buscar producto por nombre exacto (para autocompletado al escribir)
    public void getProductByName(String name, OnProductFound callback) {
        String uid = getUserId();
        if (uid == null) return;
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .whereEqualTo("name", name)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        if (callback != null) callback.onNotFound();
                    } else {
                        if (callback != null)
                            callback.onFound(snap.getDocuments().get(0).toObject(Product.class));
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // Buscar productos cuyo nombre empiece por el texto introducido
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

    // Actualizar producto (por si el usuario cambia unidad, tienda, etc.)
    public void updateProduct(String productId, Product product, ProductCallback callback) {
        db.collection(COLLECTION_PATH)
                .document(productId)
                .set(product)
                .addOnSuccessListener(a -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // Eliminar producto
    public void deleteProduct(String productId, ProductCallback callback) {
        db.collection(COLLECTION_PATH)
                .document(productId)
                .delete()
                .addOnSuccessListener(a -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }
}