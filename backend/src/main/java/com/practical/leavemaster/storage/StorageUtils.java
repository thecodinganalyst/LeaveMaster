package com.practical.leavemaster.storage;

class StorageUtils {

    private StorageUtils() {}

    static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
