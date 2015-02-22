package com.ay.filesharing.view;



import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ay.filesharing.R;

import java.io.File;
import java.util.ArrayList;

public class FileAdapter extends ArrayAdapter<File> {

	public FileAdapter(Context context, int textViewResourceId,
			ArrayList<File> fileList) {
		super(context, textViewResourceId, fileList);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		ViewHolder holder;
		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.view_file, parent, false);
			holder = new ViewHolder();
			holder.img = (ImageView) rowView.findViewById(R.id.img);
			holder.txt = (TextView) rowView.findViewById(R.id.txt);
			rowView.setTag(holder);
		} else {
			holder = (ViewHolder) rowView.getTag();
		}
		final File file = getItem(position);
		if (file.isDirectory()) {
			holder.img.setImageDrawable(getContext().getResources().getDrawable(
					R.drawable.ic_folder));
		} else {
			holder.img.setImageDrawable(getContext().getResources().getDrawable(
					R.drawable.ic_file));
		}
		holder.txt.setText(file.getName());

		return rowView;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int arg0) {
		return true;
	}

	class ViewHolder {
		ImageView img;
		TextView txt;
	}
}