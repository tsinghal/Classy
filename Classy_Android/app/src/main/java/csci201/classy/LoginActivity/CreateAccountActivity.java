package csci201.classy.LoginActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import csci201.classy.R;

public class CreateAccountActivity extends AppCompatActivity implements View.OnClickListener{
    //debugging
    private static final String TAG = "CreateAccountActivity";
    private EditText mEmailField;
    private EditText mPasswordField;
    private EditText mConfirmPasswordField;
    private Button mRegisterButton;
    // Authentication
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        if (Build.VERSION.SDK_INT > 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(Color.parseColor("#1a75ff"));

        mEmailField = (EditText)findViewById(R.id.emailText);
        mPasswordField = (EditText)findViewById(R.id.passwordText);
        mConfirmPasswordField = (EditText) findViewById(R.id.confirmPasswordEditText);
        mRegisterButton = (Button)findViewById(R.id.registerButton);



        mRegisterButton.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

      /* FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
       fab.setOnClickListener(new View.OnClickListener() {@Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mAuthListener != null)
            mAuth.removeAuthStateListener(mAuthListener);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.registerButton) {

                createAccount(mEmailField.getText().toString(), mPasswordField.getText().toString());
        }
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, "createAccount:" + email);
        if (!validateForm()) {
            return;
        }
        mAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(CreateAccountActivity.this, "create account Failed",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(CreateAccountActivity.this, "create account Successful",
                                    Toast.LENGTH_SHORT).show();

                            // Send email verification
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                            user.sendEmailVerification()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(CreateAccountActivity.this, "email sent",
                                                        Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(CreateAccountActivity.this, "email not sent",
                                                        Toast.LENGTH_SHORT).show();
                                                Log.e("email verification", task.getException().getMessage());
                                            }
                                        }
                                    });
                            Intent intent =  new Intent(CreateAccountActivity.this, LoginActivity.class);
                            startActivity(intent);
                            mAuth.signOut();
                        }
                    }
                });
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = mEmailField.getText().toString();
        if (TextUtils.isEmpty(email) || !(email.split("@").length == 2 && email.split("@")[1].equals("usc.edu"))) {
            mEmailField.setError("USC email required.");
            valid = false;
        } else {
            mEmailField.setError(null);
        }

        String password = mPasswordField.getText().toString();
        String password2 = mConfirmPasswordField.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("6 characters or more required.");
            valid = false;
        } else if(TextUtils.isEmpty(password2)){
            mConfirmPasswordField.setError("Please confirm password.");
            valid = false;
        }
        else if(!password.equals(password2)){
            mConfirmPasswordField.setError("Passwords do not match.");
            valid = false;
        }
        else {
            mPasswordField.setError(null);
            mConfirmPasswordField.setError(null);
        }

        return valid;
    }


}
