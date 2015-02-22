package protocol;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.Random;

import file.DeletedUser;
import networking.httpd.HTTPClient;
import networking.udp.UDPClient;
import networking.util.SerializableProtocolStructure;
import util.DatatypeConverter;
import util.ObjectConverter;
import util.StateManager;
import android.util.Log;
import cryptography.EncryptionUtility;
import file.ActiveLogin;
import file.ActiveUser;
import file.FTPUser;
import file.Network;
import file.SignatureContainer;
import file.User;

public class DistributedFileSharingProtocol implements EventListener {
	private static DistributedFileSharingProtocol instance = null;

	public static DistributedFileSharingProtocol getInstance() {

		if (instance == null) {
			try {
				instance = new DistributedFileSharingProtocol();
			} catch (Exception e) {
				instance = null;
			}
		}
		return instance;
	}

	public void testUDPHelloWorld() {
		UDPClient cl = new UDPClient();
		SerializableProtocolStructure s = new SerializableProtocolStructure();
		s.command = "TUDP";
		s.payload = "Halo UDP".getBytes();
		s.protocolVersion = 1;
		cl.sendCommand(s);
		Log.e("DistributedFileSharingProtocol", "Test UDP Sent");
	}

	/**
	 * Requesting NetworkName From All Client
	 * <p>
	 * RENN Get NetworkName , Used by all client
	 * <p>
	 * Type = Broadcast
	 * <p>
	 * input = networkname
	 * <p>
	 * 
	 */
	public void requestNetworkName() {
		UDPClient cl = new UDPClient();
		SerializableProtocolStructure s = new SerializableProtocolStructure();
		s.command = "RENN";
		s.payload = "".getBytes();
		s.protocolVersion = 1;
		cl.sendCommand(s);
	}

	/**
	 * Requesting NetworkName From All Client
	 * <p>
	 * RNUT Get NetworkName , Used by all client
	 * <p>
	 * Type = Broadcast
	 * <p>
	 * input = networkname
	 * <p>
	 * 
	 * @param networkName
	 *            input network name
	 */
	public void requestGetNetworkUserTable(String networkName) {

		UDPClient cl = new UDPClient();
		SerializableProtocolStructure s = new SerializableProtocolStructure();
		s.command = "RNUT";
		s.payload = networkName.getBytes();
		s.protocolVersion = 1;

		cl.sendCommand(s);
	}

