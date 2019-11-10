import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.io.File;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.sql.*;

public class Server extends Thread implements IServer { //

    NodeStruct s_node;
    int lamport_clock;
    //data structures
    Map<String, NodeStruct> client_list;
    Map<String, NodeStruct> server_list;
    Map<Integer, Pair<String, Version>> key_v_store; // key-> (value, version)
    Map<String, List<DepNode>> dependency_list; //client id -> ( key-> version)
    Map<String, Map<Integer, List<DepNode>>> pending_list; //cli_id ->key-> Map<key, Map<key, Version>>


    public Server(String id, String ip, int port) {
        s_node = new NodeStruct(id, ip, port);
        client_list = new HashMap<String, NodeStruct>();
        server_list = new HashMap<String, NodeStruct>();
        key_v_store = new HashMap<Integer, Pair<String,Version>>();
        lamport_clock = 0;
    }

    public void run() {

    }

    public boolean registerReply(NodeStruct node) throws RemoteException
    {
        client_list.put(node.id, node);
        System.out.println("Registered client " + node.id);
        return true;
    }


    public String getKey(int key, NodeStruct c_node) throws RemoteException
    {
        String ret = key_v_store.get(key).getKey();
        DepNode dn = new DepNode(key, new Version(lamport_clock, s_node.id));
        if(dependency_list.containsKey(c_node.id))
            dependency_list.get(c_node.id).add(dn);
        else
        {
            dependency_list.put(c_node.id, new ArrayList<DepNode>());
            dependency_list.get(c_node.id).add(dn);
        } 
        return ret;
    }

    public boolean putKeyValue(int key, String val, NodeStruct c_node) throws RemoteException
    {
        Pair<String, Version> new_p = new Pair<String, Version>(val, new Version(lamport_clock, s_node.id));
        key_v_store.put(key, new_p);
        System.out.println("Added " + val + " and the size of keyvstore is: " + key_v_store.size());
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