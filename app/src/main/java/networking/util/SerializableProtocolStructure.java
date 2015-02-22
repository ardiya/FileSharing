package networking.util;

import java.io.Serializable;

public class SerializableProtocolStructure
  implements Serializable
{
  public int protocolVersion;
  public String command;
  public byte[] payload;
  public String sPayload;
}
