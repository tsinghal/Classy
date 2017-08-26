package csci201.classy.LoginActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import csci201.classy.MainActivity.MainActivity;
import csci201.classy.R;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener{
    //debugging
    private static final String TAG = "LoginActivity";
    final Context context = this;
    // Views
    private EditText mEmailField;
    private EditText mPasswordField;
    private Button loginButton;
    private Button createAccountButton;
    private Button resetButton;
    private Button continueButton;
    // Authentication
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT > 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_login);

        mEmailField = (EditText) findViewById(R.id.emailText);
        mPasswordField = (EditText) findViewById(R.id.passwordText);
        loginButton = (Button) findViewById(R.id.loginButton);
        createAccountButton = (Button) findViewById(R.id.createAccountButton);
        continueButton = (Button) findViewById(R.id.guestLoginButton);
        resetButton = (Button) findViewById(R.id.resetButton);

        loginButton.setOnClickListener(this);
        createAccountButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);
        continueButton.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        /**
         * NOTE:
         * As of now, if the user is already signed in, then we will sign them out.
         * This is because we assume that if the login screen is active, then there is no user signed in.
         * If a user is already signed in, this messes up the create account logic which assumes
         * there is no user signed in.
         */
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in, sign them out.
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
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
        if (i == R.id.createAccountButton) {
            {
                Intent intent = new Intent(this, CreateAccountActivity.class);
                startActivity(intent);
            }
        } else if (i == R.id.loginButton) {
            signIn(mEmailField.getText().toString(), mPasswordField.getText().toString());
        } else if (i == R.id.resetButton){
            reset(mEmailField.getText().toString());
        } else if (i == R.id.guestLoginButton){

            String email = mEmailField.getText().toString();
            if (TextUtils.isEmpty(email) || !(email.split("@").length == 2 && email.split("@")[1].equals("usc.edu"))) {
                mEmailField.setError("USC email required.");
                Toast.makeText(LoginActivity.this, "Valid email required for Guest Login",
                        Toast.LENGTH_SHORT).show();
            } else {
                mEmailField.setError(null);
                createDialog();
            }
        }
    }


    private  void reset(String mail) {
        String email = mail;
        if (TextUtils.isEmpty(email) || !(email.split("@").length == 2 && email.split("@")[1].equals("usc.edu"))) {
            mEmailField.setError("USC email required.");
            return;
        } else {
            mEmailField.setError(null);
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email sent.");
                            Toast.makeText(LoginActivity.this, "Reset password email sent.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(LoginActivity.this, "Reset password failed. Check email address",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    /**
     * Signs in the user. Checks if credentials are invalid and if email is not verified.
     * @param email
     * @param password
     */
    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);
        if (!validateForm()) {
            return;
        }
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Login Failed",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if(user.isEmailVerified()) {
                                Toast.makeText(LoginActivity.this, "Login Successful",
                                        Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra("GUEST", false);
                                startActivity(intent);
                            } else {
                                Toast.makeText(LoginActivity.this, "Email not verified",
                                        Toast.LENGTH_SHORT).show();
                            }

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
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("6 characters or more required.");
            valid = false;
        } else {
            mPasswordField.setError(null);
        }

        return valid;
    }

    public void createDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();

        alertDialogBuilder.setView(inflater.inflate(R.layout.guestdetails, null))
                // Add action buttons
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("GUEST", true);
                        intent.putExtra("GUEST_EMAIL", mEmailField.getText().toString());
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                })
        ;


        TextView myMsg = new TextView(this);
        myMsg.setText("Guest Login");
        myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
        myMsg.setTextSize(25);
        myMsg.setTextColor(Color.WHITE);

        // set title
        alertDialogBuilder.setCustomTitle(myMsg);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }
}