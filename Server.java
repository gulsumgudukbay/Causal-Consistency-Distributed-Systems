import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.io.File;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.time.LocalDateTime;
import java.time.temporal.*;
public class Server extends Thread implements IServer { //

    NodeStruct s_node;
    int lamport_clock;
    //data structures
    Map<String, NodeStruct> client_list;
    Map<String, NodeStruct> server_list;
    Map<String, IServer> server_stubs; //server id -> server stub;
    Map<Integer, String> key_v_store; // key-> (value, version)
    Map<String, List<DepNode>> dependency_list; //client id -> ( key-> version)
    //Map<String, Map<Integer, List<DepNode>>> pending_list; //cli_id ->key-> Map<key, Map<key, Version>>
    Map<Integer, List<Version>> pending_list; //key -> list of versions 
    Map<Integer, List<Version>> recved_versions; //ket -> list of versions;
    Map<Integer, String> pending_vals; //ket -> vaue
    Queue<RW> pending_rws; //list of replicated writes pending; // try all
    public Server(String id, String ip, int port) {
        s_node = new NodeStruct(id, ip, port);
        client_list = new HashMap<String, NodeStruct>();
        server_list = new HashMap<String, NodeStruct>();
        key_v_store = new HashMap<Integer, String>();
        server_stubs = new HashMap<String, IServer>();
        dependency_list = new HashMap<String, List<DepNode>>();
        //pending_list = new HashMap<String, HashMap<String, List<DepNode>> >();
        pending_list = new HashMap<Integer, List<Version>>();
        recved_versions = new HashMap<Integer, List<Version>>();
        //pending_vals = new HashMap<Integer, List<Version>>();
        pending_rws = new LinkedList<RW>();
        lamport_clock = 0;
    }

    public boolean registerReply(NodeStruct node) throws RemoteException
    {
        client_list.put(node.id, node);
        System.out.println("Registered client " + node.id);
        //add empty list into dependency
        dependency_list.put(node.id, new ArrayList<DepNode>());
        return true;
    }


    public String getKey(int key, NodeStruct c_node) throws RemoteException
    {
        System.out.println("Received client " + c_node.id + "'s read request for key " + key);
        String  ret = "";//.getKey();
        if(key_v_store.containsKey(key)){
            ret = key_v_store.get(key);
        }else{
            return "";
        }
        List<Version> list = recved_versions.get(key);
        Version ver = list.get(list.size()-1);
        DepNode dn = new DepNode(key, ver);
        if(dependency_list.containsKey(c_node.id))
            dependency_list.get(c_node.id).add(dn);
        else
        {
            dependency_list.put(c_node.id, new ArrayList<DepNode>());
            dependency_list.get(c_node.id).add(dn);
        } 
        return ret;
    }

    public static boolean compare(Version a, Version b){
        if(a.id == b.id && a.timestamp == b.timestamp)
            return true;
        return false;
    }
    
    public boolean replicatedWrite(int key, String val, List<DepNode> attached_dep_list, Version new_version) throws RemoteException
    {
        //if attached list is empty
        /*if(attached_dep_list.isEmpty()){
            //commit write
            key_v_store.put(key, val);
            //return true;
        }*/
        System.out.println("At datacenter " + s_node.id + " received replicated write for key: " + key + " and value:  " + val + " and new version: " + new_version);
        System.out.println(dependency_list);

        //traverse list of depnodes
        boolean can_commit = true;
        //print dependency list

        //check if new version is in pending list and remove
        if( pending_list.containsKey(key) ){
            int i = 0;
            for(Version ver : pending_list.get(key)){
                if(compare(ver, new_version)){
                    pending_list.remove(i);
                    System.out.println("At datacenter " + s_node.id + " removed version " + ver + " from pending list.");
                }
                i++;
            }
        }
        if(attached_dep_list.size() > 0)
        {
            for( DepNode dn : attached_dep_list ){
                int key1 = dn.key;
                Version ver1 = dn.v;
                //check version in the recved_versions of the key1;
                boolean is_present = false;
                if(recved_versions.containsKey(key1)){
                    for( Version ver : recved_versions.get(key1) ){
                        if(compare(ver1, ver)){
                            is_present = true;
                            break;
                        }
                    }
                    /*if(is_present){
                        //commit
                        //print 
                        key_v_store.put(key, val);
                        return true;
                    }*/
                }
                if(!is_present){
                    //add to pending
                    if(pending_list.containsKey(key1)){
                        pending_list.get(key1).add(ver1);
                    }else{
                        pending_list.put(key1, new ArrayList<Version>());
                        pending_list.get(key1).add(ver1);
                    }
                    can_commit = false;
                    System.out.println("Cannot commit as " + dn + " is not received");
                }
            }
        }
        if(can_commit){
            //lamport_clock++;
            lamport_clock = Math.max( lamport_clock, new_version.timestamp);
            key_v_store.put(key, val);
            //update recved versions
            if(recved_versions.containsKey(key)){
                recved_versions.get(key).add(new_version);
            }else{
                recved_versions.put(key, new ArrayList<Version>());
                recved_versions.get(key).add(new_version);
            }

            System.out.println("Committed key: " + key + " value: " + val + " and new version: " + new_version);

            //call pending replicated writes;
            int i = 0;
            RW rw;
            //cant use queues; .. can lead to infinite;
            while(!pending_rws.isEmpty()){
                //remove it and then call;
                rw = pending_rws.remove();
                if(rw.key != key && compare(rw.ver, new_version)){
                    System.out.println("At datacenter " + s_node.id + " recalling replicated write for key: " + rw.key + " and value:  " + rw.val + " and new version: " + rw.ver);

                    replicatedWrite(rw.key, rw.val, rw.att_list, rw.ver);
                }
            }
        } else{
            //add to pending replicated write;
            pending_rws.add(new RW(key, val, attached_dep_list, new_version));
            System.out.println("Cannot commit adding key: " + key + ", value: " + val + " and new version: " + new_version + " to pending list.");
            //return false;
        }

        return can_commit;
    }

