package org.openbase.bco.authentication.lib.com;

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
