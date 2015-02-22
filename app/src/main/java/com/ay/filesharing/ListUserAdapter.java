package com.ay.filesharing;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fortysevendeg.swipelistview.SwipeListView;

import java.util.ArrayList;

import file.ActiveLogin;
import file.ActiveUser;
import file.FTPUser;
import protocol.DistributedFileSharingProtocol;
import util.StateManager;

class ListUserAdapter extends ArrayAdapter<ActiveUser> {

    private Activity activity;
	ListUserAdapter(Activity activity, ArrayList<ActiveUser> activeUsers) {
        super(activity,R.layout.view_activeuser, activeUsers);
        this.activity = activity;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		View rowView = view;
		ViewHolder holder;
		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.view_activeuser, parent, false);
			holder = new ViewHolder();
			holder.btFTP = (ImageButton) rowView.findViewById(R.id.btA);
			holder.btChat = (ImageButton) rowView.findViewById(R.id.btB);
			holder.img = (ImageView) rowView.findViewById(R.id.img);
			holder.txt = (TextView) rowView.findViewById(R.id.txt);
			rowView.setTag(holder);
		} else {
			holder = (ViewHolder) rowView.getTag();
		}

		((SwipeListView) parent).recycle(rowView, position);

		//get target active user
		final ActiveUser user = getItem(position);
		
		holder.txt.setText(user.username);
		ActiveLogin currentActiveLogin = (ActiveLogin) StateManager.getItem("currentActiveLogin");
        holder.img.setImageResource(R.drawable.ic_launcher);
		if(currentActiveLogin!=null){
			new GetClientInfo(holder.img, user, currentActiveLogin).execute();
		}
		final ActiveLogin finalActiveLogin = currentActiveLogin;
        final Intent i = new Intent(getContext(), ClientActivity.class);
        i.putExtra(ClientActivity.TAG_ACTIVE_LOGIN, finalActiveLogin);
        i.putExtra(ClientActivity.TAG_CLIENT, user);
		holder.btFTP.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                i.putExtra(ClientActivity.TAG_START, 0);

                getContext().startActivity(i);
                activity.overridePendingTransition(R.anim.push_up_in, R.anim.push_up_out);
			}
		});
		holder.btChat.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
                i.putExtra(ClientActivity.TAG_START, 1);
                getContext().startActivity(i);
                activity.overridePendingTransition(R.anim.push_up_in, R.anim.push_up_out);
			}
		});

		return rowView;
	}

	class ViewHolder {
		ImageView img;
		TextView txt;
        ImageButton btFTP;
        ImageButton btChat;
	}

}