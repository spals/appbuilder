package net.spals.appbuilder.keystore.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;
import com.typesafe.config.Config;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.PBEByteEncryptor;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEByteEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;

import javax.annotation.PostConstruct;

/**
 * A {@link KeyStorePlugin} which stores a single password
 * that is used for Password Based Encryption.
 * <p>
 * The {@link PasswordKeyStorePlugin} is a very simple
 * {@link KeyStorePlugin} implementation which requires
 * minimal setup but possesses the highest risk.
 *
 * @author tkral
 */
@AutoBindInMap(baseClass = KeyStorePlugin.class, key = "password")
class PasswordKeyStorePlugin implements KeyStorePlugin {

    static KeyStore createPasswordKeyStore(final Config serviceConfig) {
        final PasswordKeyStorePlugin keyStore = new PasswordKeyStorePlugin();
        keyStore.createEncryptors(serviceConfig.getString("keyStore.password.pwd"));

        return keyStore;
    }

    @Configuration("keyStore.password.pwd")
    @VisibleForTesting
    volatile String password = null;

    private PBEByteEncryptor byteEncryptor;
    private PBEStringEncryptor stringEncryptor;

    @Inject
    PasswordKeyStorePlugin() {  }

    @PostConstruct
    @VisibleForTesting
    void postConstruct() {
        createEncryptors(password);
    }

    @VisibleForTesting
    void createEncryptors(String password) {
        if (password == null) {
            throw new RuntimeException();
        }

        final PooledPBEByteEncryptor pooledPBEByteEncryptor =
            new PooledPBEByteEncryptor();
        pooledPBEByteEncryptor.setProvider(new BouncyCastleProvider());
        pooledPBEByteEncryptor.setPasswordCharArray(password.toCharArray());
        pooledPBEByteEncryptor.setPoolSize(Runtime.getRuntime().availableProcessors());
        this.byteEncryptor = pooledPBEByteEncryptor;

        final PooledPBEStringEncryptor pooledStringEncryptor =
            new PooledPBEStringEncryptor();
        pooledStringEncryptor.setProvider(new BouncyCastleProvider());
        pooledStringEncryptor.setPasswordCharArray(password.toCharArray());
        pooledStringEncryptor.setPoolSize(Runtime.getRuntime().availableProcessors());
        this.stringEncryptor = pooledStringEncryptor;

        // Dereference the password so it will get garbage collected and moved
        // out of memory as an immutable String.
        this.password = null;
    }

    @Override
    public String decrypt(final String encryptedString) {
        return stringEncryptor.decrypt(encryptedString);
    }

    @Override
    public byte[] decryptBytes(final byte[] encryptedBytes) {
        return byteEncryptor.decrypt(encryptedBytes);
    }

    @Override
    public String encrypt(final String unencryptedString) {
        return stringEncryptor.encrypt(unencryptedString);
    }

    @Override
    public byte[] encryptBytes(final byte[] unencryptedBytes) {
        return byteEncryptor.encrypt(unencryptedBytes);
    }
}
