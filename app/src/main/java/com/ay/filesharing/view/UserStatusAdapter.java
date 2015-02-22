package com.ay.filesharing.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ay.filesharing.R;

import java.util.List;

import file.UserStatus;

/**
 * Created by Ardiya on 1/18/2015.
 */
public class UserStatusAdapter extends ArrayAdapter<UserStatus> {
    public UserStatusAdapter(Context context, int resource, List<UserStatus> objects){
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if(view == null){
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view  = inflater.inflate(R.layout.view_user_status, parent, false);
            holder = new ViewHolder();
            holder.txtUsername = (TextView) view.findViewById(R.id.textView1);
            holder.txtRole = (TextView) view.findViewById(R.id.textView2);
            holder.txtStatus = (TextView) view.findViewById(R.id.textView3);
            holder.txtHandler = (TextView) view.findViewById(R.id.textView4);

            view.setTag(holder);
        }else{
            holder = (ViewHolder) view.getTag();
        }
        final UserStatus userStatus = getItem(position);
        holder.txtUsername.setText(userStatus.getUsername());
        holder.txtRole.setText(userStatus.getRole());
        holder.txtStatus.setText(userStatus.getStatus());
        holder.txtHandler.setText(userStatus.getHandler());

        return view;
    }
    class ViewHolder {
        TextView txtUsername, txtRole, txtStatus, txtHandler;
    }
}
