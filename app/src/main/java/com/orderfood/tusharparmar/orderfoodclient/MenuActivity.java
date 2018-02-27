package com.orderfood.tusharparmar.orderfoodclient;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

public class MenuActivity extends AppCompatActivity {
    RecyclerView mItemList;
    DatabaseReference mDB;
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_menu);

        mItemList = findViewById(R.id.itemList);
        mItemList.setHasFixedSize(true);
        mItemList.setLayoutManager(new LinearLayoutManager(this));

        mDB = FirebaseDatabase.getInstance().getReference().child("MenuItems");
        mAuth = FirebaseAuth.getInstance();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {
                    Intent loginIntent = new Intent(MenuActivity.this, LoginActivity.class);
                    loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginIntent);
                    finish();
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
        FirebaseRecyclerAdapter<MenuItem, MenuItemViewHolder> recyclerAdapter = new FirebaseRecyclerAdapter<MenuItem, MenuItemViewHolder>
                (
                        MenuItem.class,
                        R.layout.singlemenuitem,
                        MenuItemViewHolder.class,
                        mDB) {
            @Override
            protected void populateViewHolder(MenuItemViewHolder viewHolder, MenuItem model, int position) {
                viewHolder.setName(model.getName());
                viewHolder.setDesc(model.getDesc());
                viewHolder.setPrice(model.getPrice());
                viewHolder.setImage(getApplicationContext(), model.getImageURL());
            }
        };
        mItemList.setAdapter(recyclerAdapter);
    }

    public void btnSignOutClicked(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out?")
                .setMessage("Are you sure you want sign out?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        logoutUser();
                    }
                }).create().show();
    }

    public static class MenuItemViewHolder extends RecyclerView.ViewHolder{
        View mView;
        public MenuItemViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setName(String name) {
            TextView itemName = mView.findViewById(R.id.itemName);
            itemName.setText(name);
        }

        public void setDesc(String desc) {
            TextView itemDesc = mView.findViewById(R.id.itemDesc);
            itemDesc.setText(desc);
        }

        public void setImage(Context ctx, String image) {
            ImageView itemImage = (ImageView) mView.findViewById(R.id.itemImage);
            Picasso.with(ctx).load(image).into(itemImage);
        }

        public void setPrice(String price) {
            TextView itemPrice = mView.findViewById(R.id.itemPrice);
            itemPrice.setText(price);
        }
    }

    private void logoutUser()
    {
        mAuth.signOut();
        Intent loginIntent = new Intent(MenuActivity.this,LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("Are you sure you want to exit?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        MenuActivity.this.finish();
                        MenuActivity.super.onBackPressed();
                    }
                }).create().show();
    }
}
