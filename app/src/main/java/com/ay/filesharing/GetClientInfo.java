package com.ay.filesharing;

import protocol.DistributedFileSharingProtocol;
import file.ActiveLogin;
import file.ActiveUser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

public class GetClientInfo extends AsyncTask<Void, Void, byte[]>{
		private ActiveUser activeUser;
		private ActiveLogin activeLogin;
		private ImageView imageView;
		public GetClientInfo(ImageView imageView, ActiveUser user, ActiveLogin login){
			this.activeUser = user;
			this.activeLogin = login;
			this.imageView = imageView;
		}
		@Override
		protected void onPostExecute(byte[] result) {
			try{
				Bitmap b = BitmapFactory.decodeByteArray(result , 0, result.length);
				imageView.setImageBitmap(b);
			}catch(NullPointerException e){
				Log.e(e.getLocalizedMessage(), "from "+ activeUser.username+" "+ activeUser.ip);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		@Override
		protected byte[] doInBackground(Void... arg0) {
			byte[] requestClientInfo = DistributedFileSharingProtocol.getInstance()
					.requestClientInfo(activeUser, activeLogin);
			return requestClientInfo;
		}
		
	}