import java.io.Serializable;

@SuppressWarnings("serial")
public class NodeStruct implements Serializable {
	String id, ip;
	int port;
	public NodeStruct(String id, String ip, int port ){
		this.id = id;
		this.ip = ip;
		this.port = port;
	}
}
