package com.ay.filesharing;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import cryptography.EncryptionUtility;
import file.ActiveLogin;
import file.ActiveUser;
import file.ChatMessage;
import file.DeletedNetworkList;
import file.DeletedUser;
import file.DeletedUserList;
import file.FTPUser;
import file.FileLog;
import file.FileLogManager;
import file.Network;
import file.NetworkList;
import file.NetworkUserDetail;
import file.NetworkUserTable;
import file.SignatureContainer;
import file.UnapprovedNetworkUserTable;
import file.User;
import networking.ftp.FtpServer;
import networking.httpd.HTTPClient;
import networking.httpd.NanoHTTPD;
import networking.util.ProtocolStructure;
import networking.util.SerializableProtocolStructure;
import protocol.DistributedFileSharingProtocol;
import util.DatatypeConverter;
import util.ObjectConverter;
import util.SqliteUtil;
import util.StateManager;

public class FileSharingService extends Service {
	private final IBinder serviceBinder = new ServiceBinder();
	UDPServer udpServer;
	Thread udpServerThread;
	HTTPServer httpServer;

	@Override
	public void onCreate() {
		super.onCreate();
		loadDBActiveLogin();
		udpServer = new UDPServer();
		udpServerThread = new Thread(udpServer);
		httpServer = new HTTPServer();
		try {
			httpServer.start();
			udpServerThread.start();
			FtpServer server = FtpServer.getInstance(getApplicationContext());
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.d("FileSharingService", "Started");
		StateManager.setItem("RequestActiveUserEnabled", true);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (httpServer != null)
			httpServer.stop();
		if (udpServer != null && udpServerThread.isAlive()) {
			udpServerThread.stop();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		return Service.START_NOT_STICKY;
	}

	void loadDBActiveLogin(){
		SqliteUtil util = new SqliteUtil(getApplicationContext());
		try {
			Log.d("loadDBActiveLogin", "Loading");
			ArrayList<ActiveLogin> query = (ArrayList) util.query(ActiveLogin.class).queryForAll();
			if(query != null && query.size()>0){
				StateManager.setItem("ActiveLogin", query);
			}
			
			Log.d("loadDBActiveLogin", "Finished");
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			util.close();
		}
	}
	
	public void sendMessageToUI(String uiname, String method) {
		Intent x = new Intent(uiname);
		x.putExtra("method", method);
		LocalBroadcastManager.getInstance(this).sendBroadcast(x);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return serviceBinder;
	}

	public class ServiceBinder extends Binder {
		FileSharingService getService() {
			return FileSharingService.this;
		}
	}

	class UDPServer implements Runnable {
		DatagramSocket socket;
		ArrayList<String> myIP;

		public UDPServer() {
			this.myIP = new ArrayList();
		}

		public void run() {
			try {
				this.socket = new DatagramSocket(8889,
						InetAddress.getByName("0.0.0.0"));
				this.socket.setBroadcast(true);

				Enumeration interfaces = NetworkInterface
						.getNetworkInterfaces();
				while (interfaces.hasMoreElements()) {
					NetworkInterface networkInterface = (NetworkInterface) interfaces
							.nextElement();
					for (InterfaceAddress interfaceAddress : networkInterface
							.getInterfaceAddresses()) {
						this.myIP.add(interfaceAddress.getAddress().toString());
					}
				}
				Log.e("UDP Server", "Listening");
				for (;;) {
					byte[] recvBuf = new byte[1500];
					DatagramPacket packet = new DatagramPacket(recvBuf,
							recvBuf.length);
					this.socket.receive(packet);
					// Log.e("UDP Server", "UDP Received");
					byte[] data = packet.getData();

					Arrays.copyOfRange(data, 0, packet.getLength());

					SerializableProtocolStructure p = (SerializableProtocolStructure) ObjectConverter
							.byteToObject(data);

					ProtocolStructure s = new ProtocolStructure();
					s.command = p.command;
					s.payload = p.payload;
					s.protocolVersion = p.protocolVersion;
					s.sendType = "UDP";
					s.senderIP = packet.getAddress().getHostAddress();
                    if(packet.getAddress().isMulticastAddress()) continue;
                    if(s.command.equals("REAU")||s.command.equals("RERU")){
                        //Refresh List IP in case of IP Network Changed
                        this.myIP.clear();
                        interfaces = NetworkInterface
                                .getNetworkInterfaces();
                        while (interfaces.hasMoreElements()) {
                            NetworkInterface networkInterface = (NetworkInterface) interfaces
                                    .nextElement();
                            for (InterfaceAddress interfaceAddress : networkInterface
                                    .getInterfaceAddresses()) {
                                this.myIP.add(interfaceAddress.getAddress().toString());
                            }
                        }
                    }
					if (!this.myIP.contains("/" + s.senderIP) && !this.myIP.contains(s.senderIP)) {
						Log.d("UDP command", p.command);
						Log.d("UDP protocolVersion", p.protocolVersion + "");
						handleUDPRequest(s);
					}
				}
			} catch (SocketException ex) {
				ex.printStackTrace();
			} catch (UnknownHostException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			finally{
				Log.e("UDP Server", "Stopped");
			}
		}
		/**
		 * @param data
		 *            Protocol Structure data from UDP Request
		 */
		void handleUDPRequest(ProtocolStructure data) {
			final SerializableProtocolStructure message = new SerializableProtocolStructure();
			Gson gson = new Gson();
			Log.d("Handling UDP Request", data.command);
			switch (data.command) {
			case "RENN":
				ArrayList<Network> nt = new NetworkList()
						.getNetworkTable(getApplicationContext());

				message.sPayload = gson.toJson(nt);
				message.command = "RENN";
				message.protocolVersion = 1;

				HTTPClient.requestPost(data.senderIP, 8888, message);
				break;
			case "REAU":
				SignatureContainer c = (SignatureContainer) ObjectConverter
						.byteToObject(data.payload);

				// Check What Network I'm Active with & admin status
				ArrayList<ActiveLogin> aalu = (ArrayList<ActiveLogin>) StateManager
						.getItem("ActiveLogin");
				if (aalu == null) {
					aalu = new ArrayList();
				}
				ActiveLogin ul = null;
				for (int i = 0; i < aalu.size(); i++) {
					ActiveLogin temp = aalu.get(i);
					if (temp.ActiveNetwork.equals(c.Type)) {
						ul = aalu.get(i);
					}
				}
				if (ul == null) {
					return;
				}

				// Return saya aktif
				SignatureContainer sco = new SignatureContainer();
				sco.Type = c.Type;
				sco.Username = ul.username;
				sco.object = c.object;
				sco.signature = DatatypeConverter
						.printBase64Binary(EncryptionUtility.generateSignature(
								ObjectConverter.ObjectToByte(sco.object),
								ul.privKey));

				message.sPayload = ObjectConverter.ObjectToString(sco);
				message.command = "REAU";
				message.protocolVersion = 1;

				HTTPClient.requestPost(data.senderIP, 8888, message);
				Log.d("REAU Reply", "sent to " + data.senderIP + ":8888");

				break;
			case "RERU":
				// Cek signature dia
				SignatureContainer sac = (SignatureContainer) ObjectConverter
						.byteToObject(data.payload);

				// Get Older user Network table , put the new user , Dont forget to
				// check username already exist
				ArrayList<SignatureContainer> signaturec = NetworkUserTable
						.getNetworkUserTable(getApplicationContext(),
								(String) sac.Type, null);
				boolean ketemu = false;
				User requester = null;

				for (int i = 0; i < signaturec.size(); i++) {
					NetworkUserDetail networkUd = (NetworkUserDetail) ObjectConverter
							.StringToObject(signaturec.get(i).object);
					ArrayList<User> ual = networkUd.userlist;
					for (int j = 0; j < ual.size(); j++) {
						requester = ual.get(j);
						if (requester.username.equals(sac.Username)) {
							ketemu = true;
							break;
						}
					}
					if (ketemu) {
						break;
					}
				}

				if (requester == null) {
					break;
				}

				if (ketemu) {
					String sig = (String) sac.object;
					DistributedFileSharingProtocol instance = DistributedFileSharingProtocol
							.getInstance();
					if (EncryptionUtility.verifySignature(sig.getBytes(),
							DatatypeConverter.parseBase64Binary(sac.signature),
							DatatypeConverter
									.parseBase64Binary(requester.publicKey))) {
						if (instance != null) {
							// Check What Network I'm Active with & admin status
							ArrayList<ActiveLogin> activeLoginList = (ArrayList<ActiveLogin>) StateManager
									.getItem("ActiveLogin");
							if (activeLoginList == null) {
								activeLoginList = new ArrayList<ActiveLogin>();
							}
							ActiveLogin activeLogin = null;
							for (int i = 0; i < activeLoginList.size(); i++) {
								ActiveLogin temp = activeLoginList.get(i);
								if (temp.ActiveNetwork.equals(sac.Type)) {
									activeLogin = activeLoginList.get(i);
								}
							}
							if (activeLogin == null) {
								return;
							}

							instance.requestActiveUser(sac.Type,
									activeLogin.privKey);


						}
					}
				}
				break;
			case "REDU":
	            ArrayList<DeletedUser> netsa = DeletedUserList.getDeletedUserList(getApplicationContext());

	            if (netsa.size() < 1) {
	                return;
	            }
	            message.sPayload = gson.toJson(netsa);
	            message.command = "REDU";
	            message.protocolVersion = 1;
	            HTTPClient.requestPost(data.senderIP, 8888, message);
	            break;
			case "RNUT":
	            ArrayList<ActiveLogin> aal = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
	            if (aal == null) {
	                aal = new ArrayList();
	            }
	            ActiveLogin u = null;
	            for (int i = 0; i < aal.size(); i++) {
	                ActiveLogin temp = aal.get(i);
	                if (temp.ActiveNetwork.equals(new String(data.payload))) {
	                    u = aal.get(i);
	                }
	            }

	            if (u == null) {
                    break;
	            }

	            if (u.role.equals("client")) {
                    Log.e("RNUT[drop]","role of current active login is client");
	                break;
	            }
	            //Check if I'm admin in usernettable
	            final ArrayList<SignatureContainer> sc = NetworkUserTable.getNetworkUserTable(getApplicationContext(), new String(data.payload), null);
	            if (sc == null) {
                    Log.e("RNUT[drop]","No data in getNetworkUserTable");
                    break;
	            }
                final SerializableProtocolStructure finalMessage = message;
                final String ip = data.senderIP;
                final ArrayList<SignatureContainer>finalSc = sc;
	            Thread t = new Thread(new Runnable() {
	                @Override
	                public void run() {
	                    for (int i = 0; i < sc.size(); i++) {
	                        message.sPayload = ObjectConverter.ObjectToString(finalSc.get(i));
	                        message.command = "RNUT";
	                        message.protocolVersion = 1;
                            HTTPClient.requestPost(ip, 8888, message);
	                        try {
	                            Thread.sleep(100);
	                        } catch (InterruptedException ex) {
	                            ex.printStackTrace();
	                        }

	                    }
	                }
	            });
	            t.start();
	            break;
			case "RENA":
	            SignatureContainer ca = (SignatureContainer) ObjectConverter.byteToObject(data.payload);
	            ArrayList<ActiveLogin> aalux = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
	            if (aalux == null) {
	                aalux = new ArrayList();
	            }
	            ActiveLogin ulx = null;
	            for (int i = 0; i < aalux.size(); i++) {
	                ActiveLogin temp = aalux.get(i);
	                if (temp.ActiveNetwork.equals(ca.Type)) {
	                    ulx = aalux.get(i);
	                }
	            }
	            if (ulx == null) {
	                return;
	            }

	            if (!ulx.role.contentEquals("admin") && !ulx.role.contentEquals("clientadmin") && !ulx.role.contentEquals("creator")) {
                    Log.d("FileSharingService", ulx.role+"is invalid role for admin");
	                return;
	            }
	            SignatureContainer scon = new SignatureContainer();
	            scon.Type = (String) ca.Type;
	            scon.object = ca.object;
	            scon.Username = ulx.username;
	            scon.signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(ObjectConverter.ObjectToByte(scon.object), ulx.privKey));

	            message.sPayload = ObjectConverter.ObjectToString(scon);
	            message.command = "RENA";
	            message.protocolVersion = 1;

	            HTTPClient.requestPost(data.senderIP, 8888, message);

	            break;
			case "RERN":
	            SignatureContainer cax = (SignatureContainer) ObjectConverter.byteToObject(data.payload);

	            //Get Older user Network table , put the new user , Dont forget to check username already exist
	            ArrayList<SignatureContainer> sca = NetworkUserTable.getNetworkUserTable(getApplicationContext(),(String) cax.object, null);
	            boolean found = false;
	            User uuu = null;
	            for (int i = 0; i < sca.size(); i++) {
	                NetworkUserDetail nUd = (NetworkUserDetail) ObjectConverter.StringToObject(sca.get(i).object);
	                ArrayList<User> ual = nUd.userlist;
	                for (int j = 0; j < ual.size(); i++) {
	                    uuu = ual.get(j);
	                    if (uuu.username.equals(cax.Username)) {
	                        found = true;
	                        break;
	                    }
	                }
	                if (found) {
	                    break;
	                }
	            }
	            if (uuu == null) {
	                break;
	            }
	            if (found) {
	                String nn = (String) cax.object;
	                if (EncryptionUtility.verifySignature(nn.getBytes(), DatatypeConverter.parseBase64Binary(cax.signature), DatatypeConverter.parseBase64Binary(uuu.publicKey))) {
	                    DistributedFileSharingProtocol.getInstance().requestGetNetworkUserTable(nn);
	            	}
	            }
	            break;
	          //Request delete network
	        case "REDN":
	            SignatureContainer siCo = (SignatureContainer) ObjectConverter.byteToObject(data.payload);
	            //Verifikasi signature
	            //Cek signature Network User Description
	            String pk = null;
	            ArrayList<SignatureContainer> scko = NetworkUserTable.getNetworkUserTable(getApplicationContext(), siCo.Type, null);
	            for (int i = 0; i < scko.size(); i++) {
	                SignatureContainer sck = scko.get(i);
	                NetworkUserDetail nUdesc = (NetworkUserDetail) ObjectConverter.StringToObject(sck.object);
	                //Get Username
	                for (int j = 0; j < nUdesc.userlist.size(); j++) {
	                    if (nUdesc.userlist.get(j).username.contentEquals(siCo.Username) && (nUdesc.userlist.get(j).role.equalsIgnoreCase("admin") || nUdesc.userlist.get(j).role.equalsIgnoreCase("creator") || nUdesc.userlist.get(j).role.equalsIgnoreCase("clientadmin"))) {
	                        pk = nUdesc.userlist.get(j).publicKey;
	                        break;
	                    }
	                }
	                if (pk != null) {
	                    break;
	                }
	            }
	            if (pk == null) {
	                break;
	            }
	            EncryptionUtility.verifySignature(ObjectConverter.ObjectToByte(siCo.object), DatatypeConverter.parseBase64Binary(siCo.signature), DatatypeConverter.parseBase64Binary(pk));
	            Network netw = (Network) ObjectConverter.StringToObject(siCo.object);
	            NetworkList nList = new NetworkList();
	            String netwo = nList.selectAndRemoveNetworkByName(getApplicationContext(), netw.NetworkName);
	            break;
	        //Request delete network
	        case "RDUS":
	            SignatureContainer siCon = (SignatureContainer) ObjectConverter.byteToObject(data.payload);
	            //Verifikasi signature
	            //Cek signature Network User Description
	            String pki = null;
	            ArrayList<SignatureContainer> sckon = NetworkUserTable.getNetworkUserTable(getApplicationContext(), siCon.Type, null);
	            for (int i = 0; i < sckon.size(); i++) {
	                SignatureContainer sck = sckon.get(i);
	                NetworkUserDetail nUdesc = (NetworkUserDetail) ObjectConverter.StringToObject(sck.object);
	                for (int j = 0; j < nUdesc.userlist.size(); j++) {
	                    if (nUdesc.userlist.get(j).username.contentEquals(siCon.Username) && (nUdesc.userlist.get(j).role.equalsIgnoreCase("admin") || nUdesc.userlist.get(j).role.equalsIgnoreCase("creator") || nUdesc.userlist.get(j).role.equalsIgnoreCase("clientadmin"))) {
	                        pki = nUdesc.userlist.get(j).publicKey;
	                        break;
	                    }
	                }
	                if (pki != null) {
	                    break;
	                }
	            }
	            if (pki == null) {
	                break;
	            }
	            EncryptionUtility.verifySignature(ObjectConverter.ObjectToByte(siCon.object), DatatypeConverter.parseBase64Binary(siCon.signature), DatatypeConverter.parseBase64Binary(pki));
	            DeletedUser delu = (DeletedUser) ObjectConverter.StringToObject(siCon.object);
                DeletedUserList.addDeletedUser(getApplicationContext(),delu.networkName, delu.username);
	            break;
	        case "RERL":
	        	SignatureContainer signaco = (SignatureContainer) ObjectConverter.byteToObject(data.payload);
                //encrutil.verifySignature(ObjectConverter.ObjectToByte(ObjectConverter.StringToObject(signaco.object)), ObjectConverter.ObjectToByte(ObjectConverter.StringToObject(signaco.signature)) , "".getBytes());

                //get username for current network name
                ArrayList<ActiveLogin> activeLogins = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
                if (activeLogins == null) {
                    activeLogins = new ArrayList();
                }
                ActiveLogin actlog = null;
                for (int i = 0; i < activeLogins.size(); i++) {
                    ActiveLogin temp = activeLogins.get(i);
                    if (temp.ActiveNetwork.equals(signaco.Type)) {
                        actlog = activeLogins.get(i);
                    }
                }
                if (actlog == null) {
                    break; // no curent user
                }
                signaco.Username = actlog.username;
                // add arraylist of logfile into signature container
                ArrayList<FileLog> fileLogs = new FileLogManager(getApplicationContext()).getFileLogList(signaco.Type, signaco.Username);
                if (fileLogs == null) {
                    break;// no file log, there's no need for tcp connection
                }
                signaco.object = ObjectConverter.ObjectToString(fileLogs);
                byte[] bs = EncryptionUtility.generateSignature(signaco.object.getBytes(), actlog.privKey);
                signaco.signature = DatatypeConverter.printBase64Binary(bs);

                message.sPayload = DatatypeConverter.printBase64Binary(ObjectConverter.ObjectToByte(signaco));
                message.command = "RERL";
                message.protocolVersion = 1;
                HTTPClient.requestPost(data.senderIP, 8888, message);
	        	break;
                case "REDF":
                    SignatureContainer sigco = (SignatureContainer) ObjectConverter.byteToObject(data.payload);

                    //Get Older user Network table , put the new user , Dont forget to check username already exist
                    ArrayList<SignatureContainer> scax = NetworkUserTable.getNetworkUserTable(getApplicationContext(), sigco.Type, null);
                    boolean foundThat = false;
                    User uux = null;
                    for (int i = 0; i < scax.size(); i++) {
                        NetworkUserDetail nUd = (NetworkUserDetail) ObjectConverter.StringToObject(scax.get(i).object);
                        ArrayList<User> ual = nUd.userlist;
                        for (int j = 0; j < ual.size(); j++) {
                            uux = ual.get(j);
                            if (uux.username.equals(sigco.Username)) {
                                foundThat = true;
                                break;
                            }
                        }
                        if (foundThat) {
                            break;
                        }
                    }
                    if (uux == null) {
                        break;
                    }

                    if (!foundThat) {
                        break;
                    }

                    ArrayList<ActiveLogin> aalo = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
                    if (aalo == null) {
                        aalo = new ArrayList();
                    }
                    ActiveLogin ulo = null;
                    for (int i = 0; i < aalo.size(); i++) {
                        ActiveLogin temp = aalo.get(i);
                        if (temp.ActiveNetwork.equals(sigco.Type)) {
                            ulo = aalo.get(i);
                        }
                    }
                    if (ulo == null) {
                        break;
                    }

                    final String nn = sigco.object;
                    if (EncryptionUtility.verifySignature(nn.getBytes(), DatatypeConverter.parseBase64Binary(sigco.signature), DatatypeConverter.parseBase64Binary(uux.publicKey))) {

                        final File r = new File(ulo.homeDirectory);
                        Thread thr = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("FileSharingService", "REDF Search from "+r.getAbsolutePath()+" to delete "+nn);
                                for(File f:r.listFiles()){
                                    if(f.isFile()){
                                        Log.d("FileSharingService", "File:"+f.getName());
                                        if (f.getName().contentEquals(nn)){
                                            f.delete();
                                            Log.d("FileSharingService", f.getName()+" deleted");
                                        }
                                    }
                                }
                                Log.d("FileSharingService", "REDF Search done");
                            }
                        });
                        thr.start();
                    }
                    break;
			}
		}
	}

	

	public class HTTPServer extends NanoHTTPD {
		public HTTPServer() {
			super(8888);
			Log.e("HTTP SERVER", "Started");
		}
		
		@Override
		public Response serve(IHTTPSession session) {
			Log.d("HttpServer", "session serve");
			Map<String, String> headers = session.getHeaders();
			NanoHTTPD.Response response = new NanoHTTPD.Response("failed");
			Method method = session.getMethod();
			Map<String, String> parms = session.getParms();
			if (Method.PUT.equals(method) || Method.POST.equals(method)) {
				try {
					session.parseBody(parms);
				} catch (IOException ioe) {
					return new Response(Response.Status.INTERNAL_ERROR,
							MIME_PLAINTEXT,
							"SERVER INTERNAL ERROR: IOException: "
									+ ioe.getMessage());
				} catch (ResponseException re) {
					return new Response(re.getStatus(), MIME_PLAINTEXT,
							re.getMessage());
				}
			}
			SerializableProtocolStructure p = new SerializableProtocolStructure();
			try {
				p.command = ((String) parms.get("Command"));
				p.protocolVersion = Integer.parseInt((String) parms
						.get("ProtocolVersion"));
				p.sPayload = ((String) parms.get("payload"));
			} catch (Exception e) {
				Log.e("HTTP Server", "Request Format Invalid");
				return response;
			}
			Gson gson = new Gson();
			Log.e("Serving HTTP", "GET COMMAND:" + p.command + " from IP:"
					+ headers.get("remote-addr"));
			switch (p.command) {
			case "RENN":
				ArrayList<LinkedTreeMap> nt = (ArrayList) gson.fromJson(
						p.sPayload, ArrayList.class);
				NetworkList n = new NetworkList();

				ArrayList<String> deletedNetworkList = new DeletedNetworkList()
						.getDeletedNetworkTable(getApplicationContext());
				if (deletedNetworkList == null) {
					deletedNetworkList = new ArrayList<String>();
				}
				ArrayList<Network> networkList = n
						.getNetworkTable(getApplicationContext());
				for (int i = 0; i < nt.size(); i++) {
					Network wrk = new Network();
					wrk.Signature = ((String) ((LinkedTreeMap) nt.get(i))
							.get("Signature"));
					wrk.NetworkName = ((String) ((LinkedTreeMap) nt.get(i))
							.get("NetworkName"));

					boolean exist = false;
					for (Network ntw : networkList) {
						if (ntw.NetworkName.contentEquals(wrk.NetworkName)) {
							exist = true;
						}
					}
					if (!exist) {
						networkList.add(wrk);
					}
				}
				for (Iterator<String> i = deletedNetworkList.iterator(); i
						.hasNext();) {
					String nw = (String) i.next();
					for (Iterator<Network> j = networkList.iterator(); j
							.hasNext();) {
						Network ntw = (Network) j.next();
						if (ntw.NetworkName.equals(nw)) {
							j.remove();
						}
					}
				}
				n.writeNetworkTable(getApplicationContext(), networkList);

				ArrayList<String> availableNetwork = new ArrayList<String>();
				for (int i = 0; i < networkList.size(); i++) {
					availableNetwork
							.add(((Network) networkList.get(i)).NetworkName);
				}
				if (availableNetwork.size() > 0)
					StateManager.setItem("AvailableNetwork", availableNetwork);
				sendMessageToUI("com.ay.filesharing.MainActivity",
						"updateNetworkList");
				response = new Response("success");
				break;
			case "RENA":
				Log.d("p.sPayload", p.sPayload);
				String strsPayload = p.sPayload.replace(" ", "+");
				Log.d("p.sPayload2", p.sPayload);
				SignatureContainer scon = (SignatureContainer) ObjectConverter
						.StringToObject(strsPayload);

				if (scon != null) {
					Log.d("scon.Type", scon.Type);
					Log.d("scon.Username", scon.Username);
				} else
					Log.d("scon", "null");

				// Verifikasi signature
				// Cek signature Network User Description
				String pki = null;

				ArrayList<SignatureContainer> sckna = NetworkUserTable
						.getNetworkUserTable(getApplicationContext(),
								scon.Type, null);
				Log.d("sckna.size()", "" + sckna.size());
				for (int i = 0; i < sckna.size(); i++) {
					SignatureContainer sckn = sckna.get(i);
					NetworkUserDetail nUdescd = (NetworkUserDetail) ObjectConverter
							.StringToObject(sckn.object);
					// Get Username
					for (int j = 0; j < nUdescd.userlist.size(); j++) {
						User user = nUdescd.userlist.get(j);
						if (user.username.contentEquals(scon.Username)) {
							pki = user.publicKey;
						}
					}
				}
				if (pki == null) {
					Log.e("pki", "pki null");
					return response;
				}

				boolean result = EncryptionUtility.verifySignature(
						ObjectConverter.ObjectToByte(scon.object),
						DatatypeConverter.parseBase64Binary(scon.signature),
						DatatypeConverter.parseBase64Binary(pki));
				if (result) {
					String ipadd = headers.get("remote-addr");
					Log.d("remote-addr",
							headers.containsKey("remote-addr") ? headers
									.get("remote-addr") : "NULL!");

					StateManager.setItem("adminip", ipadd);
					Log.d("AdminIP", ipadd);
				}
				response = new Response("success");
				break;
			case "REAU":
				String sa = p.sPayload.replace(" ", "+");
				SignatureContainer sco = (SignatureContainer) ObjectConverter
						.StringToObject(sa);

				boolean loggedin = false;
				ArrayList<ActiveLogin> aal = (ArrayList<ActiveLogin>) StateManager
						.getItem("ActiveLogin");
				if (aal == null) {
					aal = new ArrayList<ActiveLogin>();
				}

				for (ActiveLogin al : aal) {
					if (al.ActiveNetwork.equals(sco.Type))
						loggedin = true;
				}

				if (!loggedin) {
					break;
				}

				// Verifikasi signature
				// Cek signature Network User Description
				String pk = null;

				ArrayList<SignatureContainer> networkUsersTable = NetworkUserTable
						.getNetworkUserTable(getApplicationContext(), sco.Type,
								null);

				for (SignatureContainer signatureContainer : networkUsersTable) {
					NetworkUserDetail networkUserdetail = (NetworkUserDetail) ObjectConverter
							.StringToObject(signatureContainer.object);
					for (file.User user : networkUserdetail.userlist) {
						if (user.username.contentEquals(sco.Username)) {
							pk = user.publicKey;
							break;
						}
					}
					if (pk != null)
						break;
				}

				if (pk == null) {
					return response;
				}
				boolean resu = EncryptionUtility.verifySignature(
						ObjectConverter.ObjectToByte(sco.object.replace("\n",
								"")), DatatypeConverter
								.parseBase64Binary(sco.signature.replace("\n",
										"")), DatatypeConverter
								.parseBase64Binary(pk.replace("\n", "")));
				if (resu) {
					String ip = headers.get("remote-addr");

					ArrayList<ActiveUser> activeUserArray = (ArrayList<ActiveUser>) StateManager
							.getItem("activeuser");
					if (activeUserArray == null) {
						activeUserArray = new ArrayList<ActiveUser>();
					}

					ActiveUser a = new ActiveUser();
					a.username = sco.Username;
					a.ip = ip;
					a.network = sco.Type;

					int idx = -1;
					for (int i = 0; i < activeUserArray.size(); i++)
						if (activeUserArray.get(i).username.equals(a.username))
							idx = i;
					if (idx == -1) {
						activeUserArray.add(a);
						Log.d("activeuser", "Insert new, count:"
								+ activeUserArray.size());
					} else
						activeUserArray.set(idx, a);

					StateManager.setItem("activeuser", activeUserArray);
					response = new Response("success");
				}
				break;
			case "RNUT":
				String s = p.sPayload.replace(" ", "+");
				SignatureContainer sc = (SignatureContainer) ObjectConverter
						.StringToObject(s);
				NetworkUserDetail nUd = (NetworkUserDetail) ObjectConverter
						.StringToObject(sc.object);
				ArrayList<User> d = nUd.userlist;

				ArrayList<SignatureContainer> arraySC = NetworkUserTable
						.getNetworkUserTable(getApplicationContext(),
								nUd.networkName, null);
				arraySC.add(sc);

				// Cek signature Network User Description
				String publickey = null;

				// Get Username
				for (int k = 0; k < arraySC.size(); k++) {
					NetworkUserDetail nU = (NetworkUserDetail) ObjectConverter
							.StringToObject(arraySC.get(k).object);
					ArrayList<User> e = nU.userlist;

					for (int j = 0; j < e.size(); j++) {
						if (e.get(j).username.contentEquals(sc.Username)) {

							publickey = e.get(j).publicKey;
							break;
						}
					}
				}
				if (publickey == null) {
					return response;
				}

				boolean res = EncryptionUtility.verifySignature(
						DatatypeConverter.parseBase64Binary(sc.object),
						DatatypeConverter.parseBase64Binary(sc.signature),
						DatatypeConverter.parseBase64Binary(publickey));

				if (res) {
					Log.d("RNUT", "replaceNetworkUserTable");
					NetworkUserTable.replaceNetworkUserTable(
							getApplicationContext(), sc);
					response = new Response("success");
				}
				break;
			case "RECA":
				String replacedPayload = p.sPayload.replace(" ", "+");
				SignatureContainer sigContainer = (SignatureContainer) ObjectConverter
						.StringToObject(replacedPayload);

				User user = null;
				ArrayList<User> ual = NetworkUserTable.getUserList(
						getApplicationContext(), (String) sigContainer.Type,
						null);
				for (User u : ual) {
					if (u.username.equals(sigContainer.Username)) {
						user = u;
					}
				}
				if (user == null) {
					break;
				}

				ArrayList<ActiveUser> activeusers = (ArrayList<ActiveUser>) StateManager
						.getItem("activeuser");
				if (activeusers == null) {
					activeusers = new ArrayList<ActiveUser>();
				}
				ActiveUser fromChat = null;
				for (ActiveUser temp : activeusers) {
					if (temp.username.equals(sigContainer.Username)) {
						fromChat = temp;
					}
				}

				if (fromChat == null) {
					break;
				}
				ActiveLogin al = null;
				ArrayList<ActiveLogin> activeLogins = (ArrayList<ActiveLogin>) StateManager
						.getItem("ActiveLogin");
				for (ActiveLogin a : activeLogins) {
					if (fromChat.network.equals(a.ActiveNetwork))
						al = a;
				}

				if (EncryptionUtility.verifySignature(sigContainer.object
						.getBytes(), DatatypeConverter
						.parseBase64Binary(sigContainer.signature),
						DatatypeConverter.parseBase64Binary(user.publicKey))) {
					String ip = headers.get("remote-addr");
					ArrayList<file.ChatMessage> chatMessage = (ArrayList<ChatMessage>) StateManager
							.getItem("ChatMessage");
					if (chatMessage == null)
						chatMessage = new ArrayList<ChatMessage>();
					chatMessage.add(new ChatMessage(fromChat.username,
							al == null ? null : al.username,
							sigContainer.object));
					StateManager.setItem("ChatMessage", chatMessage);
					sendMessageToUI("com.ay.filesharing.ChatActivity." + ip,
							"updateChatList");
					
					ActiveUser au = null;
					ArrayList<ActiveUser> ausers = (ArrayList<ActiveUser>) StateManager.getItem("activeuser");
					if(ausers!=null)
						for(ActiveUser x: ausers){
							if(x.ip.equals(ip)&&x.username.equals(user.username))
								au=x;
						}
					if(au!=null)
					{
						NotificationManager mNotifyManager = (NotificationManager) getApplicationContext()
								.getSystemService(Context.NOTIFICATION_SERVICE);
						Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
						mBuilder.setContentTitle(fromChat.username).setContentText(sigContainer.object)
								.setSmallIcon(R.drawable.ic_launcher);
						Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
						long[] pattern = {500,500};
						mBuilder.setVibrate(pattern);
						mBuilder.setSound(alarmSound);
						
						Intent intent = new Intent(getApplicationContext(), ClientActivity.class);
						intent.putExtra(ClientActivity.TAG_CLIENT, au);
						intent.putExtra(ClientActivity.TAG_ACTIVE_LOGIN, al);
                        intent.putExtra(ClientActivity.TAG_START, 1);
						
						//intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0,
								intent, PendingIntent.FLAG_UPDATE_CURRENT);
						mBuilder.setContentIntent(pending);
						mBuilder.setAutoCancel(true);
						mNotifyManager.notify(8, mBuilder.build());
					}
				}
				response = new Response("success");
				break;
			case "RESK":
				String str = p.sPayload.replace(" ", "+");
				SignatureContainer sContainer = (SignatureContainer) ObjectConverter
						.StringToObject(str);

				User us = null;
				ArrayList<User> uaa = NetworkUserTable.getUserList(
						getApplicationContext(), (String) sContainer.Type, null);
				for (User u : uaa) {
					if (u.username.equals(sContainer.Username)) {
						us = u;
					}
				}
				ArrayList<ActiveLogin> aalo = (ArrayList<ActiveLogin>) StateManager
						.getItem("ActiveLogin");
				if (aalo == null) {
					aalo = new ArrayList();
				}
				ActiveLogin ulo = null;
				for (ActiveLogin temp : aalo) {
					if (temp.ActiveNetwork.equals(sContainer.Type)) {
						ulo = temp;
					}
				}
				if (ulo == null) {
					break;
				}
				if (ulo.homeDirectory == null
						|| ulo.homeDirectory.length() == 0) {
					break;
				}
				// Cek Signature
				if (EncryptionUtility.verifySignature(
						sContainer.object.getBytes(),
						DatatypeConverter.parseBase64Binary(sContainer.signature),
						DatatypeConverter.parseBase64Binary(us.publicKey))) {
					FtpServer ftps = FtpServer.getInstance(getApplicationContext());
					FTPUser ftpuser = ftps.generatePassword(ulo.homeDirectory);
					
					String tbs = DatatypeConverter
							.printBase64Binary(EncryptionUtility.encryptAsymmetric(
									ObjectConverter.ObjectToByte(ftpuser),
									DatatypeConverter
											.parseBase64Binary(us.publicKey)));
					
					Log.d("RESK response", String.format("{%s}", tbs));
                    //response=new Response("G1VKlLEiqdPEt0S1x7mRQYHFac/W+uSZVXHBncye7r+rC1J0pv8caUN1w8tZVy7ZvctLe5kOFHe8pb+ZxmWm9YU1PGQICWEjNkUGf74cXb3VXjCLK+vFNKygLHuaXh7F8iAdu8JlZCTNN3XH3sp4jUO61zmzGs9ImkY5hcabfqHKSF8lwTqzQ5s6xFso9LkfClHtpxoutFpzcFkGVI0tYtA7bZ9LoeLMuBfzuuwOBRUz2umCZOY5PPawh+j/LcfxC5pza+ZzXSIDjVZ8TgiTUKDEQ5wSCjtUvDzyPJP8j1ivS6Xv8oUbp4abi9Vff/SnkHkmVhRnLZPgwWLsYx6h1w==");
					response = new Response(tbs);
				}
				break;
                case "RERE":
                    String sap = p.sPayload.replace(" ", "+");
                    SignatureContainer scont = (SignatureContainer) ObjectConverter.StringToObject(sap);
                    User u = (User) ObjectConverter.StringToObject(scont.object);

                    ArrayList<ActiveLogin> aalu = (ArrayList<ActiveLogin>) StateManager.getItem("ActiveLogin");
                    if (aalu == null) {
                        aalu = new ArrayList();
                    }
                    ActiveLogin ul = null;
                    for (int i = 0; i < aalu.size(); i++) {
                        ActiveLogin temp = aalu.get(i);
                        if (temp.ActiveNetwork.equals(scont.Type)) {
                            ul = aalu.get(i);
                        }
                    }
                    if (ul == null) {
                        break;
                    }

                    if (ul.role.contentEquals("client")) {
                        break;
                    }

                    //Cek apakan nama user sudah ada dalam table user
                    boolean exist = false;

                    ArrayList<SignatureContainer> scau = NetworkUserTable.getNetworkUserTable(getApplicationContext(), scont.Type, null);
                    for (int i = 0; i < scau.size(); i++) {
                        SignatureContainer sck = scau.get(i);
                        NetworkUserDetail nud = (NetworkUserDetail) ObjectConverter.StringToObject(sck.object);
                        ArrayList<User> uali = nud.userlist;
                        for (int j = 0; j < uali.size(); j++) {
                            if (uali.get(j).username.equals(scont.Username)) {
                                exist = true;
                                break;
                            }
                        }
                    }

                    //Cek apakan nama user sudah ada dalam table unapproved
                    SignatureContainer unutcont = UnapprovedNetworkUserTable.getUnapprovedNetworkUserTable(getApplicationContext(), scont.Type, ul.username);
                    ArrayList<User> uulist;
                    if (unutcont == null) {
                        uulist = new ArrayList();
                        uulist.add(u);
                        unutcont = UnapprovedNetworkUserTable.createUnapprovedNetworkUserTable(scont.Type, ul.username, 1, ul.privKey, uulist);
                    } else {
                        NetworkUserDetail nude = (NetworkUserDetail) ObjectConverter.StringToObject(unutcont.object);
                        uulist = nude.userlist;
                        for (int i = 0; i < uulist.size(); i++) {
                            if (uulist.get(i).username.contentEquals(scont.Username)) {
                                exist = true;
                                break;
                            }
                        }
                        uulist.add(u);
                        nude.userlist = uulist;
                        nude.VersionNumber += 1;
                        unutcont.object = ObjectConverter.ObjectToString(nude);
                    }
                    if (!exist) {
                        unutcont.Username = ul.username;
                        unutcont.signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(ObjectConverter.ObjectToByte(unutcont.object), ul.privKey));
                        UnapprovedNetworkUserTable.replaceUnapprovedNetworkUserTable(getApplicationContext(), unutcont);
                        response = new Response("success");
                    }

                    Log.d("FileSharingService", "New User ( " + scont.Username + " )is trying to register");
                    break;
			case "RECL":
                String reclPayload = p.sPayload.replace(" ", "+");
                SignatureContainer sconta = (SignatureContainer) ObjectConverter.StringToObject(reclPayload);

                boolean foundd = false;
                User uuu = null;
                ArrayList<User> ualu = NetworkUserTable.getUserList(getApplicationContext(), (String) sconta.Type, null);
                for (int j = 0; j < ualu.size(); j++) {
                    uuu = ualu.get(j);
                    if (uuu.username.equals(sconta.Username)) {
                        foundd = true;
                        break;
                    }
                }

                if (foundd) {
                    //Cek Signature
                    if (EncryptionUtility.verifySignature(sconta.object.getBytes(), DatatypeConverter.parseBase64Binary(sconta.signature), DatatypeConverter.parseBase64Binary(uuu.publicKey))) {
                        byte[] data = null;
                        try {
                            String filename = sconta.object + "_" + sconta.Type + ".uimg";
                            FileInputStream fis = getApplicationContext().openFileInput(filename);
                            Bitmap bitmap = BitmapFactory.decodeStream(fis);
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            data = stream.toByteArray();
                			//ObjectInputStream in = new ObjectInputStream(fis);
                			//data = (byte[]) in.readObject();
                			//in.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        if (data == null) {
                            break;
                        }
                        response = new Response(DatatypeConverter.printBase64Binary(data));
                    }
                }
                break;
			case "RELD":
                ArrayList<String> nts = (ArrayList<String>) gson.fromJson(p.sPayload, ArrayList.class);
                if (nts == null) {
                    break;
                }
                DeletedNetworkList delNetList = new DeletedNetworkList();
                for (int i = 0; i < nts.size(); i++) {
                    delNetList.addDeletedNetwork(getApplicationContext(),nts.get(i));
                }
                break;
			case "REDU":
                //Request List Deleted User
                ArrayList<LinkedTreeMap> ntsi = gson.fromJson(p.sPayload, ArrayList.class);
                if (ntsi == null) {
                    break;
                }
                for (int i = 0; i < ntsi.size(); i++) {
                    DeletedUserList.addDeletedUser(
                            getApplicationContext(),
                            (String) ntsi.get(i).get("networkName"),
                            (String) ntsi.get(i).get("username"));
                }
                break;
			case "RERL"://Request Replace Log File
                String strin = p.sPayload.replace(" ", "+");
                SignatureContainer signaco = (SignatureContainer) ObjectConverter.byteToObject(DatatypeConverter.parseBase64Binary(strin));
                ArrayList<FileLog> fileLogs = (ArrayList<FileLog>) ObjectConverter.StringToObject(signaco.object);
                String UsernameRERL = signaco.Username;
                String NetworkNameRERL = signaco.Type;
                if (fileLogs == null) {
                    break;
                }
                new FileLogManager(getApplicationContext()).writeFileLogs(NetworkNameRERL, fileLogs, UsernameRERL);
                break;
			}
			return response;
		}
	}
}
