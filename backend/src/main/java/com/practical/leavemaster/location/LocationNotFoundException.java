package com.practical.leavemaster.location;

public class LocationNotFoundException extends RuntimeException {

    public LocationNotFoundException(String id) {
        super("Location not found: " + id);
    }
}
