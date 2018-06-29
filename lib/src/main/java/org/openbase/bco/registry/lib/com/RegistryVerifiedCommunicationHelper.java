package org.openbase.bco.registry.lib.com;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rsb.com.TransactionIdProvider;
import org.openbase.jul.extension.rsb.com.future.TransactionVerificationConversionFuture;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.communication.TransactionValueType.TransactionValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class RegistryVerifiedCommunicationHelper {

    public static <T extends Message> Future<TransactionValue> executeVerifiedAction(final TransactionValue transactionValue, final TransactionIdProvider transactionIdProvider, final Class<T> messageClass, final SymmetricCallable<T> symmetricCallable) {
        return GlobalCachedExecutorService.submit(() -> {
            final T message = symmetricCallable.call(parseFrom(transactionValue.getValue(), messageClass)).get();
            return TransactionValue.newBuilder().setValue(message.toByteString()).setTransactionId(transactionIdProvider.getTransactionId()).build();
        });
    }

    public static <T extends Message, PROVIDER extends DataProvider<?> & TransactionIdProvider> Future<T> requestVerifiedAction(final T type, final PROVIDER dataProvider, final SymmetricCallable<TransactionValue> symmetricCallable) throws CouldNotPerformException {
        final TransactionValue transactionValue = TransactionValue.newBuilder().setValue(type.toByteString()).build();
        final Future<TransactionValue> transactionValueFuture = symmetricCallable.call(transactionValue);
        final Class<T> messageClass = (Class<T>) type.getClass();
        return new TransactionVerificationConversionFuture<T>(transactionValueFuture, dataProvider, messageClass);
    }

    public interface SymmetricCallable<T extends Message> {
        Future<T> call(final T type) throws CouldNotPerformException;
    }

    private static <T extends Message> T parseFrom(final ByteString bytes, final Class<T> messageClass) throws CouldNotPerformException {
        try {
            final Method parseFrom = messageClass.getMethod("parseFrom", ByteString.class);
            return (T) parseFrom.invoke(null, bytes);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new CouldNotPerformException("Could not parse byte string into message class[" + messageClass.getSimpleName() + "]", ex);
        }
    }
}
