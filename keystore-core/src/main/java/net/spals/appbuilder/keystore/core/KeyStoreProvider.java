package net.spals.appbuilder.keystore.core;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.netflix.governator.annotations.Configuration;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindProvider;

import java.util.Map;
import java.util.Optional;

/**
 * A {@link Provider} which provides a {@link KeyStore}.
 *
 * @author tkral
 */
@AutoBindProvider
public class KeyStoreProvider implements Provider<KeyStore> {

    /**
     * Creates a {@link KeyStore} outside of dependency injection.
     * <p>
     * This can be used for use cases that fall outside of an
     * application's dependency graph. In particular, this is useful
     * for configuration encryption.
     */
    public static KeyStore createKeyStore(final Config serviceConfig) {
        final String storeSystem = serviceConfig.getString("keyStore.system");

        if ("password".equals(storeSystem)) {
            return PasswordKeyStorePlugin.createPasswordKeyStore(serviceConfig);
        }

        throw new ConfigException.BadValue("keyStore.system",
            "No Key Store plugin found for : " + storeSystem);
    }

    @Configuration("keyStore.system")
    private volatile String storeSystem;

    private final Map<String, KeyStorePlugin> storePluginMap;

    @Inject
    KeyStoreProvider(final Map<String, KeyStorePlugin> storePluginMap) {
        this.storePluginMap = storePluginMap;
    }

    @Override
    public KeyStore get() {
        final KeyStorePlugin storePlugin = Optional.ofNullable(storePluginMap.get(storeSystem))
            .orElseThrow(() -> new ConfigException.BadValue("keyStore.system",
                "No Key Store plugin found for : " + storeSystem));

        return storePlugin;
    }
}
