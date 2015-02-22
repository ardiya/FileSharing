package networking.ftp;

import android.content.Context;
import android.util.Log;

import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import file.FTPUser;

public class FtpServer {
	private static FtpServer instance = null;
	private UserManager userManager;
	private org.apache.ftpserver.FtpServer ftpServer;

	public static FtpServer getInstance(Context ct) {
		if (instance == null) {
			instance = new FtpServer();
			instance.startServer(ct);
			Log.d("FTPServer", "File Transfer Server Started");
		}
		return instance;
	}

	private void startServer(Context context) {
		try {
			PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
			File usermanager = new File(context.getFilesDir().getAbsolutePath() + "/fuser");
			if (usermanager.exists()) {
				usermanager.delete();
			}
			
			usermanager.createNewFile();
			userManagerFactory.setFile(usermanager);
			userManagerFactory
					.setPasswordEncryptor(new SaltedPasswordEncryptor());
			
			this.userManager = userManagerFactory.createUserManager();

			FtpServerFactory serverFactory = new FtpServerFactory();
			ListenerFactory factory = new ListenerFactory();
			factory.setPort(2225);

			Listener ls = factory.createListener();

			serverFactory.addListener("default", ls);

            HashMap<String, Ftplet> ftplets = new HashMap<String, Ftplet>(0);
            ftplets.put("DEFAULT", new FTPLet(context));
            serverFactory.setFtplets(ftplets);

			serverFactory.setUserManager(this.userManager);
			this.ftpServer = serverFactory.createServer();
			this.ftpServer.start();
		} catch (FtpException | IOException ex) {
			ex.printStackTrace();
		}
	}

	public FTPUser generatePassword(String homeDirectory) {
		try {
			FTPUser ftpuser = new FTPUser();
			ftpuser.username = generateRandom(10);
			ftpuser.password = generateRandom(10);

			BaseUser user = new BaseUser();
			user.setName(ftpuser.username);
			user.setPassword(ftpuser.password);
			user.setHomeDirectory(homeDirectory);
			List<Authority> authorities = new ArrayList();
			authorities.add(new WritePermission());
			user.setAuthorities(authorities);

			Log.d("Generated FTP Username", ftpuser.username);
			Log.d("Generated FTP Password", ftpuser.password);
			
			this.userManager.save(user);
			return ftpuser;
		} catch (FtpException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	private String generateRandom(int length) {
		try {
			String digits = "";
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			for (int i = 1; i < length; i++) {
				digits = digits + String.valueOf(random.nextInt(9));
			}
			return digits;
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
