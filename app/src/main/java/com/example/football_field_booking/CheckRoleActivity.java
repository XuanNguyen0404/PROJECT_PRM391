package com.example.football_field_booking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.football_field_booking.daos.UserDAO;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

public class CheckRoleActivity extends AppCompatActivity {

    private MaterialButton btnDiscover,btnLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_role);
        btnDiscover=findViewById(R.id.btnDiscover);
        btnLogin=findViewById(R.id.btnLogin);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.e("MY_USER_ID",user.getDisplayName());
            btnLogin.setVisibility(View.GONE);
            btnDiscover.setVisibility(View.GONE);
            UserDAO userDAO = new UserDAO();
            userDAO.getUserById(user.getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    String role = documentSnapshot.getString("userInfo.role");
                    if (role != null) {
                        switch (role) {
                            case "user": {
                                Intent intent = new Intent(CheckRoleActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                CheckRoleActivity.this.finish();
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
                                Intent intent = new Intent(CheckRoleActivity.this, OwnerMainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                CheckRoleActivity.this.finish();
                                break;
                            }
                            case "admin":
                                Intent intent = new Intent(CheckRoleActivity.this, AdminMainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                CheckRoleActivity.this.finish();
                                break;
                            default:
                                Toast.makeText(CheckRoleActivity.this, "Your role is invalid",
                                        Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                }
            });
        }else{
            btnLogin.setVisibility(View.VISIBLE);
            btnDiscover.setVisibility(View.VISIBLE);
        }
    }

    public void clickToDiscover(View view) {
        Intent intent=new Intent(this,MainActivity.class);
        startActivity(intent);
    }

    public void clickToLogin(View view) {
        Intent intent=new Intent(this,LoginActivity.class);
        startActivity(intent);
    }
}