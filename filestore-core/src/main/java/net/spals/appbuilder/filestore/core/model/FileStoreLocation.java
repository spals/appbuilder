package net.spals.appbuilder.filestore.core.model;

/**
 * @author tkral
 */
public enum FileStoreLocation {
    LOCAL,
    REMOTE,
    ;

    public boolean isLocal() {
        return this == LOCAL;
    }

    public boolean isRemote() {
        return this == REMOTE;
    }
}
