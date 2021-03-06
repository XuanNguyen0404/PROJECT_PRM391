package com.example.football_field_booking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.google.firebase.firestore.GeoPoint;

public class MainActivity extends AppCompatActivity {

    public static final int RC_LOCATION = 1002;
    private MaterialToolbar topAppBar;
    private BottomNavigationView bottomNavigationView;
    private GeoPoint geoMe;
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
                            showAlert();
                            return true;
                        }
                        break;
                    case R.id.pageCart:
                        if (validation.isUser()) {
                            selectedFragment = new CartFragment();
                        } else {
                            showAlert();
                            return true;
                        }
                        break;
                    case R.id.pageHistory:
                        if (validation.isUser()) {
                            selectedFragment = new HistoryFragment();
                        } else {
                            showAlert();
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

    private void showAlert(){
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle("Function need to log in");
        alert.setMessage("Please login with your account to experience this function");
        alert.setPositiveButton("Log in", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
        alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // close dialog
                dialog.cancel();
            }
        });
        alert.show();
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
        Intent intent=new Intent(this, SearchFieldActivity.class);
        startActivity(intent);
    }

    public void clickToSearchByLocation (MenuItem item){
        Intent intent = new Intent(MainActivity.this, GoogleMapActivity.class);
        intent.putExtra("action", "chooseLocation");
        startActivityForResult(intent, RC_LOCATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_LOCATION && resultCode == RESULT_OK) {
            try {
                double lat = data.getDoubleExtra("lat", 0);
                double lng = data.getDoubleExtra("lng", 0);
                if(lat != 0 && lng != 0) {
                    geoMe = new GeoPoint(lat, lng);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new UserHomeFragment()).commit();
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public GeoPoint getGeoMe() {
        return geoMe;
    }
}