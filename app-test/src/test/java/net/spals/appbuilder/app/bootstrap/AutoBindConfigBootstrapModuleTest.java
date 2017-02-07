package net.spals.appbuilder.app.bootstrap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.spals.appbuilder.config.ConsumerConfig;
import net.spals.appbuilder.config.ProducerConfig;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link AutoBindConfigBootstrapModule}
 *
 * @author tkral
 */
public class AutoBindConfigBootstrapModuleTest {

    @DataProvider
    Object[][] parseConfigsProvider() {
        final Map<String, Object> configMapConsumer = ImmutableMap.of("myTag.consumer.channel", "myChannel",
                "myTag.consumer.globalId", "myId",
                "myTag.consumer.source", "kafka");
        final ConsumerConfig expectedConsumerConfig = new ConsumerConfig.Builder().setTag("myTag")
                .setChannel("myChannel")
                .setGlobalId("myId")
                .setSource("kafka").build();

        final Map<String, Object> configMapProducer = ImmutableMap.of("myTag.producer.channel", "myChannel",
                "myTag.producer.globalId", "myId",
                "myTag.producer.destination", "kafka");
        final ProducerConfig expectedProducerConfig = new ProducerConfig.Builder().setTag("myTag")
                .setChannel("myChannel")
                .setGlobalId("myId")
                .setDestination("kafka").build();

        final Map<String, Object> configMapMultiConsumer = ImmutableMap.<String, Object>builder()
                .put("myTag1.consumer.channel", "myChannel")
                .put("myTag1.consumer.globalId", "myId1")
                .put("myTag1.consumer.source", "kafka")
                .put("myTag2.consumer.channel", "myChannel")
                .put("myTag2.consumer.globalId", "myId2")
                .put("myTag2.consumer.source", "kafka")
                .build();
        final ConsumerConfig expectedConsumerConfig1 = new ConsumerConfig.Builder().setTag("myTag1")
                .setChannel("myChannel")
                .setGlobalId("myId1")
                .setSource("kafka").build();
        final ConsumerConfig expectedConsumerConfig2 = new ConsumerConfig.Builder().setTag("myTag2")
                .setChannel("myChannel")
                .setGlobalId("myId2")
                .setSource("kafka").build();

        return new Object[][] {
                // Case: No configs found for auto-binding
                {ConfigFactory.empty(), "consumer", ConsumerConfig.class, Collections.emptyMap()},
                {ConfigFactory.empty(), "producer", ProducerConfig.class, Collections.emptyMap()},
                // Basic cases
                {ConfigFactory.parseMap(configMapConsumer), "consumer", ConsumerConfig.class,
                        ImmutableMap.of("myTag", expectedConsumerConfig)},
                {ConfigFactory.parseMap(configMapProducer), "producer", ProducerConfig.class,
                        ImmutableMap.of("myTag", expectedProducerConfig)},
                // Case: Multiple auto-bound configurations
                {ConfigFactory.parseMap(configMapMultiConsumer), "consumer", ConsumerConfig.class,
                        ImmutableMap.of("myTag1", expectedConsumerConfig1, "myTag2", expectedConsumerConfig2)},
        };
    }

    @Test(dataProvider = "parseConfigsProvider")
    public <T> void testParseConfigs(final Config serviceConfig,
                                     final String configSubTag,
                                     final Class<T> configType,
                                     final Map<String, T> expectedConfigMap) {
        final AutoBindConfigBootstrapModule autoBindBootstrapModule = new AutoBindConfigBootstrapModule(serviceConfig);
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
        final AutoBindConfigBootstrapModule autoBindBootstrapModule = new AutoBindConfigBootstrapModule(serviceConfig);
        assertThat(autoBindBootstrapModule.parseTags(configSubTag), is(expectedParsedTags));
    }
}
