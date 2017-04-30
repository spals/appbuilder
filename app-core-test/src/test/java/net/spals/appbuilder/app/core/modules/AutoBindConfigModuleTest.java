package net.spals.appbuilder.app.core.modules;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.spals.appbuilder.config.TaggedConfig;
import net.spals.appbuilder.config.message.MessageConsumerConfig;
import net.spals.appbuilder.config.message.MessageProducerConfig;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link AutoBindConfigModule}
 *
 * @author tkral
 */
public class AutoBindConfigModuleTest {

    @DataProvider
    Object[][] parseConfigsProvider() {
        final Map<String, Object> configMapConsumer = ImmutableMap.of("myTag.consumer.channel", "myChannel",
                "myTag.consumer.format", "json",
                "myTag.consumer.globalId", "myId",
                "myTag.consumer.source", "kafka");
        final MessageConsumerConfig expectedConsumerConfig = new MessageConsumerConfig.Builder().setTag("myTag")
                .setChannel("myChannel")
                .setFormat("json")
                .setGlobalId("myId")
                .setSource("kafka").build();

        final Map<String, Object> configMapProducer = ImmutableMap.of("myTag.producer.channel", "myChannel",
                "myTag.producer.format", "json",
                "myTag.producer.globalId", "myId",
                "myTag.producer.destination", "kafka");
        final MessageProducerConfig expectedProducerConfig = new MessageProducerConfig.Builder().setTag("myTag")
                .setChannel("myChannel")
                .setFormat("json")
                .setGlobalId("myId")
                .setDestination("kafka").build();

        final Map<String, Object> configMapMultiConsumer = ImmutableMap.<String, Object>builder()
                .put("myTag1.consumer.channel", "myChannel")
                .put("myTag1.consumer.format", "json")
                .put("myTag1.consumer.globalId", "myId1")
                .put("myTag1.consumer.source", "kafka")
                .put("myTag2.consumer.channel", "myChannel")
                .put("myTag2.consumer.format", "json")
                .put("myTag2.consumer.globalId", "myId2")
                .put("myTag2.consumer.source", "kafka")
                .build();
        final MessageConsumerConfig expectedConsumerConfig1 = new MessageConsumerConfig.Builder().setTag("myTag1")
                .setChannel("myChannel")
                .setFormat("json")
                .setGlobalId("myId1")
                .setSource("kafka").build();
        final MessageConsumerConfig expectedConsumerConfig2 = new MessageConsumerConfig.Builder().setTag("myTag2")
                .setChannel("myChannel")
                .setFormat("json")
                .setGlobalId("myId2")
                .setSource("kafka").build();

        return new Object[][] {
                // Case: No configs found for auto-binding
                {ConfigFactory.empty(), "consumer", MessageConsumerConfig.class, Collections.emptyMap()},
                {ConfigFactory.empty(), "producer", MessageProducerConfig.class, Collections.emptyMap()},
                // Basic cases
                {ConfigFactory.parseMap(configMapConsumer), "consumer", MessageConsumerConfig.class,
                        ImmutableMap.of("myTag", expectedConsumerConfig)},
                {ConfigFactory.parseMap(configMapProducer), "producer", MessageProducerConfig.class,
                        ImmutableMap.of("myTag", expectedProducerConfig)},
                // Case: Multiple auto-bound configurations
                {ConfigFactory.parseMap(configMapMultiConsumer), "consumer", MessageConsumerConfig.class,
                        ImmutableMap.of("myTag1", expectedConsumerConfig1, "myTag2", expectedConsumerConfig2)},
        };
    }

    @Test(dataProvider = "parseConfigsProvider")
    public <T extends TaggedConfig> void testParseConfigs(final Config serviceConfig,
                                                          final String configSubTag,
                                                          final Class<T> configType,
                                                          final Map<String, T> expectedConfigMap) {
        final AutoBindConfigModule autoBindBootstrapModule =
                new AutoBindConfigModule.Builder().setServiceConfig(serviceConfig).buildPartial();
        assertThat(autoBindBootstrapModule.parseConfigs(configSubTag, configType), is(expectedConfigMap));
    }

    @DataProvider
    Object[][] parseTagsProvider() {
        return new Object[][] {
                // Empty case
                {ConfigFactory.empty(), "consumer", Collections.emptySet()},
                // Basic case
                {ConfigFactory.parseMap(ImmutableMap.of("myTag.consumer.channel", "myChannel")),
                        "consumer", ImmutableSet.of("myTag")},
                // Case: Parse multiple tags
                {ConfigFactory.parseMap(ImmutableMap.of("myTag1.consumer.channel", "myChannel", "myTag2.consumer.channel", "myChannel")),
                        "consumer", ImmutableSet.of("myTag1", "myTag2")},
                // Case: Skip tags not matching the subtag
                {ConfigFactory.parseMap(ImmutableMap.of("myTag.consumer.channel", "myChannel")),
                        "producer", Collections.emptySet()},
        };
    }

    @Test(dataProvider = "parseTagsProvider")
    public void testParseTags(final Config serviceConfig,
                              final String configSubTag,
                              final Set<String> expectedParsedTags) {
        final AutoBindConfigModule autoBindBootstrapModule =
                new AutoBindConfigModule.Builder().setServiceConfig(serviceConfig).buildPartial();
        assertThat(autoBindBootstrapModule.parseTags(configSubTag), is(expectedParsedTags));
    }
}
