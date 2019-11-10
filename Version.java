import java.io.Serializable;

@SuppressWarnings("serial")
public class Version implements Serializable {
	String id;
	int timestamp;
	public Version(int timestamp, String id){
		this.id = id;
		this.timestamp = timestamp;
	}
}
