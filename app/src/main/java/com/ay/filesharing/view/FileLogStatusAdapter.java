package com.ay.filesharing.view;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ay.filesharing.R;

import java.util.ArrayList;

import file.FileLogStatus;

public class FileLogStatusAdapter extends ArrayAdapter<FileLogStatus> {
	public FileLogStatusAdapter(Context context, ArrayList logs) {
		super(context, R.layout.view_log, logs);
		selectedItemsId = new SparseBooleanArray();
	}
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		ViewHolder holder;
		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.view_log, parent, false);
			holder = new ViewHolder();
			holder.filename = (TextView) rowView.findViewById(R.id.textView1);
			holder.status = (TextView) rowView.findViewById(R.id.textView2);
            holder.img = (ImageView) rowView.findViewById(R.id.imageView1);
			rowView.setTag(holder);
		} else {
			holder = (ViewHolder) rowView.getTag();
		}
		FileLogStatus filelog = getItem(position);
		holder.filename.setText(filelog.getFileName());
		holder.status.setText(filelog.getStatus());
        holder.img.setImageDrawable(getContext().getResources().getDrawable(
                filelog.getType().equals("download")? R.drawable.ic_download:R.drawable.ic_upload
        ));
		return rowView;
	}
	class ViewHolder {
		TextView filename;
		TextView status;
        ImageView img;
	}
	private SparseBooleanArray selectedItemsId;
	public SparseBooleanArray getSelectedIds() {
		return selectedItemsId;
	}
	public void toggleSelection(int position) {
		selectView(position, !selectedItemsId.get(position));
	}
	public void selectView(int position, boolean value) {
		if (value)
			selectedItemsId.put(position, value);
		else
			selectedItemsId.delete(position);
		notifyDataSetChanged();
	}
	public void removeSelection() {
		selectedItemsId = new SparseBooleanArray();
		notifyDataSetChanged();
	}
	public int getSelectedCount() {
		return selectedItemsId.size();
	}
}
