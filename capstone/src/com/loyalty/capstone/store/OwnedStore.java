package com.loyalty.capstone.store;

/**
 * Base for every I-4 data container. Each store names the one I-4 container allowed to write it,
 * so a write from anywhere else is rejected at runtime.
 */
public abstract class OwnedStore {

    private final String storeContainerName;
    private final String ownerContainerName;

    protected OwnedStore(String storeContainerName, String ownerContainerName) {
        this.storeContainerName = storeContainerName;
        this.ownerContainerName = ownerContainerName;
    }

    public String storeContainerName() { return storeContainerName; }

    public String ownerContainerName() { return ownerContainerName; }

    protected void assertWriter(String writerContainerName) {
        if (!ownerContainerName.equals(writerContainerName)) {
            throw new OwnershipViolation(storeContainerName, ownerContainerName, writerContainerName);
        }
    }
}
