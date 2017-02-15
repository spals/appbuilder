package net.spals.appbuilder.filestore.core.model;

import org.inferred.freebuilder.FreeBuilder;

import java.net.URI;

/**
 * @author tkral
 */
@FreeBuilder
public interface FileMetadata {

    FileScope getScope();

    URI getURI();

    FileStoreLocation getStoreLocation();

    class Builder extends FileMetadata_Builder {  }
}
