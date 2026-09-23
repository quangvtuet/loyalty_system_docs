package com.loyalty.capstone.store;

/**
 * Raised when a container that does not own an I-7 object tries to write it.
 * This is how CON.2 and the I-9 forbidden path are made impossible rather than merely documented.
 */
public class OwnershipViolation extends RuntimeException {
    public OwnershipViolation(String store, String owner, String attemptedWriter) {
        super("CON.2 violation: '" + attemptedWriter + "' may not write '" + store
                + "'. The only writer is '" + owner + "'.");
    }
}
