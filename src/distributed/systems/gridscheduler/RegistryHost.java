package distributed.systems.gridscheduler;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


/**
 * @author Jelmer Mulder
 *         Date: 03/12/2017
 */
public class RegistryHost {



	public static void main(String[] args) {

		if (args.length < 1) {
			System.out.printf("Usage: gradle registry  -Pargv=\"['<port>']\"");
			System.exit(1);
		}

		int port = Integer.parseInt(args[0]);
		try {
			Registry registry = LocateRegistry.createRegistry(port);
			// TODO: Get proper IP instead of localhost
			String registryHost = "127.0.0.1";

			System.out.printf("Started a registry on %s:%d\n", registryHost, port);
		} catch (RemoteException e) {
			e.printStackTrace();
		}


		while(true){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
