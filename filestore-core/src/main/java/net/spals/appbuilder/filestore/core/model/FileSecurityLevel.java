package net.spals.appbuilder.filestore.core.model;

/**
 * The definition for file security.
 *
 * @author tkral
 */
public enum FileSecurityLevel {
    PRIVATE,
    PUBLIC,
    ;

    public boolean isPrivate() {
        return this == PRIVATE;
    }

    public boolean isPublic() {
        return this == PUBLIC;
    }
}
