package org.openbase.bco.registry.lib.com;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.jul.extension.type.iface.TransactionIdProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.communication.TransactionValueType.TransactionValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    public static <T extends Message> Future<T> requestVerifiedAction(final T type, final SymmetricCallable<TransactionValue> symmetricCallable) {
        final TransactionValue transactionValue = TransactionValue.newBuilder().setValue(type.toByteString()).build();
        return new TransactionValueConversionFuture<>(symmetricCallable.call(transactionValue), (Class<T>) type.getClass());
    }

    public interface SymmetricCallable<T extends Message> {
        Future<T> call(final T type);
    }

    private static <T extends Message> T parseFrom(final ByteString bytes, final Class<T> messageClass) throws CouldNotPerformException {
        try {
            final Method parseFrom = messageClass.getMethod("parseFrom", ByteString.class);
            return (T) parseFrom.invoke(null, bytes);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new CouldNotPerformException("Could not parse byte string into message class[" + messageClass.getSimpleName() + "]", ex);
        }
    }

    private static class TransactionValueConversionFuture<T extends Message> implements Future<T> {

        private final Future<TransactionValue> internalFuture;
        private final Class<T> returnClass;

        public TransactionValueConversionFuture(final Future<TransactionValue> internalFuture, final Class<T> returnClass) {
            this.internalFuture = internalFuture;
            this.returnClass = returnClass;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return internalFuture.cancel(true);
        }

        @Override
        public boolean isCancelled() {
            return internalFuture.isCancelled();
        }

        @Override
        public boolean isDone() {
            return internalFuture.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            try {
                return parseFrom(internalFuture.get().getValue(), returnClass);
            } catch (CouldNotPerformException ex) {
                throw new ExecutionException("Could not parse byte string to class[" + returnClass.getSimpleName() + "]", ex);
            }
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            try {
                return parseFrom(internalFuture.get(timeout, unit).getValue(), returnClass);
            } catch (CouldNotPerformException ex) {
                throw new ExecutionException("Could not parse byte string to class[" + returnClass.getSimpleName() + "]", ex);
            }
        }
    }
}
