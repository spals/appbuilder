package net.spals.appbuilder.filestore.core.model;

import org.inferred.freebuilder.FreeBuilder;

import java.io.InputStream;
import java.util.Optional;

/**
 * @author tkral
 */
@FreeBuilder
public interface PutFileStoreRequest {

    InputStream getFileStream();

    FileSecurityLevel getFileSecurityLevel();

    Optional<Long> getContentLength();

    Optional<String> getContentType();

    class Builder extends PutFileStoreRequest_Builder {  }
}
