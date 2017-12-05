package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.model.*;
import distributed.systems.gridscheduler.remote.RemoteClient;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


/**
 * A special client that can simulate workloads.
 * <p>
 * Roughly similar to Client, except that it makes use of a configuration
 * file to figure out what sort of jobs to send where.
 * <p>
 * Supposed to supplement tim's python script.
 * <p>
 * NOTE:
 * to make debugging somewhat easier, the random generator is seeded, so output should be
 * the same given the same input.
 *
 * @author Arthur de Fluiter
 *         Date: 5/12/2017
 */
public class MultiClient implements RemoteClient {
    private final static String NAME = "Tim";
    private Map<String, RemoteResourceManager> resourceManagers;
    private LogicalClock                       logicalClock;
    private ConcurrentHashMap<String, Boolean> jobCompleted;

    private          Registry     registry;
    private          RemoteClient remoteClientStub;
    private volatile boolean      stillScheduling;
    private final static long PERIOD_BETWEEN_JOBCREATION = 100;


    public MultiClient(String registryHost, int registryPort) {
        this.resourceManagers = new ConcurrentHashMap<>();
        this.jobCompleted = new ConcurrentHashMap<>();

        this.logicalClock = new LamportsClock();

        try {
            registry = LocateRegistry.getRegistry(registryHost, registryPort);
            remoteClientStub = (RemoteClient) UnicastRemoteObject.exportObject(this, 0);
            registry.rebind(NAME, remoteClientStub);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /* ************************************************************************
     * Implementing the RemoteClient interface                                *
     ************************************************************************ */

    @Override
    public String getName() throws RemoteException {
        return NAME;
    }

    @Override
    public void jobDone(Job job) throws RemoteException {
        System.out.printf("Received response for job '%s'\n", job.getId());

        // Logging response
        Event event = new Event.TypedEvent(this.logicalClock, EventType.CLIENT_JOB_DONE, this.getName(), job.getId());
        if (!job.getIssueingResourceManager().logEvent(event)) {
            System.out.printf("Couldn't find any ResourceManagers to log event to...\n");
        }

        // remove from queue and check if there's more to do
        this.jobCompleted.remove(job.getId());
        if (hasOutStandingJobs())
            return;

        // reaching here means we're done for today :)
        try {
            closeClientRMI();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    /* ************************************************************************
     * Helper methods                                                         *
     ************************************************************************ */

    /**
     * Resolves the client stub of a remote resource manager (cached)
     *
     * @param name as registered in the registry
     * @return the RMI interface
     */
    private RemoteResourceManager getRemoteResourceManager(String name) {
        if (resourceManagers.containsKey(name))
            return resourceManagers.get(name);

        try {
            RemoteResourceManager rrm = (RemoteResourceManager) registry.lookup(name);
            resourceManagers.put(name, rrm);
            return rrm;
        } catch (RemoteException | NotBoundException e) {
            System.out.println("Encountered an error or smth");
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    /**
     * Attempts to schedule a job on one of the schedulers given, in order
     *
     * @param job        to be scheduled
     * @param schedulers the list of schedulers to attempt to schedule it on, in order
     * @throws RemoteException if it fails :P
     */
    private void scheduleJob(Job job, List<Named<RemoteResourceManager>> schedulers) throws RemoteException {
        for (Named<RemoteResourceManager> s : schedulers) {
            String                rrmName = s.getName();
            RemoteResourceManager rrm     = s.getObject();

            job.setIssueingResourceManager(rrm);

            Event event = new Event.TypedEvent(this.logicalClock, EventType.CLIENT_JOB_SCHEDULE_ATTEMPT, NAME,
                    job.getId(), rrmName);

            if (!RemoteResourceManager.logEvent(schedulers, event))
                System.out.println("Couldn't find any ResourceManagers to log event to...");

            try {
                if (rrm.addJob(job)) {
                    jobCompleted.put(job.getId(), false);
                    System.out.printf("Scheduled job {.name=%s, .duration=%d, .rscmngr=%s}\n", job.getId(),
                            job.getDuration(), rrmName);

                    break;
                }
            }
            catch (RemoteException e) {
                continue; // if this one doesn't work, too bad, nexttt.
            }
        }
    }

    public boolean hasOutStandingJobs() {
        return jobCompleted.size() > 0 || stillScheduling;
    }

    /**
     * Called as soon as the last job is satisfied.
     *
     * @throws RemoteException   not that important, we're closing anyway
     * @throws NotBoundException ignore me
     */
    private void closeClientRMI() throws RemoteException, NotBoundException {
        Event                              exitEvent = new Event.TypedEvent(this.logicalClock, EventType.CLIENT_EXITING, this.getName());
        List<Named<RemoteResourceManager>> rm        = new ArrayList<>();
        for (Map.Entry<String, RemoteResourceManager> entry : this.resourceManagers.entrySet()) {
            rm.add(new Named<>(entry.getKey(), entry.getValue()));
        }

        if (!RemoteResourceManager.logEvent(rm, exitEvent))
            System.out.printf("Couldn't find any ResourceManagers to log event to...\n");

        UnicastRemoteObject.unexportObject(this, true);

        for (Named<RemoteResourceManager> resourceManager : rm)
            UnicastRemoteObject.unexportObject(resourceManager.getObject(), true);

        UnicastRemoteObject.unexportObject(this.registry, true);
        UnicastRemoteObject.unexportObject(this.remoteClientStub, true);

        this.registry.unbind(NAME);
        this.registry = null;
        this.remoteClientStub = null;
        this.resourceManagers.clear();
        this.resourceManagers = null;
    }


    /**
     * Parses a Tim configuration file (c), which he defined as:
     * jobId (int) ',' timeInSec (int) ',' comma separated list of resource managers, one of which is chosen (at random)
     * <p>
     * With the addition of being able to add comments with '//'
     *
     * @param filepath path to Tim configuration file (c)
     * @throws FileNotFoundException thrown if Tim cannot be found
     */
    private void execConfiguration(String filepath) throws FileNotFoundException {
        Scanner lineScanner = new Scanner(new File(filepath));
        Random  r           = new Random(0xbadcafe);
        stillScheduling = true;

        while (lineScanner.hasNext()) {
            String line = lineScanner.nextLine().split("//")[0].trim();
            if (line.equals(""))
                continue;

            Scanner tripletScanner = new Scanner(line);
            tripletScanner.useDelimiter(Pattern.compile("\\s*,\\s*"));

            String id        = String.format("Job%d", tripletScanner.nextInt());
            int    timeInSec = tripletScanner.nextInt();

            ArrayList<Named<RemoteResourceManager>> rmList = new ArrayList<>();

            while (tripletScanner.hasNext()) {
                String resourceManager = tripletScanner.next();
                rmList.add(new Named<>(resourceManager, getRemoteResourceManager(resourceManager)));
            }

            Collections.shuffle(rmList);

            int timeInusec = timeInSec * 1000 + 100 + (r.nextInt() % 1000);

            Job job = new Job(timeInusec, id, null, this.remoteClientStub, NAME);
            try {
                scheduleJob(job, rmList);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(PERIOD_BETWEEN_JOBCREATION);
            } catch (InterruptedException e) {
                System.out.println("Interrupted, whooptee doo, I'm done for today");
                return;
            }
        }

        stillScheduling = false;
    }


    public static void main(String[] args) throws RemoteException {
        if (args.length != 3) {
            System.out.printf("Usage: gradle client  -Pargv=\"['<confFile>', '<registryHost>', '<registryPort>'");
            System.exit(1);
        }

        String confFile     = args[0];
        String registryHost = args[1];
        int    registryPort = Integer.valueOf(args[2]);

        MultiClient m = new MultiClient(registryHost, registryPort);
        try {
            m.execConfiguration(confFile);
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't find configuration file");
        }
    }
}
