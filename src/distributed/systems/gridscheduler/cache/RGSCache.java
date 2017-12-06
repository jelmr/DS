package distributed.systems.gridscheduler.cache;

import distributed.systems.gridscheduler.neogui.RGSStatusFrameData;
import distributed.systems.gridscheduler.remote.RemoteGridScheduler;

import java.rmi.RemoteException;

/**
 * DS:distributed.systems.gridscheduler.cache.RGSCache
 * Written by Glenn. Created on 2017-12-05 at 22:02.
 */
public class RGSCache implements Cache, RGSStatusFrameData{

    private final RemoteGridScheduler source;

    private final Cached<String> name = new Cached<>();

    public RGSCache(RemoteGridScheduler source) {
        this.source = source;
    }

    @Override
    public void invalidate() {
        System.out.println("RGSCache invalidated");
        name.invalidate();
    }

    @Override
    public void reloadStaleValues() {
        reloadStaleName();
    }

    private void reloadStaleName() {
        if (name.isStale()) try {
            name.updateValue(source.getName());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNameValue() {
        reloadStaleName();
        return name.getValue();
    }
}