    public boolean putKeyValue(int key, String val, NodeStruct c_node) throws RemoteException
    {
        System.out.println("Received client " + c_node.id + "'s write request for key " + key + " with value " + val);

        lamport_clock++;
        //replicated write
        Set<Map.Entry<String, IServer>> es = server_stubs.entrySet(); 
        Iterator<Map.Entry<String, IServer>> hmIterator = es.iterator(); 

        //the dependency list to be attached
        List<DepNode> att_dep_list = dependency_list.get(c_node.id);
        Version ver = new Version(lamport_clock, s_node.id);
        //do replicated writes to all DCs
        try{
            while(hmIterator.hasNext()){
                Map.Entry<String, IServer> mapElement = hmIterator.next(); 
                String server_id = mapElement.getKey();
                IServer sstub = mapElement.getValue();
                
                if(key == 1 && val.compareTo("X_A") == 0 && server_id.compareTo("s2") == 0)
                    sleep((long)5 * 1000);
                else if(key == 2 && val.compareTo("Y_C") == 0 && server_id.compareTo("s2") == 0)
                    sleep((long)10 * 1000);
                else if(key == 3 && val.compareTo("Z_B") == 0 && server_id.compareTo("s3") == 0)
                    sleep((long)25 * 1000);
                else if(key == 3 && val.compareTo("Z_D") == 0 && server_id.compareTo("s3") == 0)
                    sleep((long)10 * 1000);
                else if(key == 1 && val.compareTo("X_A") == 0 && server_id.compareTo("s3") == 0)
                    sleep((long)80 * 1000);
                else{
                    //sleep((long) 100 * 1000);
                }

                //get current time for demo:
                LocalDateTime curtime = LocalDateTime.now();
                int hour = curtime.getHour();
                int minute = curtime.getMinute();
                int second = curtime.getSecond();
                int millis = curtime.get(ChronoField.MILLI_OF_SECOND);

                System.out.print("Timestamp of replicated write to server " + server_id + " for key: " + key + " and value: " + val + " and version <" + ver.timestamp + ", " + ver.id + "> is: ");
                System.out.printf("%02d:%02d:%02d.%03d\n", hour, minute, second, millis);

                //TODO: insert a non cumulative sleep time
                sstub.replicatedWrite( key, val, att_dep_list, ver);
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        //add new dependency list;
        if(dependency_list.containsKey(c_node.id))
        {
            dependency_list.get(c_node.id).clear();
            DepNode dn = new DepNode(key, ver);
            dependency_list.get(c_node.id).add(dn);
        }
        else
        {
            dependency_list.put(c_node.id, new ArrayList<DepNode>());
            DepNode dn = new DepNode(key, ver);
            dependency_list.get(c_node.id).add(dn);
        }
        
        //add to recved_versions
        if(recved_versions.containsKey(key)){
            recved_versions.get(key).add(ver);
        }else{
            recved_versions.put(key, new ArrayList<Version>());
            recved_versions.get(key).add(ver);
        }

        //Pair<String, Version> new_p = new Pair<String, Version>(val, new Version(lamport_clock, s_node.id));
        //key_v_store.put(key, new_p);
        key_v_store.put(key, val);
        System.out.println("Added key: "+ key + " and value: " + val + " and the size of keyvstore is: " + key_v_store.size());
        return true;
    }

    //connect to all datacenters
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