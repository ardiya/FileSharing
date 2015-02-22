package com.ay.filesharing;

import java.util.ArrayList;

import protocol.DistributedFileSharingProtocol;
import util.StateManager;
import file.ActiveLogin;
import file.ActiveUser;
import file.ChatMessage;

import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.ay.filesharing.view.ChatArrayAdapter;

public class ChatFragment extends Fragment {

	ActiveUser targetChat;
	private ChatArrayAdapter chatArrayAdapter;
	private ListView listViewChatMessage;
	private EditText editTextNewMessage;
	private ImageButton buttonSend;

	Intent intent;
	private ActiveLogin activeLogin;
	private ArrayList<ChatMessage> chatMessageList;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate( R.layout.fragment_chat, container,
                false);
		if (null == chatMessageList) {
			chatMessageList = new ArrayList<ChatMessage>();
		}
		Bundle bundle = getArguments();
		targetChat = (ActiveUser) bundle.get(ClientActivity.TAG_CLIENT);
		activeLogin = (ActiveLogin) bundle.get(ClientActivity.TAG_ACTIVE_LOGIN);
		Log.d("targetChat", targetChat == null ? "null" : targetChat.username);

		buttonSend = (ImageButton) rootView.findViewById(R.id.buttonSend);
		listViewChatMessage = (ListView) rootView.findViewById(R.id.listView1);

		chatArrayAdapter = new ChatArrayAdapter(getActivity(),
				R.layout.view_message, targetChat, chatMessageList);
		listViewChatMessage.setAdapter(chatArrayAdapter);
		
		fillMessageList();

		
		if (activeLogin == null){
			Toast.makeText(getActivity(), "No Active Login", Toast.LENGTH_SHORT)
					.show();
            getActivity().finish();
		}

		editTextNewMessage = (EditText) rootView.findViewById(R.id.chatText);
		editTextNewMessage.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if(!editTextNewMessage.getText().toString().trim().equals("")) new SendChatMessage().execute();
                    return true;
                }
                return false;
            }
        });

		buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(!editTextNewMessage.getText().toString().trim().equals(""))
                    new SendChatMessage().execute();
            }
        });

		listViewChatMessage.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		listViewChatMessage.setAdapter(chatArrayAdapter);

		// to scroll the list view to bottom on data change
		chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				super.onChanged();
				listViewChatMessage.setSelection(chatArrayAdapter.getCount() - 1);
			}
		});

		broadcastReceiver = createBroadcastReceiver();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver,
				new IntentFilter("com.ay.filesharing.ChatActivity."
						+ targetChat.ip));
		//when opened, scroll to the bottom
		listViewChatMessage.setSelection(chatArrayAdapter.getCount() - 1);
        return  rootView;
	}

	BroadcastReceiver broadcastReceiver;

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
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

	private void fillMessageList() {
		chatMessageList.clear();
		ArrayList<file.ChatMessage> chatMessages = (ArrayList<ChatMessage>) StateManager
				.getItem("ChatMessage");
		if (chatMessages == null)
			chatMessages = new ArrayList<>();
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
			String msg = editTextNewMessage.getText().toString();
			if (result) {
				ArrayList<file.ChatMessage> chatMessage = (ArrayList<ChatMessage>) StateManager
						.getItem("ChatMessage");
				if (chatMessage == null)
					chatMessage = new ArrayList<>();
				chatMessage.add(new ChatMessage(activeLogin.username,
						targetChat.username, msg));
				StateManager.setItem("ChatMessage", chatMessage);

				chatArrayAdapter.add(new ChatMessage(activeLogin.username,
						targetChat.username, msg));
				editTextNewMessage.setText("");
			} else {
				chatArrayAdapter.add(new ChatMessage(activeLogin.username,
						targetChat.username, "Failed to send message"));
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			String msg = editTextNewMessage.getText().toString();
			DistributedFileSharingProtocol protocol = DistributedFileSharingProtocol
					.getInstance();
			return protocol.requestChat(activeLogin, msg, targetChat.ip);
		}

	}
}
