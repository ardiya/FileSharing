package networking.httpd;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import networking.util.SerializableProtocolStructure;

public class HTTPClient {
	public static String requestPost(String ip, int port,
			SerializableProtocolStructure p) {
		Log.d("HTTPClient", "request "+p.command+" to "+ip);
		String url = "http://" + ip + ":" + String.valueOf(port);
		DataOutputStream wr = null;
		StringBuilder response = null;
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);

			con.setDoOutput(true);

			wr = new DataOutputStream(con.getOutputStream());

			String urlParameters = "Command=" + p.command + "&ProtocolVersion="
					+ p.protocolVersion + "&payload=" + p.sPayload;

			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
			int responseCode = con.getResponseCode();

			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));

			try {
				response = new StringBuilder();
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
			} catch (Throwable localThrowable1) {

			} finally {
				in.close();
			}
			return response.toString();
		} catch (IOException ex) {
			return "failed";
		} finally {
			try {
				if (wr != null) {
					wr.close();
				}
			} catch (IOException ex) {
				Logger.getLogger(HTTPClient.class.getName()).log(Level.SEVERE,
						null, ex);
				return "failed";
			}
		}
	}
}
