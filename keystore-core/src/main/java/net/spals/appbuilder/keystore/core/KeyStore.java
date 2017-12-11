package net.spals.appbuilder.keystore.core;

/**
 * A store which contains keys for encrypting
 * and decrypting data.
 * <p>
 * *NOTE*: The semantics of any implementation
 * of {@link KeyStore} represent a bi-directional
 * encryption. This is generally useful for the
 * following use cases:
 * <p>
 * + Encrypting sensitive configuration used
 *   for third party services (e.g. passwords
 *   or secret keys).
 * <p>
 * + Encrypting application-level data for
 *   storage WITH THE EXCEPTION OF USER
 *   AUTHENTICATION PASSWORDS
 *   (e.g. credit card information)
 * <p>
 * A {@link KeyStore} *SHOULD NOT* be used
 * to encrypt passwords which are used for
 * user authentication in your application.
 * Instead you should use a digest (like BCrypt).
 *
 * @author tkral
 */
public interface KeyStore {

    /**
     * Decrypts the given {@link String}. This
     * {@link String} should have previously
     * been encrypted by {@link #encrypt(String)}.
     *
     * @return If encrypted, return the given {@link String}
     * in its plaintext form. If not encrypted, returns
     * an unknown value.
     */
    String decrypt(String encryptedString);

    /**
     * Decrypts the given byte array. This
     * byte array should have previously
     * been encrypted by {@link #encryptBytes(byte[])}.
     *
     * @return If encrypted, return the given byte array
     * in its plain, unencrypted form. If not encrypted, returns
     * an unknown value.
     */
    byte[] decryptBytes(byte[] encryptedBytes);

    /**
     * Encrypts the given {@link String}. This
     * {@link String} should be in its original,
     * plaintext form.
     *
     * @return An encrypted form of the {@link String}.
     */
    String encrypt(String unencryptedString);

    /**
     * Encrypts the given byte array. This
     * byte array should be in its original,
     * form.
     *
     * @return An encrypted form of the byte array.
     */
    byte[] encryptBytes(byte[] unencryptedBytes);
}
