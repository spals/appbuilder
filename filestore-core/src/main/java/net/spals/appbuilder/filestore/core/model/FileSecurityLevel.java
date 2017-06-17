package net.spals.appbuilder.filestore.core.model;

import com.google.common.annotations.Beta;

/**
 * The definition for file security.
 *
 * @author tkral
 */
public enum FileSecurityLevel {
    @Beta
    PRIVATE,
    PUBLIC,
    ;

    @Beta
    public boolean isPrivate() {
        return this == PRIVATE;
    }

    public boolean isPublic() {
        return this == PUBLIC;
    }
}
