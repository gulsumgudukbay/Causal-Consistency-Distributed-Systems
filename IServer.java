import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

public interface IServer extends Remote {
    public boolean registerReply(NodeStruct node) throws RemoteException;
    public String getKey(int key, NodeStruct c_node) throws RemoteException;
    public boolean replicatedWrite(int key, String val, List<DepNode> attached_dep_list, Version new_version) throws RemoteException;
    public boolean putKeyValue(int key, String val, NodeStruct c_node) throws RemoteException;
}
