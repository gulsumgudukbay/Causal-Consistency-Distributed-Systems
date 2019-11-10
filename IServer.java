import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

public interface IServer extends Remote {
    public boolean registerReply(NodeStruct node) throws RemoteException;
}
