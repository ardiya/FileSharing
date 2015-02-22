package networking.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

import android.util.Log;
import networking.util.SerializableProtocolStructure;
import util.ObjectConverter;

public class UDPClient {
	private class Worker implements Runnable {
		private final SerializableProtocolStructure s;
		ArrayList<String> myIP = new ArrayList();

		private Worker(SerializableProtocolStructure s) {
			this.s = s;
		}

		public void run() {
			try {
				DatagramSocket c = new DatagramSocket();
				c.setBroadcast(true);

				byte[] sendData = ObjectConverter.ObjectToByte(this.s);
				Enumeration interfaces;
				if (this.myIP.size() < 1) {
					interfaces = NetworkInterface.getNetworkInterfaces();
					while (interfaces.hasMoreElements()) {
						NetworkInterface networkInterface = (NetworkInterface) interfaces
								.nextElement();
						for (InterfaceAddress interfaceAddress : networkInterface
								.getInterfaceAddresses()) {
							InetAddress broadcast = interfaceAddress
									.getBroadcast();
							if (broadcast != null) {
								String bip = broadcast.toString().replace("/",
										"");
								if (!this.myIP.contains(broadcast.toString())) {
									this.myIP.add(bip);
								}
							}
						}
					}
				}
				for (String bip : this.myIP) {
					try {
						DatagramPacket sendPacket = new DatagramPacket(
								sendData, sendData.length,
								InetAddress.getByName(bip), 8889);
						c.send(sendPacket);
					} catch (Exception e) {
					}
				}
				c.close();
			} catch (IOException ex) {
			}
		}
	}

	public void sendCommand(SerializableProtocolStructure s) {
		Log.d("SEND UDP", s.command);
		Runnable r = new Worker(s);
		new Thread(r).start();
	}
}