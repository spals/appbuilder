package net.spals.appbuilder.message.core.consumer;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.config.message.MessageConsumerConfig;
import net.spals.appbuilder.message.core.MessageConsumer;
import net.spals.appbuilder.model.core.ModelSerializer;
import org.testng.annotations.Test;

import static com.googlecode.catchexception.CatchException.verifyException;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DefaultMessageConsumer}.
 *
 * @author tkral
 */
public class DefaultMessageConsumerTest {

    @Test
    public void testStart() {
        final MessageConsumerConfig consumerConfig = new MessageConsumerConfig.Builder()
            .setChannel("myChannel").setSource("mySrc")
            .setFormat("myFormat").setGlobalId("myId").setTag("myTag")
            .build();
        final ModelSerializer serializer = mock(ModelSerializer.class);
        final MessageConsumerPlugin consumerPlugin = mock(MessageConsumerPlugin.class);
        final MessageConsumer messageConsumer = new DefaultMessageConsumer(
            ImmutableMap.of("myTag", consumerConfig),
            ImmutableMap.of("myFormat", serializer),
            ImmutableMap.of("mySrc", consumerPlugin)
        );

        messageConsumer.start();
        verify(consumerPlugin).start(same(consumerConfig), same(serializer));
    }

    @Test
    public void testMissingConsumerPlugin() {
        final MessageConsumerConfig consumerConfig = new MessageConsumerConfig.Builder()
            .setChannel("myChannel").setSource("mySrc")
            .setFormat("myFormat").setGlobalId("myId").setTag("myTag")
            .build();
        final ModelSerializer serializer = mock(ModelSerializer.class);
        final MessageConsumer messageConsumer = new DefaultMessageConsumer(
            ImmutableMap.of("myTag", consumerConfig),
            ImmutableMap.of("myFormat", serializer),
            ImmutableMap.of()
        );

        verifyException(() -> messageConsumer.start(), ConfigException.BadValue.class);
    }

    @Test
    public void testMissingSerializer() {
        final MessageConsumerConfig consumerConfig = new MessageConsumerConfig.Builder()
            .setChannel("myChannel").setSource("mySrc")
            .setFormat("myFormat").setGlobalId("myId").setTag("myTag")
            .build();
        final MessageConsumerPlugin consumerPlugin = mock(MessageConsumerPlugin.class);
        final MessageConsumer messageConsumer = new DefaultMessageConsumer(
            ImmutableMap.of("myTag", consumerConfig),
            ImmutableMap.of(),
            ImmutableMap.of("mySrc", consumerPlugin)
        );

        verifyException(() -> messageConsumer.start(), ConfigException.BadValue.class);
    }

    @Test
    public void testStop() {
        final MessageConsumerConfig consumerConfig = new MessageConsumerConfig.Builder()
            .setChannel("myChannel").setSource("mySrc")
            .setFormat("myFormat").setGlobalId("myId").setTag("myTag")
            .build();
        final ModelSerializer serializer = mock(ModelSerializer.class);
        final MessageConsumerPlugin consumerPlugin = mock(MessageConsumerPlugin.class);
        final MessageConsumer messageConsumer = new DefaultMessageConsumer(
            ImmutableMap.of("myTag", consumerConfig),
            ImmutableMap.of("myFormat", serializer),
            ImmutableMap.of("mySrc", consumerPlugin)
        );

        messageConsumer.stop();
        verify(consumerPlugin).stop(same(consumerConfig));
    }
}
