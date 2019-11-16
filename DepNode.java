import java.io.Serializable;

@SuppressWarnings("serial")
public class DepNode implements Serializable {
	Version v;
	int key;
	public DepNode(int key, Version v ){
		this.key = key;
		this.v = v;
	}

	public String toString()
	{
		return "<" + key + ", " + v.toString() + ">";
	}
}
