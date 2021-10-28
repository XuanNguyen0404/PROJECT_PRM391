package com.example.football_field_booking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.example.football_field_booking.daos.UserDAO;
import com.example.football_field_booking.fragments.CartFragment;
import com.example.football_field_booking.fragments.HistoryFragment;
import com.example.football_field_booking.fragments.ProfileFragment;
import com.example.football_field_booking.fragments.UserHomeFragment;
import com.example.football_field_booking.validations.Validation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;
    private BottomNavigationView bottomNavigationView;

    private Validation validation = new Validation();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        updateUI(user);

        topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setTitleTextAppearance(this, R.style.FontLogo);
        View logo = topAppBar.getChildAt(0);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                switch (item.getItemId()){
                    case R.id.pageHome:
                        selectedFragment = new UserHomeFragment();
                        break;
                    case R.id.pageAccount:
                        if (validation.isUser()) {
                            selectedFragment = new ProfileFragment();
                        } else {
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                            return true;
                        }
                        break;
                    case R.id.pageCart:
                        if (validation.isUser()) {
                            selectedFragment = new CartFragment();
                        } else {
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                            return true;
                        }
                        break;
                    case R.id.pageHistory:
                        if (validation.isUser()) {
                            selectedFragment = new HistoryFragment();
                        } else {
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                            return true;
                        }
                        break;

                    default:
                        return false;
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, selectedFragment).commit();
                return true;
            }
        });

        Intent intent = this.getIntent();
        String action = intent.getStringExtra("action");
        if (action != null) {
            Log.d("action", action);
            if (action.equals("add to cart")) {
                Fragment cartFragment = new CartFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, cartFragment).commit();
                bottomNavigationView.setSelectedItemId(R.id.pageCart);
            } else if (action.equals("view history")) {
               Fragment historyFragment = new HistoryFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, historyFragment).commit();
                bottomNavigationView.setSelectedItemId(R.id.pageHistory);
            } else if(action.equals("check_out_success")){
                Fragment cartFragment = new CartFragment();
                Bundle bundle=new Bundle();
                bundle.putString("check_out_success","check_out_success");
                cartFragment.setArguments(bundle);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, cartFragment).commit();
                bottomNavigationView.setSelectedItemId(R.id.pageCart);
            }
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new UserHomeFragment()).commit();
        }

        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            UserDAO userDAO = new UserDAO();
            userDAO.getUserById(user.getUid()).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    String role = documentSnapshot.getString("userInfo.role");
                    Log.d("USER", "role: " + role);
                    if (role != null) {
                        switch (role) {
                            case "owner": {
                                Intent intent = new Intent(MainActivity.this, OwnerMainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                MainActivity.this.finish();
                                break;
                            }
                            case "admin":
                                Intent intent = new Intent(MainActivity.this, AdminMainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                MainActivity.this.finish();
                                break;
                        }
                    }
                }
            });
        }
    }

    public void clickToGoToSearchActivity(MenuItem item) {
        Intent intent=new Intent(this, SearchActivity.class);
        startActivity(intent);
    }
}