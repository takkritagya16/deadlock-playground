package com.deadlocksim.model;

public class Resource {
    private final String id;
    private final String name;
    private final int total;
    private int available;
    private final boolean shareable;

    public Resource(String id, int total, boolean shareable) {
        if (total <= 0) {
            throw new IllegalArgumentException("Total instances must be greater than zero");
        }
        this.id = id;
        this.name = id;
        this.total = total;
        this.available = total;
        this.shareable = shareable;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTotal() {
        return total;
    }

    public int getAvailable() {
        return available;
    }

    public boolean isShareable() {
        return shareable;
    }

    public boolean getShareable() {
        return shareable;
    }

    public boolean grant(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Grant count must be greater than zero");
        }
        if (n <= available) {
            available -= n;
            return true;
        }
        return false;
    }

    public void release(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Release count must be greater than zero");
        }
        if (available + n > total) {
            throw new IllegalStateException("Releasing instances would exceed total instances");
        }
        available += n;
    }

    void restoreAvailable(int a) {
        if (a < 0 || a > total) {
            throw new IllegalArgumentException("Available instances must be between 0 and total");
        }
        this.available = a;
    }
}
