package distributed.systems.gridscheduler.cache;

import distributed.systems.gridscheduler.remote.RemoteGridScheduler;

import java.rmi.RemoteException;

/**
 * DS:distributed.systems.gridscheduler.cache.RGSCache
 * Written by Glenn. Created on 2017-12-05 at 22:02.
 */
public class RGSCache implements Cache {

    private String name;
    private final RemoteGridScheduler source;

    private Cached<Integer> waitingJobs;

    public RGSCache(String name, RemoteGridScheduler source) {
        this.name = name;
        this.source = source;

        waitingJobs = new Cached<>();

        forceRefresh();
    }

    @Override
    public void invalidate() {
        waitingJobs.invalidate();
    }

    @Override
    public void forceRefresh() {
        try {
            waitingJobs.updateValue(source.getWaitingJobs());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public int getWaitingJobs() {
        if (waitingJobs.isStale()) {
            try {
                waitingJobs.updateValue(source.getWaitingJobs());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return waitingJobs.getValue();
    }
}

