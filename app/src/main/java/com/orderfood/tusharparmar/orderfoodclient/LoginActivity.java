package com.orderfood.tusharparmar.orderfoodclient;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
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
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {
    EditText email, password;
    FirebaseAuth mAuth;
    DatabaseReference mDB;
    ImageView imgGSignIn;
    LoginButton btnFBSignIn;
    ImageView imgFBSignIn;
    private static final int RC_SIGN_IN_GOOGLE = 2;
    private static final int RC_SIGN_IN_FB = 3;
    CallbackManager mCallbackManager;
    private static final String EMAIL = "email";
    GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth.AuthStateListener mAuthStateListener;
    AuthCredential failedCredentials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseDatabase.getInstance().getReference().child("users");

        email = findViewById(R.id.userEmail);
        password = findViewById(R.id.userPassword);

        imgGSignIn = findViewById(R.id.sign_in_button_img);
        imgGSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn();
            }
        });
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        mCallbackManager = CallbackManager.Factory.create();
        imgFBSignIn = findViewById(R.id.login_button_img);
        btnFBSignIn = (LoginButton) findViewById(R.id.login_button);
        btnFBSignIn.setReadPermissions(Arrays.asList(EMAIL));
        btnFBSignIn.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                final String[] fbEmail = new String[1];
                final String[] fbUserId = new String[1];
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject me, GraphResponse response) {
                                /*if (response.getError() != null) {
                                } else {*/
                                    fbEmail[0] = me.optString("email");
                                    fbUserId[0] = me.optString("id");
                                //}
                                System.out.println("######## " + me.toString());
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email");
                request.setParameters(parameters);
                request.executeAsync();
                handleFacebookAccessToken(loginResult.getAccessToken(),fbEmail,fbUserId);
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

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser() != null) {
                    Intent menuIntent = new Intent(LoginActivity.this, MenuActivity.class);
                    menuIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(menuIntent);
                    LoginActivity.this.finish();
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN_GOOGLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
            }
        }
        else
        {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            if(failedCredentials != null)
                            {
                                mAuth.getCurrentUser().linkWithCredential(failedCredentials).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        Toast.makeText(LoginActivity.this, "Accounts linked.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            checkUserExists();
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
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

    private void handleFacebookAccessToken(AccessToken token,final String[] fbEmail,final String[] fbUserId) {
        final AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            checkUserExists();
                        } else {
                            if(task.getException() instanceof FirebaseAuthUserCollisionException)
                            {
                                mAuth.fetchProvidersForEmail(fbEmail[0])
                                        .addOnCompleteListener(new OnCompleteListener<ProviderQueryResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                                                if (task.isSuccessful()) {
                                                    if (task.getResult().getProviders().contains(
                                                            EmailAuthProvider.PROVIDER_ID)) {
                                                        // Password account already exists with the same email.
                                                        // Ask user to provide password associated with that account.
                                                        // Sign in with email and the provided password.
                                                        // If this was a Google account, call signInWithCredential instead.
                                                        mAuth.signInWithEmailAndPassword(fbEmail[0], fbEmail[0]).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                                if (task.isSuccessful()) {
                                                                    // Link initial credential to existing account.
                                                                    mAuth.getCurrentUser().linkWithCredential(credential);
                                                                    checkUserExists();
                                                                }
                                                            }
                                                        });
                                                    }
                                                    else if(task.getResult().getProviders().contains(
                                                            GoogleAuthProvider.PROVIDER_ID))
                                                    {
                                                        failedCredentials = credential;
                                                        googleSignIn();
                                                    }
                                                }
                                            }
                                        });
                            }
                            else {
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
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
                    menuIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(menuIntent);
                    LoginActivity.this.finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void txtSignUpClicked(View view) {
        Intent mainIntent = new Intent(LoginActivity.this,MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    public void imgFBSignInClicked(View view) {
        btnFBSignIn.performClick();
    }
}
