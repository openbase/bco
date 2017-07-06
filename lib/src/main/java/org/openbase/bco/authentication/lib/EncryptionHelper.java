package org.openbase.bco.authentication.lib;

import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.openbase.jul.exception.FatalImplementationErrorException;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 openbase.org
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

    private static final String ASYMMETRIC_ALGORITHM = "RSA";
//    private static final String ASYMMETRIC_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String ASYMMETRIC_TRANSFORMATION = ASYMMETRIC_ALGORITHM;
    private static final int ASYMMETRIC_KEY_LENGTH = 1024;
    
    private static final String SYMMETRIC_ALGORITHM = "AES";
//    private static final String SYMMETRIC_TRANSFORMATION = "AES/CBC/PKCS5Padding";
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
            new FatalImplementationErrorException("Key transformation non existent", SYMMETRIC_ALGORITHM, ex);
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
            new FatalImplementationErrorException("Key transformation non existent", ASYMMETRIC_TRANSFORMATION, ex);
            return null;
        }
    }

    /**
     * Hashes a string symmetrically.
     * str must be in UTF-8 encoding
     *
     * @param str String to be hashed
     * @return Returns a byte[] representing the hashed string
     */
    public static byte[] hash(String str) {
        try {
            byte[] key = str.getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance(HASH_ALGORITHM);
            key = sha.digest(key);
            return Arrays.copyOf(key, 16);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            new FatalImplementationErrorException("Hashing[" + str + "] failed!", HASH_ALGORITHM, ex);
            return null;
        }
    }

    /**
     * Encrypts any Object into a ByteString using a symmetric key
     *
     * @param obj Object to be encrypted
     * @param key byte[] to encrypt obj with
     * @param initializingVector a byteArray of 16 bytes used for Cipher Block Chaining
     * @return Returns encrypted object as ByteString
     * @throws IOException Any IO error occurring during the serialization and
     * encryption.
     */
    public static ByteString encrypt(Serializable obj, byte[] key) throws IOException {
        ByteArrayOutputStream baos = null;
        ObjectOutput out = null;
        try {
            // specify key
            SecretKeySpec sks = new SecretKeySpec(key, SYMMETRIC_ALGORITHM);

            // create cipher
//            IvParameterSpec iv = new IvParameterSpec(initializingVector);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, sks);

            // cipher
            baos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(baos);
            out.writeObject(obj);
            out.flush();
            return ByteString.copyFrom(cipher.doFinal(baos.toByteArray()));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | InvalidKeyException | BadPaddingException ex) {
            new FatalImplementationErrorException("Key transformation non existent", SYMMETRIC_TRANSFORMATION, ex);
            return null;
        } finally {
            if (baos != null) baos.close();
            if (out != null) out.close();
        }
    }

    /**
     * Decrypts a ByteString into an Object using a symmetric key
     *
     * @param bstr ByteString to be decrypted
     * @param key byte[] to decrypt bstr with
     * @param initializingVector a byteArray of 16 bytes used for Cipher Block Chaining
     * @return Returns decrypted object as Object
     * @throws StreamCorruptedException If the decryption fails, because of
     * corrupted data or an invalid key.
     * @throws IOException Any other I/O failure.
     */
    public static Object decrypt(ByteString bstr, byte[] key) throws StreamCorruptedException, IOException {
        ByteArrayInputStream bais = null;
        ObjectInput in = null;
        try {
            // specify key
            SecretKeySpec sks = new SecretKeySpec(key, SYMMETRIC_ALGORITHM);

            // create cipher
//            IvParameterSpec iv = new IvParameterSpec(initializingVector);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, sks);

            // decipher
            bais = new ByteArrayInputStream(cipher.doFinal(bstr.toByteArray()));
            in = new ObjectInputStream(bais);
            return in.readObject();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | ClassNotFoundException | InvalidKeyException ex) {
            new FatalImplementationErrorException("Decryption failed.", SYMMETRIC_TRANSFORMATION, ex);
            return null;
        } finally {
            if (bais != null) bais.close();
            if (in != null) in.close();
        }
    }

    /**
     * Encrypts any Object into a ByteString using a symmetric key
     *
     * @param obj Object to be encrypted
     * @param key byte[] to encrypt obj with
     * @return Returns encrypted object as ByteString
     * @throws IOException Any IO error occurring during the serialization and
     * encryption.
     */
    public static ByteString encryptAsymmetric(Serializable obj, byte[] key) throws IOException {
        ByteArrayOutputStream baos = null;
        ObjectOutput out = null;
        try {
            // generate public key from byte[] key
            PublicKey publicKey = KeyFactory.getInstance(ASYMMETRIC_TRANSFORMATION).generatePublic(new X509EncodedKeySpec(key));
            
            // create cipher
            Cipher cipher = Cipher.getInstance(ASYMMETRIC_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            
            baos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(baos);
            out.writeObject(obj);
            out.flush();
            return ByteString.copyFrom(cipher.doFinal(baos.toByteArray()));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | InvalidKeyException | InvalidKeySpecException ex) {
            new FatalImplementationErrorException("Key transformation non existent", ASYMMETRIC_TRANSFORMATION, ex);
            return null;
        } catch (BadPaddingException ex) {
            throw new IOException(ex);
        } finally {
            if (baos != null) baos.close();
            if (out != null) out.close();
        }
    }

    /**
     * Decrypts a ByteString into an Object using a symmetric key
     *
     * @param bstr ByteString to be decrypted
     * @param key byte[] to decrypt bstr with
     * @return Returns decrypted object as Object
     * @throws StreamCorruptedException If the decryption fails, because of
     * corrupted data or an invalid key.
     * @throws IOException Any other I/O failure.
     */
    public static Object decryptAsymmetric(ByteString bstr, byte[] key) throws StreamCorruptedException, IOException {
        ByteArrayInputStream bais = null;
        ObjectInput in = null;
        try {
            // generate private key from byte[] key
            PrivateKey privateKey = KeyFactory.getInstance(ASYMMETRIC_TRANSFORMATION).generatePrivate(new PKCS8EncodedKeySpec(key));
            
            // create cipher
            Cipher cipher = Cipher.getInstance(ASYMMETRIC_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // decipher
            bais = new ByteArrayInputStream(cipher.doFinal(bstr.toByteArray()));
            in = new ObjectInputStream(bais);
            return in.readObject();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | InvalidKeySpecException | ClassNotFoundException ex) {
            new FatalImplementationErrorException("Decryption failed.", ASYMMETRIC_TRANSFORMATION, ex);
            return null;
        } finally {
            if (bais != null) bais.close();
            if (in != null) in.close();
        }
    }
    
    /**
     * Creates an initializing vector used for Cipher Block Chaining.
     * Use this same vector for encryption and decryption
     * @return byteArray of length 16
     */
    public static byte[] createCiptherBlockChainingVector() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
