package net.spals.appbuilder.filestore.core.model;

/**
 * @author tkral
 */
public enum FileScope {
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
