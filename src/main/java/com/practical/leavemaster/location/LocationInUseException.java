package com.practical.leavemaster.location;

public class LocationInUseException extends RuntimeException {

    public LocationInUseException(String id) {
        super("Location is in use and cannot be deleted: " + id);
    }
}
