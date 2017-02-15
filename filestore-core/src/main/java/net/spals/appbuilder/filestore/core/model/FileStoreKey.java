package net.spals.appbuilder.filestore.core.model;

import org.inferred.freebuilder.FreeBuilder;

import java.util.List;

/**
 * @author tkral
 */
@FreeBuilder
public interface FileStoreKey {

    String getPartition();

    String getId();

    List<String> getSubPartitions();

    class Builder extends FileStoreKey_Builder {  }
}
