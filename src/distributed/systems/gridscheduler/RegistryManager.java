package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.remote.RemoteRegistry;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public class RegistryManager {
    private String registryAddr;
    private int registryPort;

    RegistryManager(String registryAddr, int registryPort) {
        this.registryAddr = registryAddr;
        this.registryPort = registryPort;
    }

    private String getUrl(String name) {
        return String.format("//%s:%d/%s", registryAddr, registryPort, name);
    }

    void bind(String name, Remote r) throws RemoteException {
        System.out.println(getUrl(name));

        try {
            RemoteRegistry registry = (RemoteRegistry) Naming.lookup(getUrl("registry"));
            registry.proxyBind(name, r);
//            Naming.rebind(getUrl(name), r);
        } catch (MalformedURLException e) {
            System.out.println("url was borked: " + getUrl(name));
            e.printStackTrace();
            System.exit(-1);
        } catch (NotBoundException e) {
            System.out.println("Not bound :(");
        }
    }

    Remote lookup(String name) throws NotBoundException, RemoteException {
        try {
            return Naming.lookup(getUrl(name));
        } catch (MalformedURLException e) {
            System.out.println("url was borked: " + getUrl(name));
            e.printStackTrace();
            System.exit(-1);
        }

        return null;
    }

    void unbind(String name) throws RemoteException, NotBoundException {
        try {
            Naming.unbind(getUrl(name));
        } catch (MalformedURLException e) {
            System.out.println("url was borked: " + getUrl(name));
            e.printStackTrace();
            System.exit(-1);
        }
    }

    String getRegistryHost() { return registryAddr; }
    int getRegistryPort() { return registryPort; }
}
