package com.ay.filesharing;

import java.util.ArrayList;

import protocol.DistributedFileSharingProtocol;
import util.StateManager;
import file.ActiveLogin;
import file.ActiveUser;
import file.ChatMessage;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.ay.filesharing.view.ChatArrayAdapter;

public class ChatActivity extends ActionBarActivity {

	final public static String TAG_CHAT_TARGET = "target";
	final public static String TAG_ACTIVE_LOGIN = "activelogin";
	private static final String TAG = "ChatActivity";

	ActiveUser targetChat;
	private ChatArrayAdapter chatArrayAdapter;
	private ListView listView;
	private EditText chatText;
	private ImageButton buttonSend;

	Intent intent;
	private ActiveLogin activeLogin;
	private ArrayList<ChatMessage> chatMessageList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (null == chatMessageList) {
			chatMessageList = new ArrayList<ChatMessage>();
		}
		Intent i = getIntent();
		targetChat = (ActiveUser) i.getExtras().get(TAG_CHAT_TARGET);
		activeLogin = (ActiveLogin) i.getExtras().get(TAG_ACTIVE_LOGIN);
		Log.d("targetChat", targetChat==null?"null":targetChat.username);
		setTitle(targetChat.username);
		setContentView(R.layout.activity_chat);

		buttonSend = (ImageButton) findViewById(R.id.buttonSend);
		listView = (ListView) findViewById(R.id.listView1);

		chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(),
				R.layout.view_message, targetChat, chatMessageList);
		listView.setAdapter(chatArrayAdapter);
		
		fillMessageList();

		
		if (activeLogin == null){
			Toast.makeText(getApplicationContext(), "No Active Login", Toast.LENGTH_SHORT)
					.show();
			finish();
		}

		chatText = (EditText) findViewById(R.id.chatText);
		chatText.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					new SendChatMessage().execute();
					return true;
				}
				return false;
			}
		});

		buttonSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				new SendChatMessage().execute();
			}
		});

		listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		listView.setAdapter(chatArrayAdapter);

		// to scroll the list view to bottom on data change
		chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				super.onChanged();
				listView.setSelection(chatArrayAdapter.getCount() - 1);
			}
		});

		rec = createBroadcastReceiver();
		LocalBroadcastManager.getInstance(this).registerReceiver(
				rec,
				new IntentFilter("com.ay.filesharing.ChatActivity."
						+ targetChat.ip));
		//when opened, scroll to the bottom
		listView.setSelection(chatArrayAdapter.getCount() - 1);
	}

	BroadcastReceiver rec;

	private BroadcastReceiver createBroadcastReceiver() {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String method = (String) intent.getExtras().get("method");
				Log.e("ChatActivity", "BroadcastReceived method " + method);
				switch (method) {
				case "updateChatList":
					fillMessageList();
					break;
				}
			}

		};
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (rec != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(rec);
		}
	}

	private void fillMessageList() {
		chatMessageList.clear();
		ArrayList<file.ChatMessage> chatMessages = (ArrayList<ChatMessage>) StateManager
				.getItem("ChatMessage");
		if (chatMessages == null)
			chatMessages = new ArrayList<ChatMessage>();
		Log.d("fillMessageList","populating");
		for (file.ChatMessage message : chatMessages)
		{
			Log.d("targetChat.username", targetChat.username);
			Log.d("message.from", String.format("'%s'",message.from));
			Log.d("message.to", String.format("'%s'",message.to==null?"null":message.to));
			Log.d("message.message", message.message==null?"null":message.message);
			if (message.from.equals(targetChat.username) || message.to.equals(targetChat.username))
				chatMessageList.add(message);
		}
		//chatMessageList.addAll(chatMessages);
		chatArrayAdapter.notifyDataSetChanged();
	}

	class SendChatMessage extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			String msg = chatText.getText().toString();
			if (result) {
				ArrayList<file.ChatMessage> chatMessage = (ArrayList<ChatMessage>) StateManager
						.getItem("ChatMessage");
				if (chatMessage == null)
					chatMessage = new ArrayList<ChatMessage>();
				chatMessage.add(new ChatMessage(activeLogin.username,
						targetChat.username, msg));
				StateManager.setItem("ChatMessage", chatMessage);

				chatArrayAdapter.add(new ChatMessage(activeLogin.username,
						targetChat.username, msg));
				chatText.setText("");
			} else {
				chatArrayAdapter.add(new ChatMessage(activeLogin.username,
						targetChat.username, "Failed to send message"));
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			String msg = chatText.getText().toString();
			DistributedFileSharingProtocol protocol = DistributedFileSharingProtocol
					.getInstance();
			return protocol.requestChat(activeLogin, msg, targetChat.ip);
		}

	}
}
