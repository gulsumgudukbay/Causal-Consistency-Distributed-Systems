import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.io.File;
import java.rmi.Naming;
//import java.rmi.RMISecurityManager;
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

    String peer_IP = "";
    int peer_port = 0;
    int peer_cnt = 0;
    int epoch = 0;

    public Server() {

    }

    public void run() {

    }

    public static void main(String args[]) {

        try {
            System.setProperty("java.security.policy", "security.policy");

            Server obj = new Server();
            IServer stub = (IServer) UnicastRemoteObject.exportObject(obj, 2014);

            Registry registry = LocateRegistry.createRegistry(2014);
            registry.rebind("IServer", stub);
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");

            System.err.println("Server ready");
            // testing

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }

}