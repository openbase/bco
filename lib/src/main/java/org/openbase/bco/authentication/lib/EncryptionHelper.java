package org.openbase.bco.authentication.lib;

import com.google.protobuf.ByteString;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

/**
 * A key that is used to encrypt and decrypt tickets during Kerberos
 * authentication
 *
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class EncryptionHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionHelper.class);

    private static final String ASYMMETRIC_ALGORITHM = "RSA";
    private static final String ASYMMETRIC_TRANSFORMATION = ASYMMETRIC_ALGORITHM;
    private static final int ASYMMETRIC_KEY_LENGTH = 1024;

    private static final String SYMMETRIC_ALGORITHM = "AES";
    private static final String SYMMETRIC_TRANSFORMATION = SYMMETRIC_ALGORITHM;
    private static final int SYMMETRIC_KEY_LENGTH = 128;

    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Generate a key with given transformation and key length which can then be used
     * for symmetric en- or decryption.
     *
     * @return the generated key as a byte array
     */
    public static byte[] generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
            keyGenerator.init(SYMMETRIC_KEY_LENGTH);
            SecretKey secretKey = keyGenerator.generateKey();
            return secretKey.getEncoded();
        } catch (NoSuchAlgorithmException ex) {
            ExceptionPrinter.printHistory(new FatalImplementationErrorException("Key transformation non existent", EncryptionHelper.class, ex), LOGGER);
            return null;
        }
    }

    /**
     * Generate a key pair given transformation and key length which can then be used
     * for asymmetric en- or decryption.
     *
     * @return the generated key as a byte array
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ASYMMETRIC_TRANSFORMATION);
            keyPairGenerator.initialize(ASYMMETRIC_KEY_LENGTH);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            ExceptionPrinter.printHistory(new FatalImplementationErrorException("Key transformation non existent", EncryptionHelper.class, ex), LOGGER);
            return null;
        }
    }

    /**
     * Hashes a string that has to be UTF-8 encoded symmetrically.
     *
     * @param string String to be hashed
     *
     * @return Returns a byte[] representing the hashed string
     */
    public static byte[] hash(final String string) {
        try {
            // NOTE: the charset has to match the copy of method below, see: https://github.com/openbase/jul/issues/56
            byte[] key = string.getBytes(StandardCharsets.UTF_16);
            MessageDigest sha = MessageDigest.getInstance(HASH_ALGORITHM);
            key = sha.digest(key);
            // length of byte array has to match the symmetric key length for the symmetric algorithm
            return Arrays.copyOf(key, SYMMETRIC_KEY_LENGTH / 8);
        } catch (NoSuchAlgorithmException ex) {
            ExceptionPrinter.printHistory(new FatalImplementationErrorException("Hashing[" + string + "] failed!", EncryptionHelper.class, ex), LOGGER);
            return null;
        }
    }

    /**
     * Encrypts any Object into a ByteString using a symmetric key.
     *
     * @param object Object to be encrypted
     * @param key    byte[] to encrypt object with
     *
     * @return Returns encrypted object as ByteString
     *
     * @throws CouldNotPerformException if encrypting fails encryption.
     */
    public static ByteString encryptSymmetric(final Serializable object, final byte[] key) throws CouldNotPerformException {
        return ByteString.copyFrom(encrypt(object, key, true));
    }

    /**
     * Encrypts any Object into a ByteString using a symmetric key
     *
     * @param object Object to be encrypted
     * @param key    byte[] to encrypt object with
     *
     * @return Returns encrypted object as ByteString
     *
     * @throws CouldNotPerformException if encrypting fails encryption.
     */
    public static ByteString encryptAsymmetric(final Serializable object, final byte[] key) throws CouldNotPerformException {
        return ByteString.copyFrom(encrypt(object, key, false));
    }

    /**
     * Encrypt an object into a byte array.
     *
     * @param object           the object which is encrypted.
     * @param loginCredentials type containing the credentials and a flag if symmetric or asymmetric encryption should
     *                         be used.
     *
     * @throws CouldNotPerformException if encryption fails.
     */
    public static byte[] encrypt(final Serializable object, final LoginCredentials loginCredentials) throws CouldNotPerformException {
        return encrypt(object, loginCredentials.getCredentials().toByteArray(), loginCredentials.getSymmetric());
    }

    /**
     * Encrypts any Object into a ByteString.
     *
     * @param object    Object to be encrypted
     * @param key       byte[] to encrypt object with
     * @param symmetric if the encryption should use a symmetric or asymmetric key
     *
     * @return Returns encrypted object as ByteString
     *
     * @throws CouldNotPerformException if encrypting fails encryption.
     */
    public static byte[] encrypt(final Serializable object, final byte[] key, final boolean symmetric) throws CouldNotPerformException {
        try {
            Key keyType;
            Cipher cipher;
            // specify key and generate cipher
            if (symmetric) {
                keyType = new SecretKeySpec(key, SYMMETRIC_ALGORITHM);
                cipher = Cipher.getInstance(SYMMETRIC_TRANSFORMATION);
            } else {
                keyType = KeyFactory.getInstance(ASYMMETRIC_TRANSFORMATION).generatePublic(new X509EncodedKeySpec(key));
                cipher = Cipher.getInstance(ASYMMETRIC_TRANSFORMATION);
            }
            cipher.init(Cipher.ENCRYPT_MODE, keyType);

            // cipher
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                    objectOutputStream.writeObject(object);
                    objectOutputStream.flush();
                    return cipher.doFinal(byteArrayOutputStream.toByteArray());
                }
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | InvalidKeyException | BadPaddingException | InvalidKeySpecException ex) {
            throw new FatalImplementationErrorException("Unable to encrypt object[" + object + "]", EncryptionHelper.class, ex);
        } catch (IOException ex) {
            throw new CouldNotPerformException("Could not encrypt object[" + object + "]", ex);
        }
    }

    /**
     * Decrypt an object with a symmetric key and directly cast it to type T.
     *
     * @param <T>             the type to which the encrypted object is casted
     * @param encryptedObject the object encrypted as a ByteString
     * @param key             the key used to decrypt the object
     * @param encryptedClass  the class to which the decrypted object is cast
     *
     * @return the decrypted object cast as T
     *
     * @throws CouldNotPerformException if the byte array could not be decrypted using the given key or is not an instance of the given class
     */
    public static <T> T decryptSymmetric(final ByteString encryptedObject, final byte[] key, final Class<T> encryptedClass) throws CouldNotPerformException {
        return decrypt(encryptedObject, key, encryptedClass, true);
    }

    /**
     * Decrypt an object using an asymmetric key and directly cast it to type T.
     *
     * @param <T>             the type to which the encrypted object is casted
     * @param encryptedObject the object encrypted as a ByteString
     * @param key             the key used to decrypt the object
     * @param encryptedClass  the class to which the decrypted object is cast
     *
     * @return the decrypted object cast as T
     *
     * @throws CouldNotPerformException if the byte array could not be decrypted using the given key or is not an instance of the given class
     */
    public static <T> T decryptAsymmetric(final ByteString encryptedObject, final byte[] key, final Class<T> encryptedClass) throws CouldNotPerformException {
        return decrypt(encryptedObject, key, encryptedClass, false);
    }

    /**
     * Decrypt a ByteString into an object of type T.
     *
     * @param encryptedObject  byte array to be decrypted.
     * @param loginCredentials type containing the credentials and if symmetric or asymmetric encryption needs to be used.
     * @param encryptedClass   the class to which the decrypted object is cast
     * @param <T>              the type to which the encrypted object is casted.
     *
     * @return the decrypted object cast to class T.
     *
     * @throws CouldNotPerformException if the byte array could not be decrypted using the given key or is not an instance of the given class.
     */
    public static <T> T decrypt(final byte[] encryptedObject, final LoginCredentials loginCredentials, final Class<T> encryptedClass) throws CouldNotPerformException {
        return decrypt(encryptedObject, loginCredentials.getCredentials().toByteArray(), encryptedClass, loginCredentials.getSymmetric());
    }

    /**
     * Decrypts a ByteString into an Object of type T.
     *
     * @param <T>             the type to which the encrypted object is casted
     * @param encryptedObject ByteString to be decrypted
     * @param key             byte[] to decrypt the encrypted object with
     * @param encryptedClass  the class to which the decrypted object is cast
     * @param symmetric       if the key is symmetric or asymmetric
     *
     * @return Returns decrypted object as Object
     *
     * @throws CouldNotPerformException if the byte array could not be decrypted using the given key or is not an instance of the given class
     */
    public static <T> T decrypt(final ByteString encryptedObject, final byte[] key, final Class<T> encryptedClass, final boolean symmetric) throws CouldNotPerformException {
        return decrypt(encryptedObject.toByteArray(), key, encryptedClass, symmetric);
    }

    /**
     * Decrypts a ByteArray into an Object of type T.
     *
     * @param <T>             the type to which the encrypted object is casted
     * @param encryptedObject ByteString to be decrypted
     * @param key             byte[] to decrypt the encrypted object with
     * @param encryptedClass  the class to which the decrypted object is cast
     * @param symmetric       if the key is symmetric or asymmetric
     *
     * @return Returns decrypted object as Object
     *
     * @throws CouldNotPerformException if the byte array could not be decrypted using the given key or is not an instance of the given class
     */
    public static <T> T decrypt(final byte[] encryptedObject, final byte[] key, final Class<T> encryptedClass, final boolean symmetric) throws CouldNotPerformException {
        try {
            Key keyType;
            Cipher cipher;
            // specify key and generate cipher
            if (symmetric) {
                keyType = new SecretKeySpec(key, SYMMETRIC_ALGORITHM);
                cipher = Cipher.getInstance(SYMMETRIC_TRANSFORMATION);
            } else {
                keyType = KeyFactory.getInstance(ASYMMETRIC_TRANSFORMATION).generatePrivate(new PKCS8EncodedKeySpec(key));
                cipher = Cipher.getInstance(ASYMMETRIC_TRANSFORMATION);
            }
            cipher.init(Cipher.DECRYPT_MODE, keyType);
            byte[] decrypted = cipher.doFinal(encryptedObject);

            // decipher
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decrypted)) {
                try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
                    return (T) objectInputStream.readObject();
                }
            }
        } catch (NoSuchAlgorithmException | ClassNotFoundException | NoSuchPaddingException | IllegalBlockSizeException | InvalidKeySpecException ex) {
            throw new FatalImplementationErrorException("Decryption of [" + encryptedObject + "] failed", EncryptionHelper.class, ex);
        } catch (IOException | BadPaddingException | ClassCastException | InvalidKeyException ex) {
            throw new CouldNotPerformException("Could not decrypt byte array to object of type[" + encryptedClass.getName() + "]", ex);
        }
    }

    /**
     * Creates an initialized vector used for Cipher Block Chaining.
     * Use this same vector for encryption and decryption
     *
     * @return byteArray of length 16
     */
    public static byte[] createCipherBlockChainingVector() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
