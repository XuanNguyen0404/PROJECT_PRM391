package com.example.football_field_booking.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.football_field_booking.R;
import com.example.football_field_booking.dtos.UserDTO;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class UserAdapter extends BaseAdapter {

    private Context context;
    private List<UserDTO> listUser;
    private LayoutInflater layoutInflater;

    public UserAdapter(Context context, List<UserDTO> listUser) {
        this.context = context;
        this.listUser = listUser;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return listUser.size();
    }

    @Override
    public Object getItem(int i) {
        return listUser.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View rowView = view;
        if (rowView == null) {
            rowView = layoutInflater.inflate(R.layout.item_user, viewGroup, false);
        }
        ImageView imgUser = rowView.findViewById(R.id.imgUser);
        TextView txtFullName = rowView.findViewById(R.id.txtFullName);
        TextView txtEmail = rowView.findViewById(R.id.txtEmail);
        TextView txtStatus = rowView.findViewById(R.id.txtStatus);
        TextView txtPhone = rowView.findViewById(R.id.txtPhone);
        TextView txtRole = rowView.findViewById(R.id.txtRole);
        LinearLayout lnListUser = rowView.findViewById(R.id.lnListUser);


        UserDTO user = listUser.get(i);

        if (user.getStatus().equals("inactive")) {
            lnListUser.setAlpha(0.3F);
        }else{
            lnListUser.setAlpha(1);
        }
        if (user.getPhotoUri() != null) {
            Uri uri = Uri.parse(user.getPhotoUri());
            Glide.with(imgUser.getContext())
                    .load(uri)
                    .into(imgUser);
        } else {
            imgUser.setImageResource(R.drawable.outline_account_circle_24);
        }
        txtEmail.setText(user.getEmail());
        txtFullName.setText(user.getFullName());
        txtStatus.setText(user.getStatus());

        String phone = user.getPhone();
        if (phone != null) {
            txtPhone.setText(phone);
        } else {
            txtPhone.setText("empty");
        }
        txtRole.setText(user.getRole());
        return rowView;
    }
}
