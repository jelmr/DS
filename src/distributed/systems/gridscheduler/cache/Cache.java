package distributed.systems.gridscheduler.cache;

/**
 * DS:distributed.systems.gridscheduler.cache.Cache
 * Written by Glenn. Created on 2017-12-05 at 21:58.
 */
public interface Cache {
    void invalidate();

    void forceRefresh();
}
