import java.io.Serializable;
import java.util.List;
@SuppressWarnings("serial")
public class RW implements Serializable {
    int key;
    String val;
    List<DepNode> att_list;
    Version ver;
	public RW(int key, String val, List<DepNode> attached_dep_list, Version new_version){
		this.key = key;
        this.val = val;
        this.att_list = attached_dep_list;
        this.ver = new_version;
	}
}
