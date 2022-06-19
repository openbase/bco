package org.openbase.bco.authentication.test;

/*-
 * #%L
 * BCO Authentication Test
 * %%
 * Copyright (C) 2017 - 2021 openbase.org
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

import static org.junit.jupiter.api.Assertions.*;
import com.google.protobuf.ByteString;
import java.security.KeyPair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.junit.jupiter.api.Test;

import org.openbase.jul.exception.CouldNotPerformException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class EncryptionHelperTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EncryptionHelperTest.class);

    public EncryptionHelperTest() {
    }

    @Test
    @Timeout(20)
    public void testGenerateKey() {
        LOGGER.info("test key generation");
        int expLen = 16;
        int len = EncryptionHelper.generateKey().length;
        assertEquals(expLen, len);
    }

    @Test
    public void testSymmetricHashing() {
        LOGGER.info("test if hashing method hashes symmetrically");
        byte[] hash1 = EncryptionHelper.hash("test");
        byte[] hash2 = EncryptionHelper.hash("test");
        assertArrayEquals(hash1, hash2);
    }

    @Test
    @Timeout(20)
    public void testSymmetricEncryptionDecryption() throws Exception {
        LOGGER.info("test symmetric encryption and decryption");
        String str = "test";
        byte[] key = EncryptionHelper.generateKey();
        ByteString encrypted = EncryptionHelper.encryptSymmetric(str, key);
        String decrypted = EncryptionHelper.decryptSymmetric(encrypted, key, String.class);
        assertEquals(str, decrypted);
    }

    @Test
    @Timeout(20)
    public void testAsymmetricEncryptionDecryption() throws Exception {
        LOGGER.info("test asymmetric encryption and decryption");
        String str = "test";
        KeyPair keyPair = EncryptionHelper.generateKeyPair();
        ByteString encrypted = EncryptionHelper.encryptAsymmetric(str, keyPair.getPublic().getEncoded());
        String decrypted = EncryptionHelper.decryptAsymmetric(encrypted, keyPair.getPrivate().getEncoded(), String.class);
        assertEquals(str, decrypted);
    }

    @Test
    public void testExceptionsWithWrongKeySymmetric() {
        LOGGER.info("testExceptionsWithWrongKey");

        byte[] correctPassword = EncryptionHelper.hash("123654");
        byte[] wrongPassword = EncryptionHelper.hash("654321");

        String value = "This String should be encrypted";

        Assertions.assertThrows(CouldNotPerformException.class, () -> {
            ByteString encryptedValue = EncryptionHelper.encryptSymmetric(value, correctPassword);
            EncryptionHelper.decryptSymmetric(encryptedValue, wrongPassword, String.class);
        });
    }

    @Test
    public void testExceptionsWithWrongKeyAsymmetric() {
        LOGGER.info("testExceptionsWithWrongKeyAsymmetric");

        KeyPair correctKeyPair = EncryptionHelper.generateKeyPair();
        KeyPair wrongKeyPair = EncryptionHelper.generateKeyPair();

        String value = "This String should be encrypted";
        Assertions.assertThrows(CouldNotPerformException.class, () -> {
            ByteString encryptedValue = EncryptionHelper.encryptAsymmetric(value, correctKeyPair.getPublic().getEncoded());

            EncryptionHelper.decryptAsymmetric(encryptedValue, wrongKeyPair.getPrivate().getEncoded(), String.class);
        });
    }
}
