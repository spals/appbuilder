package net.spals.appbuilder.filestore.core.model;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link FileStoreKey}
 *
 * @author tkral
 */
public class FileStoreKeyTest {

    @DataProvider
    Object[][] toGlobalIdProvider() {
        return new Object[][] {
                // Case: No custom partitions
                {new FileStoreKey.Builder()
                        .setPartition("myPartition")
                        .setId("myId")
                        .build(), "myPartition/myId"},
                // Case: Exactly one sub partition
                {new FileStoreKey.Builder()
                        .setPartition("myPartition")
                        .addSubPartitions("subpartition1")
                        .setId("myId")
                        .build(), "myPartition/subpartition1/myId"},
                // Case: Multiple sub partitions
                {new FileStoreKey.Builder()
                        .setPartition("myPartition")
                        .addSubPartitions("subpartition1", "subpartition2")
                        .setId("myId")
                        .build(), "myPartition/subpartition1/subpartition2/myId"},
        };
    }

    @Test(dataProvider = "toGlobalIdProvider")
    public void testToGlobalId(final FileStoreKey fileKey, final String expectedGlobalId) {
        assertThat(fileKey.toGlobalId("/"), is(expectedGlobalId));
    }
}
