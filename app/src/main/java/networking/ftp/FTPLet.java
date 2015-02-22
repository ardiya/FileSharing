package networking.ftp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.ay.filesharing.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.ftpserver.ftplet.DefaultFtplet;

import org.apache.ftpserver.ftplet.FtpException;

import org.apache.ftpserver.ftplet.FtpRequest;

import org.apache.ftpserver.ftplet.FtpSession;

import org.apache.ftpserver.ftplet.FtpletResult;

import file.ActiveUser;
import util.StateManager;

public class FTPLet extends DefaultFtplet {
    public FTPLet(Context context){
        this.context = context;
    }
    Context context;
    @Override
	public FtpletResult onUploadEnd(FtpSession session, FtpRequest request)
			throws FtpException, IOException {

        Log.d("FPTLet", "onUploadEn[request.getCommand()] " + request.getCommand());
        Log.d("FTPLet", "onUploadEnd[request.getArgument()]"+
                (request.hasArgument()?request.getArgument():"null"));
        Log.d("FTPLet", "onUploadEnd[request.getRequestLine()]"+request.getRequestLine());
        ArrayList<ActiveUser> activeUsers = (ArrayList<ActiveUser>) StateManager.getItem("activeuser");
        String username = null;
        String ip = session.getClientAddress().getAddress().getHostAddress();
        for(ActiveUser u: activeUsers) {
            Log.d("ActiveUser", u.username+":"+u.ip);
            if (u.ip.equals(ip))
                username = u.username;
        }
        String userRoot=session.getUser().getHomeDirectory();
        String currDir=session.getFileSystemView().getWorkingDirectory().getAbsolutePath();
        String fileName=request.getArgument();
        String path = userRoot + fileName;
        showNotification(request.getArgument()
                , "Added " + request.getArgument() +
                    (username==null?
                        (" from IP:" + session.getClientAddress().getAddress().getHostAddress())
                        : (" by "+username))
                ,path);
		return FtpletResult.DEFAULT;
	}

    public void showNotification(String filename, String description, String path){
        NotificationManager systemService = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentTitle(filename).setContentText(description)
                .setSmallIcon(R.drawable.ic_launcher);
        mBuilder.setAutoCancel(true);
        try
        {
            Log.d("Creating notification for "+filename, "with path"+path);
            Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
            File file = new File(path);
            if(file.exists()){
                String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
                String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                myIntent.setDataAndType(Uri.fromFile(file), mimetype);
                myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pending = PendingIntent.getActivity(context, 0,
                        myIntent, 0);
                mBuilder.setContentIntent(pending);
                Log.d("FTPListener", "created pending intent");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        systemService.notify(77112, mBuilder.build());
    }
}