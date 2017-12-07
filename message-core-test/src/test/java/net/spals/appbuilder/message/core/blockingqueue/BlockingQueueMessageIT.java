package net.spals.appbuilder.message.core.blockingqueue;

import com.google.common.collect.ImmutableSet;
import com.typesafe.config.ConfigFactory;
import net.spals.appbuilder.config.message.MessageConsumerConfig;
import net.spals.appbuilder.config.message.MessageProducerConfig;
import net.spals.appbuilder.executor.core.ExecutorServiceFactory;
import net.spals.appbuilder.executor.core.ExecutorServiceFactory.Key;
import net.spals.appbuilder.message.core.TestMessageConsumerCallback;
import net.spals.appbuilder.model.core.ModelSerializer;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the blocking queue message system.
 *
 * @author tkral
 */
public class BlockingQueueMessageIT {

    private final BlockingQueue<BlockingQueueMessage> blockingQueue = new LinkedBlockingQueue<>(Integer.MAX_VALUE);
    private final BlockingQueueMessageProducerPlugin producerPlugin =
            new BlockingQueueMessageProducerPlugin(ConfigFactory.empty(), blockingQueue);

    private final MessageProducerConfig producerConfig = new MessageProducerConfig.Builder()
            .setTag("myTag").setGlobalId("myProducerId").setFormat("pojo")
            .setDestination("blockingQueue").setChannel("myChannel").build();
    private final MessageConsumerConfig consumerConfig = new MessageConsumerConfig.Builder()
            .setTag("myTag").setGlobalId("myConsumerId").setFormat("pojo")
            .setSource("blockingQueue").setChannel("myChannel").build();

    @Test
    public void testBlockingQueueMessage() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(1);
        final TestMessageConsumerCallback consumerCallback =
                new TestMessageConsumerCallback(cdl, "payload");

        final BlockingQueueMessageConsumerPlugin consumerPlugin =
                new BlockingQueueMessageConsumerPlugin(ConfigFactory.empty(),
                        ImmutableSet.of(consumerCallback),
                        executorServiceFactory(),
                        blockingQueue);
        consumerPlugin.start(consumerConfig, modelSerializer());

        producerPlugin.sendMessage(producerConfig, "payload".getBytes());
        assertThat(cdl.await(1L, TimeUnit.SECONDS), is(true));
    }

    private ExecutorServiceFactory executorServiceFactory() {
        final ExecutorServiceFactory executorServiceFactory = mock(ExecutorServiceFactory.class);
        when(executorServiceFactory.createFixedThreadPool(anyInt(), any(Key.class)))
                .thenReturn(Executors.newSingleThreadExecutor());

        return executorServiceFactory;
    }

    private ModelSerializer modelSerializer() throws IOException {
        final ModelSerializer modelSerializer = mock(ModelSerializer.class);
        when(modelSerializer.serialize(any()))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0).toString().getBytes());
        when(modelSerializer.deserialize(any()))
                .thenAnswer(invocationOnMock -> new String((byte[])invocationOnMock.getArgument(0)));

        return modelSerializer;
    }
}
