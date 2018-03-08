package com.orderfood.tusharparmar.orderfoodclient;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.ViewGroup;
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
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    EditText email, password;
    FirebaseAuth mAuth;
    DatabaseReference mDB;
    ImageView imgGSignIn;
    LoginButton btnFBSignIn;
    ImageView imgFBSignIn;
    Button btnSignUp;
    private static final int RC_SIGN_IN_GOOGLE = 2;
    private static final int RC_SIGN_IN_FB = 3;
    CallbackManager mCallbackManager;
    private static final String EMAIL = "email";
    GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth.AuthStateListener mAuthStateListener;
    AuthCredential failedCredentials;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseDatabase.getInstance().getReference().child("users");
        mProgressDialog = new ProgressDialog(LoginActivity.this);

        email = findViewById(R.id.userEmail);
        password = findViewById(R.id.userPassword);
        btnSignUp = findViewById(R.id.btnSignUp);

        initGoogleSignIn();
        initFacebookSignIn();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(failedCredentials == null)
            updateUI(mAuth.getCurrentUser());
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

    public void btnSignInClicked(View view) {
        String strEmail = email.getText().toString().trim();
        String strPassword = password.getText().toString().trim();

        if(!TextUtils.isEmpty(strEmail) && !TextUtils.isEmpty(strPassword)) {
            AuthCredential credential = EmailAuthProvider.getCredential(strEmail, strPassword);
            if(failedCredentials == null)
            {
                isProviderRegistered(credential, strEmail, strPassword, EmailAuthProvider.PROVIDER_ID);
            }
            else {
                signInWithEmailPass(strEmail, strPassword);
            }
        }
    }

    private void signInWithEmailPass(String strEmail, String strPassword)
    {
        mProgressDialog.setMessage("Signing In...");
        mProgressDialog.show();
        mAuth.signInWithEmailAndPassword(strEmail, strPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    if (failedCredentials != null)
                        linkProvidersAndSignIn();
                    else
                        updateUI(mAuth.getCurrentUser());
                }
                else
                {
                    mProgressDialog.hide();
                    Toast.makeText(LoginActivity.this, "Authentication failed. Incorrect Email or Password!!!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initGoogleSignIn()
    {
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
    }

    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        if(GoogleSignIn.getLastSignedInAccount(LoginActivity.this) != null)
        {
            mGoogleSignInClient.signOut();
        }
        startActivityForResult(signInIntent, RC_SIGN_IN_GOOGLE);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        mProgressDialog.setMessage("Signing In...");
        mProgressDialog.show();
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        if(failedCredentials == null)
            isProviderRegistered(credential, acct.getEmail(), null, GoogleAuthProvider.PROVIDER_ID);
        else
            signInWithGoogle(credential);
    }

    private void signInWithGoogle(AuthCredential credential)
    {
        mProgressDialog.setMessage("Signing In...");
        mProgressDialog.show();
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            if(failedCredentials != null)
                                linkProvidersAndSignIn();
                            else
                                updateUI(mAuth.getCurrentUser());
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void initFacebookSignIn()
    {
        mCallbackManager = CallbackManager.Factory.create();
        imgFBSignIn = findViewById(R.id.login_button_img);
        btnFBSignIn = (LoginButton) findViewById(R.id.login_button);
        btnFBSignIn.setReadPermissions(Arrays.asList(EMAIL));
        btnFBSignIn.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                final String[] fbEmail = new String[1];
                final String[] fbUserId = new String[1];
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject me, GraphResponse response) {
                                fbEmail[0] = me.optString("email");
                                fbUserId[0] = me.optString("id");
                                handleFacebookAccessToken(loginResult.getAccessToken(),fbEmail,fbUserId);
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email");
                request.setParameters(parameters);
                request.executeAsync();
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

    public void imgFBSignInClicked(View view) {
        LoginManager.getInstance().logOut();
        btnFBSignIn.performClick();
    }

    private void signInWithFB(AuthCredential credential)
    {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            if(failedCredentials != null)
                                linkProvidersAndSignIn();
                            else
                                updateUI(mAuth.getCurrentUser());
                        }
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token,final String[] fbEmail,final String[] fbUserId) {
        mProgressDialog.setMessage("Signing In...");
        mProgressDialog.show();
        final AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        if(failedCredentials == null)
            isProviderRegistered(credential, fbEmail[0], null, FacebookAuthProvider.PROVIDER_ID);
        else
            signInWithFB(credential);
    }

    private void initAuthStateListener()
    {
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

    private void updateUI(FirebaseUser user) {
        mProgressDialog.hide();
        if (user != null) {
            Intent menuIntent = new Intent(LoginActivity.this,MenuActivity.class);
            menuIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(menuIntent);
            failedCredentials = null;
            LoginActivity.this.finish();
        }
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

    private void linkProvidersAndSignIn()
    {
        mAuth.getCurrentUser().linkWithCredential(failedCredentials).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                Toast.makeText(LoginActivity.this, "Accounts linked.",
                        Toast.LENGTH_SHORT).show();
                updateUI(mAuth.getCurrentUser());
            }
        });
    }

    private void isProviderRegistered(final AuthCredential credential, final String email, final String password, final String providerId)
    {
        mAuth.fetchProvidersForEmail(email).addOnCompleteListener(new OnCompleteListener<ProviderQueryResult>() {
            @Override
            public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                final List<String> providers = task.getResult().getProviders();
                if(providers.size() > 0 && !providers.contains(providerId)) {
                    new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("Email already registered.")
                            .setMessage("Please login using original method to link accounts.")
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    failedCredentials = credential;
                                    if(providers.contains(GoogleAuthProvider.PROVIDER_ID)) {
                                        googleSignIn();
                                    }
                                    else if(providers.contains(EmailAuthProvider.PROVIDER_ID))
                                    {
                                        final AlertDialog.Builder alert = new AlertDialog.Builder(LoginActivity.this);
                                        alert.setTitle("Email login");
                                        alert.setMessage("Please enter password for \nEmail: "+ email);
                                        final EditText input = new EditText(LoginActivity.this);
                                        input.setHint("Password");
                                        input.setWidth(100);
                                        input.setTransformationMethod(new PasswordTransformationMethod());
                                        alert.setView(input);
                                        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                failedCredentials = null;
                                                mProgressDialog.hide();
                                            }
                                        });
                                        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                String value = input.getText().toString();
                                                if (value != null || value != "") {
                                                    signInWithEmailPass(email, value);
                                                }
                                            }
                                        });
                                        alert.show();
                                    }
                                    else if(providers.contains(FacebookAuthProvider.PROVIDER_ID))
                                    {
                                        LoginManager.getInstance().logOut();
                                        btnFBSignIn.performClick();
                                    }
                                }
                            }).create().show();
                }
                else
                {
                    if(FacebookAuthProvider.PROVIDER_ID.equals(providerId))
                    {
                        signInWithFB(credential);
                    }
                    else if(GoogleAuthProvider.PROVIDER_ID.equals(providerId))
                    {
                        signInWithGoogle(credential);
                    }
                    else if(EmailAuthProvider.PROVIDER_ID.equals(providerId) && providers.isEmpty())
                    {
                        new AlertDialog.Builder(LoginActivity.this)
                                .setTitle("Email not registered.")
                                .setMessage("Please SignUp first to sign in.")
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        txtSignUpClicked(btnSignUp);
                                    }
                                }).create().show();
                    }
                    else if(EmailAuthProvider.PROVIDER_ID.equals(providerId))
                    {
                        signInWithEmailPass(email, password);
                    }
                }
            }
        });
    }
}
