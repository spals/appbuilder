package net.spals.appbuilder.filestore.core.model;

import net.spals.appbuilder.filestore.core.FileStore;
import org.inferred.freebuilder.FreeBuilder;

import java.io.InputStream;
import java.util.Optional;

/**
 * A request bean for putting files in a {@link FileStore}.
 *
 * @author tkral
 */
@FreeBuilder
public interface PutFileRequest {

    InputStream getFileStream();

    FileSecurityLevel getFileSecurityLevel();

    Optional<Long> getContentLength();

    Optional<String> getContentType();

    class Builder extends PutFileRequest_Builder {  }
}
