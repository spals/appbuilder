package net.spals.appbuilder.message.core.producer;

import com.google.common.collect.ImmutableMap;
import net.spals.appbuilder.config.message.MessageProducerConfig;
import net.spals.appbuilder.message.core.MessageProducer;
import net.spals.appbuilder.model.core.ModelSerializer;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultMessageProducer}
 *
 * @author tkral
 */
public class DefaultMessageProducerTest {

    @Test
    public void testSendMessage() throws IOException {
        final MessageProducerConfig producerConfig = new MessageProducerConfig.Builder()
                .setChannel("myChannel").setDestination("myDest")
                .setFormat("myFormat").setGlobalId("myId").setTag("myTag")
                .build();
        final ModelSerializer modelSerializer = mock(ModelSerializer.class);
        when(modelSerializer.serialize(any())).thenReturn("abc".getBytes());
        final MessageProducerPlugin producerPlugin = mock(MessageProducerPlugin.class);

        final DefaultMessageProducer messageProducer = new DefaultMessageProducer(
                ImmutableMap.of("myTag", producerConfig),
                ImmutableMap.of("myFormat", modelSerializer),
                ImmutableMap.of("myDest", producerPlugin)
        );

        messageProducer.sendMessage("myTag", "abc");
        verify(producerPlugin).sendMessage(same(producerConfig), eq("abc".getBytes()));
    }
}
