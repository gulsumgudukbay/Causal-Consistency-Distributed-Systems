import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.io.File;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.sql.*;

public class Server extends Thread implements IServer { //

    NodeStruct s_node;

    //data structures
    Map<String, NodeStruct> client_list;
    Map<String, NodeStruct> server_list;


    public Server(String id, String ip, int port) {
        s_node = new NodeStruct(id, ip, port);
        client_list = new HashMap<String, NodeStruct>();
        server_list = new HashMap<String, NodeStruct>();
    }

    public void run() {

    }

    public boolean registerReply(NodeStruct node) throws RemoteException{
        
        client_list.put(node.id, node);
        System.out.println("Registered client " + node.id);
        return true;
    }

    public static void main(String args[]) {

        try {
            System.setProperty("java.security.policy", "security.policy");

            //Server obj = new Server("s1", "127.0.0.1", 2014);
            Server obj = new Server(args[0], args[1], Integer.parseInt(args[2]));

            IServer stub = (IServer) UnicastRemoteObject.exportObject(obj, obj.s_node.port);

            Registry registry = LocateRegistry.createRegistry(obj.s_node.port);
            registry.rebind(obj.s_node.id, stub);
            System.setProperty("java.rmi.server.hostname", obj.s_node.ip);

            System.err.println("Server ready");
            // testing

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }

}