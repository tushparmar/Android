package com.orderfood.tusharparmar.orderfoodclient;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {
    EditText email, password;
    LoginButton loginButtonFB;
    ImageView loginButtonFBImg;
    FirebaseAuth mAuth;
    DatabaseReference mDB;
    private static final int RC_SIGN_IN_GOOGLE = 123;
    private static final int RC_SIGN_IN_FB = 2;
    CallbackManager mCallbackManager;
    private static final String EMAIL = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_login);

        mCallbackManager = CallbackManager.Factory.create();
        email = findViewById(R.id.userEmail);
        password = findViewById(R.id.userPassword);


        mAuth = FirebaseAuth.getInstance();

        mDB = FirebaseDatabase.getInstance().getReference().child("users");

        loginButtonFBImg = findViewById(R.id.login_button_img);
        loginButtonFB = (LoginButton) findViewById(R.id.login_button);
        loginButtonFB.setReadPermissions(Arrays.asList(EMAIL));
        loginButtonFB.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Intent menuIntent = new Intent(LoginActivity.this,MenuActivity.class);
                startActivity(menuIntent);
                /*handleFacebookAccessToken(loginResult.getAccessToken());*/
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this,"Facebook login cancelled.", Toast.LENGTH_LONG);
            }

            @Override
            public void onError(FacebookException exception) {
                Toast.makeText(LoginActivity.this,"Facebook login failed!!!", Toast.LENGTH_LONG);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void btnSignInClicked(View view) {
        String strEmail = email.getText().toString().trim();
        String strPassword = password.getText().toString().trim();

        if(!TextUtils.isEmpty(strEmail) && !TextUtils.isEmpty(strPassword)) {
            mAuth.signInWithEmailAndPassword(strEmail, strPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        checkUserExists();
                    }
                }
            });
        }
    }

    private void checkUserExists()
    {
        final String userId = mAuth.getCurrentUser().getUid();
        mDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(userId))
                {
                    Intent menuIntent = new Intent(LoginActivity.this,MenuActivity.class);
                    startActivity(menuIntent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Intent menuIntent = new Intent(LoginActivity.this,MenuActivity.class);
                            startActivity(menuIntent);
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void btnFBSignInClicked(View view) {
        loginButtonFB.performClick();
    }
}
