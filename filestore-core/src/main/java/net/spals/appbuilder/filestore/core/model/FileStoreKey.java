package net.spals.appbuilder.filestore.core.model;

import com.google.common.base.Joiner;
import org.inferred.freebuilder.FreeBuilder;

import java.util.List;
import java.util.Optional;

/**
 * @author tkral
 */
@FreeBuilder
public interface FileStoreKey {

    String getPartition();

    String getId();

    List<String> getSubPartitions();

    default String toGlobalId(final String delimiter) {
        final StringBuilder globalIdBuilder = new StringBuilder(getPartition());

        final Joiner globalIdJoiner = Joiner.on(delimiter);
        Optional.of(getSubPartitions()).filter(subPartitions -> !subPartitions.isEmpty())
                .ifPresent(subPartitions -> {
                    globalIdBuilder.append(delimiter);
                    globalIdJoiner.appendTo(globalIdBuilder, subPartitions);
                });
        globalIdBuilder.append(delimiter).append(getId());

        return globalIdBuilder.toString();
    }

    class Builder extends FileStoreKey_Builder {  }
}
