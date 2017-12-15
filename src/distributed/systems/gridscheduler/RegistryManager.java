package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.remote.RemoteRegistry;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RegistryManager {
    private String registryAddr;
    private int registryPort;
    private Registry registry;
    private RemoteRegistry proxyRegistry;

    RegistryManager(String registryAddr, int registryPort) throws RemoteException {
        this.registryAddr = registryAddr;
        this.registryPort = registryPort;

        this.registry = LocateRegistry.getRegistry(registryAddr, registryPort);
        System.out.println("Registry connection created");

        try {
            this.proxyRegistry = (RemoteRegistry) registry.lookup("registry");
            System.out.println("Looked up registryhost object");
        } catch (NotBoundException e) {
            System.out.println("RegistryHost wasn't bound to registry");
            System.exit(1);
        }
    }

//    private String getUrl(String name) {
//        return String.format("//%s:%d/%s", registryAddr, registryPort, name);
//    }

    void bind(String name, Remote r) throws RemoteException {
        System.out.println("RegistryManager.bind(" + name + ")");
        proxyRegistry.proxyBind(name, r);
    }

    Remote lookup(String name) throws NotBoundException, RemoteException {
        return registry.lookup(name);
    }

    void unbind(String name) throws RemoteException {
        proxyRegistry.proxyUnbind(name);
    }

//    void bind(String name, Remote r) throws RemoteException {
//        System.out.println(getUrl(name));
//
//        try {
//            RemoteRegistry registry = (RemoteRegistry) Naming.lookup(getUrl("registry"));
//            registry.proxyBind(name, r);
////            Naming.rebind(getUrl(name), r);
//        } catch (MalformedURLException e) {
//            System.out.println("url was borked: " + getUrl(name));
//            e.printStackTrace();
//            System.exit(-1);
//        } catch (NotBoundException e) {
//            System.out.println("Not bound :(");
//        }
//    }
//
//    Remote lookup(String name) throws NotBoundException, RemoteException {
//        try {
//            return Naming.lookup(getUrl(name));
//        } catch (MalformedURLException e) {
//            System.out.println("url was borked: " + getUrl(name));
//            e.printStackTrace();
//            System.exit(-1);
//        }
//
//        return null;
//    }
//
//    void unbind(String name) throws RemoteException, NotBoundException {
//        try {
//            Naming.unbind(getUrl(name));
//        } catch (MalformedURLException e) {
//            System.out.println("url was borked: " + getUrl(name));
//            e.printStackTrace();
//            System.exit(-1);
//        }
//    }

    String getRegistryHost() { return registryAddr; }
    int getRegistryPort() { return registryPort; }
}
