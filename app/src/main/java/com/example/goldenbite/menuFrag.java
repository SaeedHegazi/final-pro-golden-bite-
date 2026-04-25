package com.example.goldenbite;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class menuFrag extends Fragment {

    private static final List<String> MENU_CATEGORIES = Arrays.asList(
            "Hot drinks",
            "Cold drinks",
            "Pancakes"
    );

    private RecyclerView productsRecycler;
    private TextView productsSectionTitle;
    private final List<Product> products = new ArrayList<>();
    private CustomerProductsAdapter productsAdapter;

    public menuFrag() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        RecyclerView categoriesRecycler = view.findViewById(R.id.menu_categories_recycler);
        categoriesRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        MenuCategoryAdapter categoryAdapter = new MenuCategoryAdapter(MENU_CATEGORIES, category -> {
            productsSectionTitle.setVisibility(View.VISIBLE);
            productsSectionTitle.setText(category);
            loadProductsForCategory(category);
        });
        categoriesRecycler.setAdapter(categoryAdapter);

        productsSectionTitle = view.findViewById(R.id.products_section_title);
        productsRecycler = view.findViewById(R.id.menu_products_recycler);
        productsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        productsAdapter = new CustomerProductsAdapter(products, product ->
                AddToCartDialogFragment.newInstance(product)
                        .show(getParentFragmentManager(), "addToCart"));
        productsRecycler.setAdapter(productsAdapter);

        return view;
    }

    private void loadProductsForCategory(String category) {
        FirebaseFirestore.getInstance()
                .collection("Product")
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(getActivity(), snapshot -> {
                    products.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Long sizeLong = doc.getLong("size");
                        int size = sizeLong != null ? sizeLong.intValue() : 0;
                        if (size == 0) continue; // removed from menu

                        String docId = doc.getId();
                        String name = doc.getString("name");
                        Long priceLong = doc.getLong("price");
                        String description = doc.getString("description");
                        if (description == null) description = doc.getString("descirption");
                        String imagUrl = doc.getString("imagUrl");
                        String cat = doc.getString("category");

                        int price = priceLong != null ? priceLong.intValue() : 0;
                        Product p = new Product(docId, name != null ? name : "", price, size,
                                description != null ? description : "", imagUrl != null ? imagUrl : "", cat);
                        products.add(p);
                    }
                    productsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(getActivity(), e -> {
                    products.clear();
                    productsAdapter.notifyDataSetChanged();
                });
    }

    private interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    private static class MenuCategoryAdapter extends RecyclerView.Adapter<MenuCategoryAdapter.ViewHolder> {
        private final List<String> categories;
        private final OnCategoryClickListener listener;

        MenuCategoryAdapter(List<String> categories, OnCategoryClickListener listener) {
            this.categories = categories;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_menu_category, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String category = categories.get(position);
            holder.textView.setText(category);
            holder.itemView.setOnClickListener(v -> listener.onCategoryClick(category));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.category_name);
            }
        }
    }

    private interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private static class CustomerProductsAdapter extends RecyclerView.Adapter<CustomerProductsAdapter.ViewHolder> {
        private final List<Product> list;
        private final OnProductClickListener productClickListener;

        CustomerProductsAdapter(List<Product> list, OnProductClickListener productClickListener) {
            this.list = list;
            this.productClickListener = productClickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product p = list.get(position);
            holder.name.setText(p.getName());
            holder.description.setText(p.getDescription());
            holder.price.setText(String.valueOf(p.getPrice()));
            String url = p.getImagUrl();
            if (url != null && !url.isEmpty()) {
                Glide.with(holder.image.getContext()).load(url).centerCrop().into(holder.image);
            } else {
                holder.image.setImageDrawable(null);
            }
            holder.itemView.setOnClickListener(v -> productClickListener.onProductClick(p));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView image;
            final TextView name, price, description;

            ViewHolder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.product_image);
                name = itemView.findViewById(R.id.product_name);
                price = itemView.findViewById(R.id.product_price);
                description = itemView.findViewById(R.id.product_description);
            }
        }
    }
}
