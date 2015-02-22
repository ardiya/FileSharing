package com.ay.filesharing.view;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ay.filesharing.R;

import java.util.ArrayList;

import file.ActiveUser;
import file.ChatMessage;

public class ChatArrayAdapter extends ArrayAdapter<ChatMessage> {

	private ActiveUser targetChat;

	public ChatArrayAdapter(Context context, int textViewResourceId, ActiveUser targetChat, ArrayList<ChatMessage> chatMessageList) {
		super(context, textViewResourceId, chatMessageList);
		this.targetChat = targetChat;
	}


	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.view_message, parent, false);
		}
		LinearLayout singleMessageContainer = (LinearLayout) row.findViewById(R.id.singleMessageContainer);
		ChatMessage chatMessageObj = getItem(position);
        TextView chatText = (TextView) row.findViewById(R.id.singleMessage);
		chatText.setText(chatMessageObj.message);
		chatText.setBackgroundResource(chatMessageObj.to.equals(targetChat.username) ? R.drawable.bubble_b : R.drawable.bubble_a);
        chatText.setTextColor(
            chatMessageObj.to.equals(targetChat.username) ?
                    getContext().getResources().getColor(R.color.grey_600)
                    : Color.rgb(8,124,183)
        );
        singleMessageContainer.setGravity(chatMessageObj.to.equals(targetChat.username) ? Gravity.LEFT : Gravity.RIGHT);
		return row;
	}
}