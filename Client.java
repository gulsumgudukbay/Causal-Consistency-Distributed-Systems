import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.Random; 
import java.util.List; 

public class Client implements IClient{

    final public static int BUF_SIZE = 1024 * 64;
    String my_IP = "";
    String server_IP = "";
    String my_id = "";
    int my_port = 0;
    int server_port = 0;
    Registry server_registry;
    static IServer server_stub;
    static NodeStruct my_node; //stores my ip, my name and my port
    

    private Client(String id, String ip, int port, String sip, int sport) {
    	my_IP = ip;
    	my_port = port;
    	server_IP = sip;
    	server_port = sport;
    	my_id = id;

	    my_node = new NodeStruct(my_id, my_IP, my_port);

	
      	try {
            System.setProperty("java.security.policy","security.policy");
    		server_registry = LocateRegistry.getRegistry(server_IP, server_port);
            server_stub = (IServer) server_registry.lookup("IServer");

        } catch (Exception e) {
            System.err.println("peer exception " + e.toString());
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {


        try {
            System.setProperty("java.security.policy","security.policy");

            Client cli = new Client(args[0], args[1], Integer.parseInt(args[2]), args[3], Integer.parseInt(args[4]));
            System.setProperty("java.rmi.server.hostname", cli.my_IP);
            IClient mystub = (IClient) UnicastRemoteObject.exportObject(cli, cli.my_port);
            
            Registry registry = LocateRegistry.createRegistry( cli.my_port);
            registry.rebind(cli.my_id,mystub);

            System.out.println(cli.my_id + " ready");
			
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
