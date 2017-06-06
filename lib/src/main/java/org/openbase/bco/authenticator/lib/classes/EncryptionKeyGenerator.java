package org.openbase.bco.authenticator.lib.classes;

import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.LoggerFactory;

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
 * A key that is used to encrypt and decrypt tickets during Kerberos authentication
 * @author sebastian
 */
public class EncryptionKeyGenerator {
            
    private static final String TRANSFORMATION = "AES";
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(KeyGenerator.class);
        
    public static byte[] generateKey()  {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance(TRANSFORMATION);
        } catch (NoSuchAlgorithmException ex) {
            ExceptionPrinter.printHistory(new FatalImplementationErrorException("Key transformation non existent", keyGen, ex), LOGGER, LogLevel.ERROR);
        }
        keyGen.init(128);
        SecretKey secKey = keyGen.generateKey();
        return secKey.getEncoded();
    }
}
