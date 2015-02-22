package com.ay.filesharing.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.ay.filesharing.GetClientInfo;
import com.ay.filesharing.R;
import com.gc.materialdesign.views.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import file.ActiveLogin;
import file.ActiveUser;

public class SelectUserAdapter extends ArrayAdapter<file.ActiveUser> {

	ArrayList<ActiveLogin> listActiveLogin;
	ArrayList<ActiveUser> activeUsers;
	
	public ArrayList<ActiveUser> getActiveUsers(){
		return activeUsers;
	}
	
	public SelectUserAdapter(Context context, int resource,
			List<ActiveUser> objects, ArrayList<ActiveLogin> listActiveLogin) {
		super(context, resource, objects);

		this.listActiveLogin = listActiveLogin;
		activeUsers = new ArrayList<>();
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		ViewHolder holder;
		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.view_select_user, parent, false);
			holder = new ViewHolder();
			holder.user = (TextView) rowView.findViewById(R.id.textView1);
			holder.img = (ImageView) rowView.findViewById(R.id.imageView1);
			holder.chk = (CheckBox) rowView.findViewById(R.id.checkBox1);
			rowView.setTag(holder);
		} else {
			holder = (ViewHolder) rowView.getTag();
		}
		final ActiveUser user = getItem(position);
		holder.user.setText(user.username);
		final CheckBox chk = holder.chk;
        chk.setChecked(activeUsers.contains(user));
		holder.chk.setOncheckListener(new CheckBox.OnCheckListener() {
            @Override
            public void onCheck(boolean b) {
                if(b){
                    activeUsers.add(user);
                    Log.d("CheckBox.onClick", "Inserting "+user.username);
                }else{
                    if(activeUsers.contains(user)){
                        activeUsers.remove(user);
                        Log.d("CheckBox.onClick", "Removing "+user.username);
                    }
                }
            }
        });
		
		ActiveLogin activeLogin = null;
		
		for(ActiveLogin a: listActiveLogin)
			if(a.ActiveNetwork.equals(user.network)) activeLogin = a;
		
		if(activeLogin!=null){
			new GetClientInfo(holder.img, user, activeLogin).execute();
		}else{
			Toast.makeText(getContext(), "activeLogin is null", Toast.LENGTH_LONG).show();
		}
		
		return rowView;
	}
	
	class ViewHolder {
		TextView user;
		ImageView img;
		CheckBox chk;
	}

}
