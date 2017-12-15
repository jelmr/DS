package distributed.systems.gridscheduler.remote;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRegistry extends Remote {
    void proxyBind(String name, Remote r) throws RemoteException;
}
