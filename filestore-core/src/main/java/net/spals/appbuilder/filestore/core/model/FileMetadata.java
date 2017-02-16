package net.spals.appbuilder.filestore.core.model;

import net.spals.appbuilder.filestore.core.FileStore;
import org.inferred.freebuilder.FreeBuilder;

import java.net.URI;

/**
 * A metadata bean which holds information about a
 * file stored in a {@link FileStore}.
 *
 * Note that individual {@link FileStore} implementations
 * may choose to provide expanded metadata.
 *
 * @author tkral
 */
@FreeBuilder
public interface FileMetadata {

    /**
     * The security level for the stored
     * file in the file store.
     */
    FileSecurityLevel getSecurityLevel();

    /**
     * The {@link URI} which gives access
     * to the file's contents.
     */
    URI getURI();

    /**
     * Returns the general location of the
     * file store holding the file (either
     * local or remote).
     */
    FileStoreLocation getStoreLocation();

    class Builder extends FileMetadata_Builder {  }
}
