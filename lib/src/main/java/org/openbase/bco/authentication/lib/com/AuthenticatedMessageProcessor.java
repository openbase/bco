package org.openbase.bco.authentication.lib.com;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2018 openbase.org
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

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.processing.SimpleMessageProcessor;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;

import javax.crypto.BadPaddingException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AuthenticatedMessageProcessor<M extends GeneratedMessage> extends SimpleMessageProcessor<M> {

    public AuthenticatedMessageProcessor(Class<M> dataClass) {
        super(dataClass);
    }

    @Override
    public M process(GeneratedMessage input) throws CouldNotPerformException, InterruptedException {
        if (input instanceof AuthenticatedValue) {
            AuthenticatedValue authenticatedValue = (AuthenticatedValue) input;
            if (SessionManager.getInstance().isLoggedIn()) {
                try {
                    return super.process(EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), SessionManager.getInstance().getSessionKey(), getDataClass()));
                } catch (BadPaddingException | IOException ex) {
                    throw new CouldNotPerformException("Decrypting result in of authenticated value failed!", ex);
                }
            } else {
                try {
                    Method parseFrom = getDataClass().getMethod("parseFrom", ByteString.class);
                    return super.process((M) parseFrom.invoke(null, authenticatedValue.getValue()));
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new CouldNotPerformException("Could not invoke parseFrom method on [" + getDataClass().getSimpleName() + "]", ex);
                }
            }
        } else {
            return super.process(input);
        }
    }
}
