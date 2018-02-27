package com.orderfood.tusharparmar.orderfoodclient;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    EditText email, password, passwordConf;
    DatabaseReference mDB;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        email = findViewById(R.id.txtEmail);
        password = findViewById(R.id.txtPassword);
        passwordConf = findViewById(R.id.txtPasswordConf);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseDatabase.getInstance().getReference().child("users");
    }


    public void btnSignUpClicked(View view) {
        final String strEmail = email.getText().toString().trim();
        String strPassword = password.getText().toString().trim();
        String strPasswordConf = passwordConf.getText().toString().trim();

        if(isValidEmailPassword(strEmail,strPassword,strPasswordConf))
        {
            mAuth.createUserWithEmailAndPassword(strEmail,strPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        DatabaseReference currentUser = mDB.child(userId);
                        currentUser.child("Email").setValue(strEmail);

                        Intent intent = new Intent(MainActivity.this, MenuActivity.class);
                        startActivity(intent);
                    }
                }
            });
        }
        else
        {
            Toast.makeText(MainActivity.this,
                    "Email & Password can't be empty or Password should match confirm Password.",Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidEmailPassword(String strEmail, String strPassword, String strPasswordConf)
    {
        if(!TextUtils.isEmpty(strEmail) && !TextUtils.isEmpty(strPassword) && !TextUtils.isEmpty(strPasswordConf))
        {
            if(strPassword.equals(strPasswordConf))
            {
                return true;
            }
        }
        return false;
    }

    public void btnSignInClicked(View view) {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}
