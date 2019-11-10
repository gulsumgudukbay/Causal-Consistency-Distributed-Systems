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

public class Client implements IClient
{
    Registry server_registry;
    IServer server_stub;
    NodeStruct my_node; //stores my ip, my name and my port
    NodeStruct server_node;

    private Client(String id, String ip, int port, String sid, String sip, int sport) 
    {
	    my_node = new NodeStruct(id, ip, port);
        server_node = new NodeStruct(sid, sip, sport);

        try 
        {
            System.setProperty("java.security.policy","security.policy");
    		server_registry = LocateRegistry.getRegistry(sip, sport);
            server_stub = (IServer) server_registry.lookup(sid);
        } 
        catch (Exception e) 
        {
            System.err.println("peer exception " + e.toString());
            e.printStackTrace();
        }
    }

    public void registerRequest()
    {
        try
        {
            server_stub.registerReply(my_node);
        }
        catch(RemoteException e)
        {
            e.printStackTrace();
        }
    }

    public String read(int key)
    {
        String ret = "";
        try
        {
            ret = server_stub.getKey(key, my_node);
            System.out.println(key + ": " + ret);
        }
        catch(RemoteException e)
        {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean write(int key, String val)
    {
        boolean ret = false;
        try
        {
            ret = server_stub.putKeyValue(key, val, my_node);
            System.out.println(key + ": " + ret);
        }
        catch(RemoteException e)
        {
            ret = false;
            e.printStackTrace();
        }
        return ret;
    }


    public static void main(String[] args) 
    {
        try 
        {
            System.setProperty("java.security.policy","security.policy");

            Client cli = new Client(args[0], args[1], Integer.parseInt(args[2]), args[3], args[4], Integer.parseInt(args[5]));
            //Client cli = new Client("c1", "127.0.0.1", 2015, "s1", "127.0.0.1", 2014);

            System.setProperty("java.rmi.server.hostname", cli.my_node.ip);
            IClient mystub = (IClient) UnicastRemoteObject.exportObject(cli, cli.my_node.port);
            
            Registry registry = LocateRegistry.createRegistry( cli.my_node.port);
            registry.rebind(cli.my_node.id, mystub);

            System.out.println(cli.my_node.id + " ready");

            System.out.println(cli.my_node.id + " sent register request for server " + cli.server_node.id);

            mystub.registerRequest();
            if(cli.my_node.id.compareTo("c1") == 0)
            {
                cli.write(1, "a");
                cli.write(2, "b");
                cli.write(3, "c");
                cli.read(2);
            }
            if(cli.my_node.id.compareTo("c2") == 0)
            {
                cli.write(1, "x");
                cli.write(5, "y");
                cli.write(6, "z");
                cli.read(1);
                cli.read(5);

            }
			
        } 
        catch (Exception e) 
        {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
