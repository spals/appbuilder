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

    String decrypt(String encryptedString);

    byte[] decryptBytes(byte[] encryptedBytes);

    String encrypt(String unencryptedString);

    byte[] encryptBytes(byte[] unencryptedBytes);
}
