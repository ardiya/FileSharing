package com.ay.filesharing.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ay.filesharing.R;

import java.util.ArrayList;

import file.FileLog;

public class FileLogAdapter extends ArrayAdapter<FileLog> {
	public FileLogAdapter(Context context, ArrayList logs) {
		super(context, R.layout.view_log, logs);
	}
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		ViewHolder holder;
		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.view_file_log, parent, false);
			holder = new ViewHolder();
			holder.filename = (TextView) rowView.findViewById(R.id.textView1);
			holder.hash = (TextView) rowView.findViewById(R.id.textView2);
			holder.from = (TextView) rowView.findViewById(R.id.textView3);
			holder.to = (TextView) rowView.findViewById(R.id.textView4);
			holder.date = (TextView) rowView.findViewById(R.id.textView5);
			rowView.setTag(holder);
		} else {
			holder = (ViewHolder) rowView.getTag();
		}
		FileLog filelog = getItem(position);
        try {
            holder.filename.setText(filelog.getFilename());
            holder.hash.setText(filelog.getHash());
            holder.from.setText(filelog.getUsernameFrom());
            holder.to.setText(filelog.getUsernameTo());
            holder.date.setText(filelog.getLogDate());
        }catch(Exception e){
            e.printStackTrace();
        }
		return rowView;
	}
	class ViewHolder {
		TextView filename;
        TextView hash;
        TextView from;
        TextView to;
        TextView date;
	}
	
}
