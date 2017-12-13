package net.spals.appbuilder.keystore.core;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link PasswordKeyStorePlugin}.
 *
 * @author tkral
 */
public class PasswordKeyStorePluginTest {

    @Test
    public void testCreatePasswordKeyStore() {
        final Config serviceConfig = ConfigFactory.parseMap(ImmutableMap.of("keyStore.password.pwd", "myPassword"));
        final KeyStore keyStore = PasswordKeyStorePlugin.createPasswordKeyStore(serviceConfig);

        assertThat(keyStore, notNullValue(KeyStore.class));
        assertThat(keyStore.encrypt(""), notNullValue(String.class));
        assertThat(keyStore.encryptBytes(new byte[0]), notNullValue(byte[].class));
    }

    @Test
    public void testCreatePasswordKeyStoreNoConfig() {
        catchException(() -> PasswordKeyStorePlugin.createPasswordKeyStore(ConfigFactory.empty()));
        assertThat(caughtException(), instanceOf(ConfigException.Missing.class));
    }

    @Test
    public void testPasswordCleanup() {
        final PasswordKeyStorePlugin keyStorePlugin = new PasswordKeyStorePlugin();
        keyStorePlugin.password = "myPassword";
        keyStorePlugin.postConstruct();

        assertThat(keyStorePlugin.password, nullValue(String.class));
    }

    @Test
    public void testEncryptNullString() {
        final PasswordKeyStorePlugin keyStorePlugin = new PasswordKeyStorePlugin();
        keyStorePlugin.password = "myPassword";
        keyStorePlugin.postConstruct();

        final String encrypted = keyStorePlugin.encrypt(null);
        assertThat(encrypted, nullValue(String.class));

        final String decrypted = keyStorePlugin.decrypt(encrypted);
        assertThat(decrypted, nullValue(String.class));
    }

    @DataProvider
    Object[][] encryptDecryptStringProvider() {
        return new Object[][] {
            // Case: Empty string
            {""},
            {"The quick brown cat"},
        };
    }

    @Test(dataProvider = "encryptDecryptStringProvider")
    public void testEncryptDecryptString(final String str) {
        final PasswordKeyStorePlugin keyStorePlugin = new PasswordKeyStorePlugin();
        keyStorePlugin.password = "myPassword";
        keyStorePlugin.postConstruct();

        final String encrypted = keyStorePlugin.encrypt(str);
        assertThat(encrypted, not(is(str)));

        final String decrypted = keyStorePlugin.decrypt(encrypted);
        assertThat(decrypted, is(str));
    }

    @DataProvider
    Object[][] encryptDecryptBytesProvider() {
        return new Object[][] {
            // Empty byte array
            {new byte[0]},
            // Case: Zeroed byte array
            {new byte[]{0, 0, 0}},
            {new byte[]{1, 2, 3}},
        };
    }

    @Test(dataProvider = "encryptDecryptBytesProvider")
    public void testEncryptDecryptBytes(final byte[] bytes) {
        final PasswordKeyStorePlugin keyStorePlugin = new PasswordKeyStorePlugin();
        keyStorePlugin.password = "myPassword";
        keyStorePlugin.postConstruct();

        final byte[] encrypted = keyStorePlugin.encryptBytes(bytes);
        assertThat(encrypted, is(not(bytes)));

        final byte[] decrypted = keyStorePlugin.decryptBytes(encrypted);
        assertThat(decrypted, is(bytes));
    }
}
