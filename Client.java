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
import java.time.LocalDateTime;
import java.util.Scanner;

public class Client implements IClient
{
    Registry server_registry;
    IServer server_stub;
    NodeStruct my_node; //stores my ip, my name and my port
    NodeStruct server_node;
    ArrayList<ArrayList<String>> messages;

    private Client(String id, String ip, int port, String sid, String sip, int sport) 
    {
	    my_node = new NodeStruct(id, ip, port);
        server_node = new NodeStruct(sid, sip, sport);
        messages = new ArrayList<ArrayList<String>>();
        //x = 1, y = 2, z = 3
        if(id.compareTo("A") == 0)
        {
            messages.add(new ArrayList<String>());
            messages.get(0).add("W");
            messages.get(0).add("1");
            messages.get(0).add("X_A");

        }
        else if(id.compareTo("B") == 0)
        {
            messages.add(new ArrayList<String>());
            messages.get(0).add("R");
            messages.get(0).add("1");
            
            messages.add(new ArrayList<String>());
            messages.get(1).add("R");
            messages.get(1).add("2");

            messages.add(new ArrayList<String>());
            messages.get(2).add("W");
            messages.get(2).add("3");
            messages.get(2).add("Z_B");

        }
        else if(id.compareTo("C") == 0)
        {
            messages.add(new ArrayList<String>());
            messages.get(0).add("W");
            messages.get(0).add("2");
            messages.get(0).add("Y_C");
        }
        else if(id.compareTo("D") == 0)
        {
            messages.add(new ArrayList<String>());
            messages.get(0).add("W");
            messages.get(0).add("3");
            messages.get(0).add("Z_D");
        }

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
            
            /*if(cli.my_node.id.compareTo("A") == 0)
            {
                long ms = 1 * 1000;
                sleep(ms);
            }
            else if(cli.my_node.id.compareTo("B") == 0)
            {
                long ms = 20 * 1000;
                sleep(ms);
            }
            else if(cli.my_node.id.compareTo("C") == 0)
            {
                long ms = 1 * 1000;
                sleep(ms);
            }
            else if(cli.my_node.id.compareTo("D") == 0)
            {
                long ms = 55 * 1000;
                sleep(ms);
            }
*/
            Scanner in = new Scanner(System.in);
            for(int i = 0; i < cli.messages.size(); i++)
            {
                String s = in.nextLine();
                //System.out.println("You entered string "+s);

                if(cli.messages.get(i).get(0).compareTo("R") == 0)
                {
                    System.out.println("Client " + cli.my_node.id + " read key " + Integer.parseInt(cli.messages.get(i).get(1))+ " and value is: " + cli.read(Integer.parseInt(cli.messages.get(i).get(1))));
                }
                else if(cli.messages.get(i).get(0).compareTo("W") == 0)
                {
                    boolean flag = cli.write(Integer.parseInt(cli.messages.get(i).get(1)), cli.messages.get(i).get(2));
                    if(flag)
                        System.out.println("Client " + cli.my_node.id + " successfully wrote to key " + Integer.parseInt(cli.messages.get(i).get(1)) + " the value " + cli.messages.get(i).get(2));
                    else if (!flag)
                        System.out.println("Client " + cli.my_node.id + " could not write to key " + Integer.parseInt(cli.messages.get(i).get(1)) + " the value " + cli.messages.get(i).get(2));
                }
            }
			in.close();

        } 
        catch (Exception e) 
        {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
