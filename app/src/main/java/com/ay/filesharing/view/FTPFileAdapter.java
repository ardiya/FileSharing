package com.ay.filesharing.view;

import it.sauronsoftware.ftp4j.FTPFile;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ay.filesharing.R;

public class FTPFileAdapter extends ArrayAdapter<FTPFile> {

	public FTPFileAdapter(Context context, int textViewResourceId,
			ArrayList<FTPFile> ftpList) {
		super(context, textViewResourceId, ftpList);
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
        try {
            final FTPFile file = getItem(position);
            if (file.getType() == FTPFile.TYPE_DIRECTORY) {
                holder.img.setImageDrawable(getContext().getResources().getDrawable(
                        R.drawable.ic_folder));
            } else {
                holder.img.setImageDrawable(getContext().getResources().getDrawable(
                        R.drawable.ic_file));
            }
            holder.txt.setText(file.getName());
        }catch (Exception e){}
		return rowView;
	}
	class ViewHolder {
		ImageView img;
		TextView txt;
	}
}