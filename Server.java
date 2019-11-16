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
//import javafx.util.Pair;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.sql.*;
import java.util.Set;

public class Server extends Thread implements IServer { //

    NodeStruct s_node;
    int lamport_clock;
    //data structures
    Map<String, NodeStruct> client_list;
    Map<String, NodeStruct> server_list;
    Map<String, IServer> server_stubs; //server id -> server stub;
    Map<Integer, String> key_v_store; // key-> (value, version)
    Map<String, List<DepNode>> dependency_list; //client id -> ( key-> version)
    Map<String, Map<Integer, List<DepNode>>> pending_list; //cli_id ->key-> Map<key, Map<key, Version>>


    public Server(String id, String ip, int port) {
        s_node = new NodeStruct(id, ip, port);
        client_list = new HashMap<String, NodeStruct>();
        server_list = new HashMap<String, NodeStruct>();
        key_v_store = new HashMap<Integer, String>();
        server_stubs = new HashMap<String, IServer>();
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
        String ret = key_v_store.get(key); //.getKey();
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
        lamport_clock++;
        //replicated write

        dependency_list.get(c_node.id).clear();
        DepNode dn = new DepNode(key, new Version(lamport_clock, s_node.id));
        dependency_list.get(c_node.id).add(dn);

        //Pair<String, Version> new_p = new Pair<String, Version>(val, new Version(lamport_clock, s_node.id));
        //key_v_store.put(key, new_p);
        key_v_store.put(key, val);
        System.out.println("Added " + val + " and the size of keyvstore is: " + key_v_store.size());
        return true;
    }

    public static void init(Server obj, long ms) throws InterruptedException{
        obj.server_list.put("s1", new NodeStruct("s1", "127.0.0.1", 2014));
        obj.server_list.put("s2", new NodeStruct("s2", "127.0.0.1", 2015));
        obj.server_list.put("s3", new NodeStruct("s3", "127.0.0.1", 2016));


        //sleep(ms);
        sleep(ms);
        try{
            Set<Map.Entry<String, NodeStruct>> es = obj.server_list.entrySet(); 
            Iterator<Map.Entry<String, NodeStruct>> hmIterator = es.iterator(); 

            //create map of id -> server stub;
            //System.out.println(obj.server_list.size());
            while(hmIterator.hasNext()){

                Map.Entry<String, NodeStruct> mapElement = hmIterator.next(); 
                String server_id = mapElement.getKey();
                NodeStruct server_node = mapElement.getValue();
                //System.out.println(server_id + " " + server_node.ip + " " + server_node.port);
                //System.out.println(obj.s_node.id);
                if(server_id.compareTo(obj.s_node.id) != 0){
                    System.out.println(server_id + " " + server_node.ip + " " + server_node.port);
                    Registry dc_registry = LocateRegistry.getRegistry(server_node.ip, server_node.port);
                    IServer dc_stub = (IServer) dc_registry.lookup(server_id);
                    obj.server_stubs.put(server_id, dc_stub);
                }
                
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /*public static void ConnectServer(Server obj, string server_id){
        try{
            NodeStruct server_node = obj.server_list.getKey();
            System.out.println(server_id + " " + server_node.ip + " " + server_node.port);
            Registry dc_registry = LocateRegistry.getRegistry(server_node.ip, server_node.port);
            IServer dc_stub = (IServer) dc_registry.lookup(server_id);
            obj.server_stubs.put(server_id, dc_stub);
        }catch(Exception e){
            e.printStackTrace();
        }
    }*/

    public static void main(String args[]) {

        try {
            //Scanner myObj = new Scanner(System.in);
            System.setProperty("java.security.policy", "security.policy");
            /*
            0 - id;1 - localhost; 2- port
            3 - seconds of time to sleep before connecting to servers.
            */
            //Server obj = new Server("s1", "127.0.0.1", 2014);
            Server obj = new Server(args[0], args[1], Integer.parseInt(args[2]));

            
            //init(obj, 1);
            /*
            System.out.println("Enter first datacenter id:");
            String server_id = myObj.nextLine();  // Read user input
            ConnectServer(obj, server_id);
            System.out.println("Enter second datacenter id:");
            server_id = myObj.nextLine();  // Read user input
            ConnectServer(obj, server_id);*/
            
            IServer stub = (IServer) UnicastRemoteObject.exportObject(obj, obj.s_node.port);

            Registry registry = LocateRegistry.createRegistry(obj.s_node.port);
            registry.rebind(obj.s_node.id, stub);
            System.setProperty("java.rmi.server.hostname", obj.s_node.ip);

            init(obj, Integer.parseInt(args[3])*1000); //setup datacenter connections;
            System.out.println("num servers: " + obj.server_list.size());
            System.out.println("connected to servers: " + obj.server_stubs.size());
            System.out.println("connected to all datacenters");
            System.err.println("Server ready");
            // testing

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }

}