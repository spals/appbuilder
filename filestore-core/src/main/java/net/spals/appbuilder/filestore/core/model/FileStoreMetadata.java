package net.spals.appbuilder.filestore.core.model;

import org.inferred.freebuilder.FreeBuilder;

import java.net.URI;

/**
 * @author tkral
 */
@FreeBuilder
public interface FileStoreMetadata {

    FileScope getFileScore();

    URI getFileURI();

    FileStoreLocation getStoreLocation();

    class Builder extends FileStoreMetadata_Builder {  }
}