	/**
	 * Request register to network
	 * <p>
	 * RERE Send to CLA (clientadmin) N * Type = 1 to 1 CLA
	 * <p>
	 * input = Networkname , SessionKey(Username , SHA1(password))
	 * <p>
	 * Used by all client
	 * 
	 * @param networkname
	 *            input network name
	 * @param username
	 *            input username
	 * @param password
	 *            input password
	 * @param adminip
	 *            input administrator's IP
	 */
	public void requestRegister(String networkname, String username,
			String password, String adminip) {
		try {
			SerializableProtocolStructure message = new SerializableProtocolStructure();

			// Put / Generate User Table
			KeyPair self = EncryptionUtility.generateAsymmetricKey();

			SignatureContainer s = new SignatureContainer();
			// Create the user
			User u = new User();
			u.username = username;
			u.role = "user";
			u.publicKey = DatatypeConverter.printBase64Binary(self.getPublic()
					.getEncoded());
			u.privateKeyHash = EncryptionUtility.hash(self.getPrivate()
					.getEncoded());
			u.privateKey = DatatypeConverter
					.printBase64Binary(EncryptionUtility.encryptSymetric(self
							.getPrivate().getEncoded(), EncryptionUtility
							.createKeyFromPassword(password, u.privateKeyHash)
							.getEncoded()));
			s.Username = username;
			s.object = ObjectConverter.ObjectToString(u);
			s.Type = networkname;
			message.sPayload = ObjectConverter.ObjectToString(s);
			message.command = "RERE";
			message.protocolVersion = 1;
			String result = HTTPClient.requestPost(adminip, 8888, message);

		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Request Network Administrator's IP
	 * 
	 * @param networkName
	 *            input network name
	 */
	public void requestNetworkAdminIP(String networkName) {
		UDPClient cl = new UDPClient();

		SignatureContainer c = new SignatureContainer();
		Random generator = new Random();

		c.Type = networkName;
		c.object = String.valueOf(100000 + generator.nextInt(900000));

		SerializableProtocolStructure s = new SerializableProtocolStructure();
		s.command = "RENA";
		s.payload = ObjectConverter.ObjectToByte(c);
		s.protocolVersion = 1;
		cl.sendCommand(s);
	}

	/**
	 * Request Active User
	 * <p>
	 * REAU Send to all client
	 * <p>
	 * Type = Broadcast
	 * <p>
	 * input = NetworkName, CL SIGN
	 * <p>
	 * Used by all client
	 *
	 * @param username
	 *            input username
	 * @param privateKey
	 *            private key
	 */
	public void requestActiveUser(String username, byte[] privateKey) {
		UDPClient cl = new UDPClient();

		SignatureContainer c = new SignatureContainer();

		ArrayList<ActiveLogin> aal = (ArrayList<ActiveLogin>) StateManager
				.getItem("ActiveLogin");

		if (aal == null)
			aal = new ArrayList<ActiveLogin>();
		for (Iterator<ActiveLogin> i = aal.iterator(); i.hasNext();) {
			ActiveLogin al = i.next();

			Random generator = new Random();
			c.Type = al.ActiveNetwork;
			c.Username = username;
			c.object = String.valueOf(100000 + generator.nextInt(900000));
			c.signature = DatatypeConverter
					.printBase64Binary(EncryptionUtility.generateSignature(
							al.ActiveNetwork.getBytes(), privateKey));

			SerializableProtocolStructure s = new SerializableProtocolStructure();
			s.command = "REAU";
			s.payload = ObjectConverter.ObjectToByte(c);
			s.protocolVersion = 1;
			cl.sendCommand(s);
		}
	}

	/**
	 * Request Delete Network
	 * <p>
	 * REDN Request All client to delete the network
	 * <p>
	 * Type = Broadcast
	 * <p>
	 * input = JSON(NetworkName ,Public Key)
	 * <p>
	 * Used by ADMIN
	 * 
	 * @param netw
	 *            input network name
	 * @param username
	 *            input username
	 * @param privateKey
	 *            private key
	 * @param netwName
	 */
	public void requestDeleteNetwork(Network netw, String username,
			byte[] privateKey, String netwName) {
		SignatureContainer sigco = new SignatureContainer();
		sigco.Type = netwName;
		sigco.Username = username;
		sigco.signature = DatatypeConverter.printBase64Binary(EncryptionUtility
				.generateSignature(ObjectConverter.ObjectToString(netw)
						.getBytes(), privateKey));
		sigco.object = ObjectConverter.ObjectToString(netw);

		UDPClient udpCl = new UDPClient();
		SerializableProtocolStructure sps = new SerializableProtocolStructure();
		sps.command = "REDN";
		sps.payload = ObjectConverter.ObjectToByte(sigco);
		sps.protocolVersion = 1;
		udpCl.sendCommand(sps);
	}

	/**
	 * Requesting All active user in network to refresh it's active user
	 * <p>
	 * RERU Get NetworkName , Used by all client
	 * <p>
	 * Type = Broadcast
	 * <p>
	 * input = networkname
	 * <p>
	 */
	public void requestRefreshUser(ActiveLogin u) {
		Random generator = new Random();

		SignatureContainer f = new SignatureContainer();
		f.Username = u.username;
		f.object = String.valueOf(100000 + generator.nextInt(900000));
		f.Type = u.ActiveNetwork;
		f.signature = DatatypeConverter.printBase64Binary(EncryptionUtility
				.generateSignature(f.object.getBytes(), u.privKey));

		UDPClient cl = new UDPClient();
		SerializableProtocolStructure s = new SerializableProtocolStructure();
		s.command = "RERU";
		s.payload = ObjectConverter.ObjectToByte(f);
		s.protocolVersion = 1;
		cl.sendCommand(s);
	}

	/**
	 * Request Chat
	 * <p>
	 * RECA Chat to other client
	 * <p>
	 * Type = 1 to 1
	 * <p>
	 * input = NetworkName ,SKRE(Chat Message) , CL Sign
	 * <p>
	 * Used by all
	 * 
	 * @param u
	 *            ourself
	 * @param m
	 *            input string message
	 * @param ip
	 *            input IP
	 * @return boolean status
	 */
	public boolean requestChat(ActiveLogin u, String m, String ip) {
		SerializableProtocolStructure message = new SerializableProtocolStructure();

		SignatureContainer s = new SignatureContainer();
		s.Username = u.username;
		s.object = m;
		s.Type = u.ActiveNetwork;

		s.signature = DatatypeConverter.printBase64Binary(EncryptionUtility
				.generateSignature(m.getBytes(), u.privKey));

		message.sPayload = ObjectConverter.ObjectToString(s);
		message.command = "RECA";
		message.protocolVersion = 1;
		String res;
        int i = 0;
        do {
            res = HTTPClient.requestPost(ip, 8888, message);
            try {
                Thread.sleep(i*100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }while(res.contentEquals("failed") && i++ < 3);
		if (res.contentEquals("failed")) {
			Log.d("Chat Failed", "reply from "+ip+": "+res==null?"null":res);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Request another user session key
	 * <p>
	 * RESK Send a file to other client
	 * <p>
	 * Type = 1 to 1
	 * <p>
	 * input = n
	 * <p>
	 * Used by all client
	 * 
	 * @param aU
	 *            available user
	 * @param u
	 *            ourself
	 * @return ftpu
	 */
	public FTPUser requestSessionKey(ActiveUser aU, ActiveLogin u) {
            SerializableProtocolStructure message = new SerializableProtocolStructure();
		SignatureContainer s = new SignatureContainer();
		s.Username = u.username;
		s.object = aU.username;
		s.Type = u.ActiveNetwork;
		s.signature = DatatypeConverter.printBase64Binary(EncryptionUtility
				.generateSignature(s.object.getBytes(), u.privKey));
		message.sPayload = ObjectConverter.ObjectToString(s);
		message.command = "RESK";
		message.protocolVersion = 1;

        FTPUser ftpu;
        int i = 0;

        do {
            String result = HTTPClient.requestPost(aU.ip, 8888, message);
            Log.d("RESK HTTP Result from " + aU.ip, result);
            if (result.startsWith("failed")) {
                return null;
            }
            result = result.replace(" ", "+");
            byte[] dt = DatatypeConverter.parseBase64Binary(result);
            byte[] res = EncryptionUtility.decryptAsymetric(dt, u.privKey);
            Log.d("res", DatatypeConverter.printBase64Binary(res));

            ftpu = (FTPUser) ObjectConverter.byteToObject(res);
            try {
                Thread.sleep(i*100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }while(ftpu==null && i++ < 3);
		return ftpu;
	}

	/**
	 * Request Client Info
	 * <p>
	 * RECL Get Client Info
	 * <p>
	 * Type = 1 to 1
	 * <p>
	 * input = NetworkName, CL Sign
	 * <p>
	 * Used by all client
	 * 
	 * @param aU
	 *            available user
	 * @param u
	 *            current active login
	 */
	public byte[] requestClientInfo(ActiveUser aU, ActiveLogin u) {
		SerializableProtocolStructure message = new SerializableProtocolStructure();
		SignatureContainer s = new SignatureContainer();

		s.Username = u.username;
		s.object = aU.username;
		s.Type = u.ActiveNetwork;
		s.signature = DatatypeConverter.printBase64Binary(EncryptionUtility
				.generateSignature(s.object.getBytes(), u.privKey));
		message.sPayload = ObjectConverter.ObjectToString(s);
		message.command = "RECL";
		message.protocolVersion = 1;
		String result = HTTPClient.requestPost(aU.ip, 8888, message);
		if (result.equals("failed") || result.equals("")) {
			return null;
		}
		try {
			return DatatypeConverter.parseBase64Binary(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
     * Request Replace Log File<p>
     * RERL Replace Log File
     * <p>
     * Type = Broadcast
     * <p>
     * input = networkname
     * <p>
     *
     * @param networkName input network name
     * @param username input username
     * @param privateKey private key
     */
    public void requestReplaceLogFile(String networkName, String username, byte[] privateKey) {
        UDPClient cl = new UDPClient();
        SerializableProtocolStructure s = new SerializableProtocolStructure();

        SignatureContainer sico = new SignatureContainer();
        sico.Type = networkName;
        sico.Username = username;
        sico.object = networkName;
        sico.signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(networkName.getBytes(), privateKey));

        s.command = "RERL";
        s.payload = ObjectConverter.ObjectToByte(sico);
        s.protocolVersion = 1;
        cl.sendCommand(s);
    }
    /**
     * Request Replace Network Table<p>
     * RERN Get NetworkName , Used by all client
     * <p>
     * Type = Broadcast
     * <p>
     * input = networkname
     * <p>
     *
     * @param networkName input network name
     * @param username input username
     * @param privateKey private key
     */
    public void requestReplaceNetworkUserTable(String networkName, String username, byte[] privateKey) {
        UDPClient cl = new UDPClient();
        SerializableProtocolStructure s = new SerializableProtocolStructure();

        SignatureContainer c = new SignatureContainer();
        c.Type = "";
        c.Username = username;
        c.object = networkName;
        c.signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(networkName.getBytes(), privateKey));

        s.command = "RERN";
        s.payload = ObjectConverter.ObjectToByte(c);
        s.protocolVersion = 1;
        cl.sendCommand(s);
    }
    /**
     * Requesting Delete Network From Client<p>
     * REDU Get ListDeletedNetwork ,Used by all client
     * <p>
     * Type = Broadcast
     *
     */
    public void requestDeletedUserList() {
        UDPClient cl = new UDPClient();
        SerializableProtocolStructure s = new SerializableProtocolStructure();
        s.command = "REDU";
        s.payload = "".getBytes();
        s.protocolVersion = 1;
        cl.sendCommand(s);
    }
    /**
     * Request Delete User
     * <p>
     * RDUS Request All client to delete the User
     * <p>
     * Type = Broadcast
     * <p>
     * input = JSON(NetworkName ,Public Key)
     * <p>
     * Used by ADMIN
     *
     * @param username input username
     * @param privateKey private key
     * @param delUser delete user
     * @param networkName current username
     */
    public void requestDeleteUser(DeletedUser delUser, String username, byte[] privateKey, String networkName) {
        SignatureContainer sigco = new SignatureContainer();
        sigco.Type = networkName;
        sigco.Username = username;
        sigco.signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(ObjectConverter.ObjectToString(delUser).getBytes(), privateKey));
        sigco.object = ObjectConverter.ObjectToString(delUser);

        UDPClient udpCl = new UDPClient();
        SerializableProtocolStructure sps = new SerializableProtocolStructure();
        sps.command = "RDUS";
        sps.payload = ObjectConverter.ObjectToByte(sigco);
        sps.protocolVersion = 1;
        udpCl.sendCommand(sps);
    }
    /**
     * Requesting Delete File<p>
     * REDF Get NetworkName , Used by all client
     * <p>
     * Type = Broadcast
     * <p>
     * input = networkname
     * <p>
     *
     * @param fileName input file name
     * @param networkName input network name
     * @param u ourself
     */
    public void requestDeleteFile(String fileName, String networkName, ActiveLogin u) {
        SignatureContainer f = new SignatureContainer();

        f.Username = u.username;
        f.object = fileName;
        f.Type = networkName;
        f.signature = DatatypeConverter.printBase64Binary(EncryptionUtility.generateSignature(f.object.getBytes(), u.privKey));

        UDPClient cl = new UDPClient();
        SerializableProtocolStructure s = new SerializableProtocolStructure();
        s.command = "REDF";
        s.payload = ObjectConverter.ObjectToByte(f);
        s.protocolVersion = 1;
        cl.sendCommand(s);
    }
}
