package distributed.systems.gridscheduler.cache;

/**
 * DS:distributed.systems.gridscheduler.cache.Cached
 * Written by Glenn. Created on 2017-12-05 at 21:57.
 */
public class Cached<V> {
    private boolean stale;
    private V value;

    public Cached() {
        this.stale = true;
        this.value = null;
    }

    public Cached(boolean stale, V value) {
        this.stale = stale;
        this.value = value;
    }

    public boolean isStale() {
        return stale;
    }

    public V getValue() {
        return value;
    }

    public void updateValue(V value) {
        this.value = value;
        this.stale = false;
    }

    public void invalidate() {
        this.stale = true;
    }
}
