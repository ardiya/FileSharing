package networking.ftp;

import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cryptography.EncryptionUtility;
import file.FileLog;
import file.FileLogManager;
import util.SqliteUtil;

import com.ay.filesharing.R;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import file.FileLogStatus;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;

public class FTPListener implements FTPDataTransferListener {

	private FileLogStatus fileLogStatus;
	private Long fileSize;
	private Long transferedSize = Long.parseLong("0");
	private long deltaUpdate = 0;
	private Builder notificationBuilder;
	private NotificationManager notificationManager;
	private Context context;
	private String contentText;

	/**
	 * Class Constructor
	 * 
	 * @param size
	 *            file size
	 * @param fls
	 *            file log status
	 */
	public FTPListener(Long size, FileLogStatus fls, Context context,
			String contentText) {
		this.fileSize = size;
		this.fileLogStatus = fls;
		this.context = context;
		this.contentText = contentText;
		notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationBuilder = new NotificationCompat.Builder(context);
		notificationBuilder.setContentTitle(fls.getFileName()).setContentText(contentText)
				.setSmallIcon(R.drawable.ic_launcher);
		SqliteUtil sqlite = new SqliteUtil(context);
		try {
			sqlite.insert(fls);
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			sqlite.close();
		}
	}

	/**
	 * Get File SIze
	 * 
	 * @return fileSize
	 */
	public Long getFileSize() {
		return fileSize;
	}

	/**
	 * Set File Size
	 * 
	 * @param fileSize
	 *            file size
	 */
	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	/**
	 * Get Transfered Size
	 * 
	 * @return transferedSize
	 */
	public Long getTransferedSize() {
		return transferedSize;
	}

	/**
	 * Set Transfered Size
	 * 
	 * @param transferedSize
	 *            transfer size
	 */
	public void setTransferedSize(Long transferedSize) {
		this.transferedSize = transferedSize;
	}

	/**
	 * getProgress
	 * 
	 * @return test
	 */
	public double getProgress() {
		double test = (double) transferedSize * 100 / (double) fileSize;
		return test;
	}

	@Override
	public void started() {
		// Transfer started
	}

	/**
	 * If Transfered
	 * 
	 * @param length
	 *            byte length
	 */
	@Override
	public void transferred(int length) {
		// Yet other length bytes has been transferred since the last time this
		// method was called

		transferedSize += length;
		fileLogStatus.setProgress(getProgress());

		if (deltaUpdate + length > 200000) {
			reportLogDataUpdated(fileLogStatus);
			deltaUpdate = 0;
		} else {
			deltaUpdate += length;
		}
	}

	private void reportLogDataUpdated(FileLogStatus fls) {
		notificationBuilder.setProgress(100, (int) Math.round(fls.getProgress()), false);
		notificationManager.notify(fls.getId(), notificationBuilder.build());
		
		if (fls.getStatus().equals("Done")) {
			notificationBuilder.setProgress(0, 0, false);
            notificationBuilder.setAutoCancel(true);
			notificationBuilder.setContentText(
                    (fls.getType().equals("upload")?"Send":"Fetch") + " complete");
			if (fls.getType().equals("Download")) {
                //Update LogFragment
                Intent x = new Intent("com.ay.filesharing.LogFragment");
                x.putExtra("method", "updateLogList");
                LocalBroadcastManager.getInstance(context).sendBroadcast(x);
				try
				{
                    //Create On Click Notification
					Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
					File file = new File(fls.getPath());
					if(file.exists()){
						String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
						String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
						                myIntent.setDataAndType(Uri.fromFile(file), mimetype);
						myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						PendingIntent pending = PendingIntent.getActivity(context, 0,
								myIntent, 0);
						notificationBuilder.setContentIntent(pending);
						Log.d("FTPListener", "created pending intent");
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}				
			}else if(fls.getStatus().equals("Failed")){
                notificationBuilder.setProgress(0, 0, false);
                notificationBuilder.setAutoCancel(true);
                notificationBuilder.setContentText( (fls.getType().equals("upload")?"Send":"Fetch") + " failed");
            }
            //update to database
			if(fls.getStatus().equals("Done")||fls.getStatus().equals("Aborted")||fls.getStatus().equals("Failed"))
			{
                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                long[] pattern = {500,500};
                notificationBuilder.setVibrate(pattern);
                notificationBuilder.setSound(alarmSound);
				SqliteUtil sqlite = new SqliteUtil(context);
				try {
					sqlite.update(fls);
				} catch (SQLException e) {
					e.printStackTrace();
				}finally{
					sqlite.close();
				}
                FileLog fileLog = new FileLog();
                fileLog.setFilename(fls.getFileName());
                fileLog.setHash(EncryptionUtility.hashFile(fls.getPath()));
                fileLog.setLogDate(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
                fileLog.setUsernameFrom(fls.getFrom());
                fileLog.setUsernameTo(fls.getTo());
                new FileLogManager(context).addFileLog(fls.getNetworkName(), fileLog, fls.getTo());
			}
            notificationManager.notify(fls.getId(), notificationBuilder.build());
			
		}
		Log.d("FTPListener filename", fls.getFileName());
		Log.d("FTPListener progress", "" + fls.getProgress());
		Log.d("FTPListener status", fls.getStatus());
	}

	/**
	 * If Completed
	 * 
	 */
	@Override
	public void completed() {
		// Transfer completed
		fileLogStatus.setStatus("Done");
		fileLogStatus.setProgress(Double.valueOf(100));
		reportLogDataUpdated(fileLogStatus);
	}

	/**
	 * If Aborted
	 * 
	 */
	@Override
	public void aborted() {
		// Transfer aborted
		fileLogStatus.setStatus("Aborted");
		reportLogDataUpdated(fileLogStatus);
	}

	/**
	 * If Failed
	 * 
	 */
	@Override
	public void failed() {
		fileLogStatus.setStatus("Failed");
		reportLogDataUpdated(fileLogStatus);
	}

}
