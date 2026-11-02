package com.deadlocksim.model;

public class Resource {
    private final String id;
    private final String name;
    private final int total;
    private final int available;
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
}
