package net.spals.appbuilder.keystore.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;
import com.typesafe.config.Config;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import org.jasypt.encryption.pbe.PBEByteEncryptor;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEByteEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;

import javax.annotation.PostConstruct;
import javax.validation.ValidationException;

/**
 * A {@link KeyStorePlugin} which stores a single password
 * that is used for Password Based Encryption.
 * <p>
 * The {@link PasswordKeyStorePlugin} is a very simple
 * {@link KeyStorePlugin} implementation which requires
 * minimal setup but possesses the highest risk.
 * <p>
 * Note that this implementation uses the default JVM
 * security provider. This means that it will work out-of-the-box
 * without extra work on the part of the application developer
 * (e.g. installing the Java Cryptography Extension). But
 * this also means that this provides only low end security.
 * <p>
 * Applications which have more serious security concerns
 * should consider using a different implementation.
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
            throw new ValidationException(
                "may not be null - " + this.getClass().getName() + ".password = null"
            );
        }

        final PooledPBEByteEncryptor pooledPBEByteEncryptor =
            new PooledPBEByteEncryptor();
        pooledPBEByteEncryptor.setPasswordCharArray(password.toCharArray());
        pooledPBEByteEncryptor.setPoolSize(Runtime.getRuntime().availableProcessors());
        this.byteEncryptor = pooledPBEByteEncryptor;

        final PooledPBEStringEncryptor pooledStringEncryptor =
            new PooledPBEStringEncryptor();
        pooledStringEncryptor.setPasswordCharArray(password.toCharArray());
        pooledStringEncryptor.setPoolSize(Runtime.getRuntime().availableProcessors());
        this.stringEncryptor = pooledStringEncryptor;

        // Dereference the password so it will get garbage collected and moved
        // out of memory as an immutable String.
        this.password = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String decrypt(final String encryptedString) {
        return stringEncryptor.decrypt(encryptedString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] decryptBytes(final byte[] encryptedBytes) {
        return byteEncryptor.decrypt(encryptedBytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String encrypt(final String unencryptedString) {
        return stringEncryptor.encrypt(unencryptedString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] encryptBytes(final byte[] unencryptedBytes) {
        return byteEncryptor.encrypt(unencryptedBytes);
    }
}
