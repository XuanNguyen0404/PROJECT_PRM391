package com.example.football_field_booking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.football_field_booking.daos.UserDAO;
import com.example.football_field_booking.dtos.UserDTO;
import com.example.football_field_booking.utils.Utils;
import com.example.football_field_booking.validations.Validation;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    public static final String GOOGLE_LOG = "Google Log:";
    private static final int RC_SIGN_IN = 9001;
    public static final String EMAIL_LOG = "Email";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private Utils util;
    private Validation val;

    private Button btnSignInWithGoogle, btnLogin, btnSignUp;
    private TextInputLayout txtEmail, txtPassword;
    private ProgressDialog prdLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id_bookingFF))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();
        util = new Utils();
        val = new Validation();

        btnSignInWithGoogle = findViewById(R.id.btnSignInWithGoogle);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        txtEmail = findViewById(R.id.txtEmail);
        txtPassword = findViewById(R.id.txtPassword);
        prdLogin = new ProgressDialog(LoginActivity.this);

        btnSignInWithGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInWithGoogle();
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = txtEmail.getEditText().getText().toString();
                String password = txtPassword.getEditText().getText().toString();
                if (isValidLogin(email, password)) {
                    util.showProgressDialog(prdLogin, "Login", "Please wait for login");
                    signInWithEmail(email, password);
                } else {
                    updateUI(null);
                }
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Validation validation = new Validation();
        if (validation.isUser()) {
            updateUI(currentUser);
        } else {
            updateUI(null);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(GOOGLE_LOG, "firebaseAuthWithGoogle:" + account.getId());
                util.showProgressDialog(prdLogin, "Login", "Please wait for login");
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(GOOGLE_LOG, "Google sign in failed", e);
            }
        }

    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(GOOGLE_LOG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (task.getResult().getAdditionalUserInfo().isNewUser()) {
                                try {
                                    UserDAO dao = new UserDAO();
                                    UserDTO userDTO = new UserDTO(user.getUid(), user.getEmail(),
                                            user.getDisplayName(), user.getPhoneNumber(), "user",
                                            "active", null);
                                    if (user.getPhotoUrl() != null) {
                                        userDTO.setPhotoUri(user.getPhotoUrl().toString());
                                    }
                                    dao.createUser(userDTO);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            checkActive(user);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(GOOGLE_LOG, "signInWithCredential:failure", task.getException());
                            updateUI(null);
                        }
                    }
                });
    }

    private void signInWithGoogle() {
        mGoogleSignInClient.signOut();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(EMAIL_LOG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user.isEmailVerified()) {
                                checkActive(user);
                            } else {
                                Toast.makeText(LoginActivity.this, "Please verify your email address",
                                        Toast.LENGTH_SHORT).show();
                                updateUI(null);
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void checkActive (FirebaseUser user) {
        UserDAO userDAO = new UserDAO();
        userDAO.getUserById(user.getUid()).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    String status = task.getResult().getString("userInfo.status");
                    if(status.equals("active")){
                        updateUI(user);
                    } else if (status.equals("inactive")) {
                        Toast.makeText(LoginActivity.this, "Your account has been deleted", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        updateUI(null);
                    } else {
                        Toast.makeText(LoginActivity.this, getResources().getString(R.string.something_went_wrong)
                                , Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        updateUI(null);
                    }
                }else {
                    Toast.makeText(LoginActivity.this, "Load data fail", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    updateUI(null);
                }
            }
        });
    }

    private void updateUI(FirebaseUser user) {
        prdLogin.cancel();
        if (user != null) {
            UserDAO userDAO = new UserDAO();
            userDAO.getUserById(user.getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    String role = documentSnapshot.getString("userInfo.role");
                    if (role != null) {
                        switch (role) {
                            case "user": {
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                LoginActivity.this.finish();
                                break;
                            }
                            case "owner": {
                                FirebaseMessaging.getInstance().getToken()
                                        .addOnCompleteListener(new OnCompleteListener<String>() {
                                            @Override
                                            public void onComplete(@NonNull Task<String> task) {
                                                if (!task.isSuccessful()) {
                                                    Log.w("Fetch Token Error", "Fetching FCM registration token failed", task.getException());
                                                    return;
                                                }
                                                // Get new FCM registration token
                                                String token = task.getResult();
                                                UserDAO userDAO = new UserDAO();
                                                try {
                                                    userDAO.saveToken(token, user.getUid());
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                Intent intent = new Intent(LoginActivity.this, OwnerMainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                LoginActivity.this.finish();
                                break;
                            }
                            case "admin":
                                Intent intent = new Intent(LoginActivity.this, AdminMainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                LoginActivity.this.finish();
                                break;
                            default:
                                Toast.makeText(LoginActivity.this, "Your role is invalid",
                                        Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                }
            });
        }
    }

    private boolean isValidLogin(String email, String password) {
        util.clearError(txtEmail);
        util.clearError(txtPassword);
        boolean result = true;
        if (!val.isValidPassword(password)) {
            util.showError(txtPassword, "Password must be more than 8 character");
            result = false;
        }
        if (!val.isValidEmail(email)) {
            util.showError(txtEmail, "Email is invalid");
            result = false;
        }
        return result;
    }

}