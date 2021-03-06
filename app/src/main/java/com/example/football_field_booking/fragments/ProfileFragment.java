package com.example.football_field_booking.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.football_field_booking.ChangePasswordActivity;
import com.example.football_field_booking.CheckRoleActivity;
import com.example.football_field_booking.MainActivity;
import com.example.football_field_booking.EditProfileActivity;
import com.example.football_field_booking.R;
import com.example.football_field_booking.daos.UserDAO;
import com.example.football_field_booking.dtos.UserDTO;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

public class ProfileFragment extends Fragment {

    private Button btnLogout, btnChangePassword;
    private Button btnUpdateUser;
    private TextView txtFullName;
    private ImageView imgUser;

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        txtFullName = view.findViewById(R.id.txtFullName);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnUpdateUser = view.findViewById(R.id.btnUpdateUser);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        imgUser = view.findViewById(R.id.imgUser);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        UserDAO userDAO = new UserDAO();
        if(user!=null) {
            for (UserInfo userInfo: user.getProviderData()){
                Log.d("USER", "provider: " + userInfo.getProviderId());
                if(userInfo.getProviderId().equals("password")){
                    btnChangePassword.setVisibility(View.VISIBLE);
                    break;
                }
            }

            userDAO.getUserById(user.getUid()).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        try {
                            UserDTO userDTO = task.getResult().get("userInfo", UserDTO.class);
                            txtFullName.setText(userDTO.getFullName());
                            if (userDTO.getPhotoUri() != null) {
                                Uri uri = Uri.parse(userDTO.getPhotoUri());
                                Glide.with(imgUser.getContext())
                                        .load(uri)
                                        .into(imgUser);
                            } else {
                                imgUser.setImageResource(R.drawable.outline_account_circle_24);
                            }
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        userDAO.deleteToken(s, user.getUid()).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                });
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), CheckRoleActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });

        btnUpdateUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                startActivity(intent);
            }
        });
        // Inflate the layout for this fragment
        return view;
    }
}