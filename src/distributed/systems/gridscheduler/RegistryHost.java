package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.remote.RemoteRegistry;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


/**
 * @author Jelmer Mulder
 *         Date: 03/12/2017
 */
public class RegistryHost implements RemoteRegistry {
	private Registry host;
	private int 	 port;

	RegistryHost(int port) throws RemoteException {
		this.port = port;

		// Start a registry on this machine
		host = LocateRegistry.createRegistry(port);

		// Bind service to it
		host.rebind("registry", UnicastRemoteObject.exportObject(this, 0));

		try {
			LocateRegistry.getRegistry(port).lookup("registry");
			System.out.println("RegistryHost object connected correctly");
		} catch (NotBoundException e) {
			System.out.println("Registry binding went wrong");
		}
	}

	@Override
	public void proxyBind(String name, Remote r) {
		System.out.println("Binding " + name + " to localhost");

		try {
			host.rebind(name, r);
		} catch (RemoteException e) {
			System.out.println("Failed to connect to myself... that's a new one");
			e.printStackTrace();
		}
	}

	@Override
	public void proxyUnbind(String name) throws RemoteException {
		try {
			host.unbind(name);
		} catch (NotBoundException e) {
			System.out.println("Unbind without bind: " + name);
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.printf("Usage: gradle registry  -Pargv=\"['<port>']\"");
			System.exit(1);
		}

		int port = Integer.parseInt(args[0]);
		try {
			System.out.println("Binding to the registry");
			new RegistryHost(port);
			System.out.println("Just finished starting our own proxy bound registry");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}


}
