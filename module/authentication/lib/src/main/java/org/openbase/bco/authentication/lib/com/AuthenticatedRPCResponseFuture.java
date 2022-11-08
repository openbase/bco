package org.openbase.bco.authentication.lib.com;

import org.jetbrains.annotations.NotNull;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.jul.communication.data.RPCResponse;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This is a wrapper type to pass the Properties of the RPC Response of the initial request through the
 * authentication process.
 *
 * @param <M> the inital type of the wrapped RPCResponse
 */
public class AuthenticatedRPCResponseFuture<M> implements Future<RPCResponse<M>> {

    final Future<RPCResponse<AuthenticatedValue>> requestFuture;
    final AuthenticatedValueFuture<M> authenticationFuture;

    public AuthenticatedRPCResponseFuture(final Future<RPCResponse<AuthenticatedValue>> actualFuture, final AuthenticatedValueFuture<M> authenticationFuture) {
        this.requestFuture = actualFuture;
        this.authenticationFuture = authenticationFuture;
    }

    @Override
    public boolean cancel(boolean b) {
        return this.requestFuture.cancel(b);
    }

    @Override
    public boolean isCancelled() {
        return this.requestFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.requestFuture.isDone();
    }

    @Override
    public RPCResponse<M> get() throws InterruptedException, ExecutionException {
        requestFuture.get();
        return new RPCResponse<>(authenticationFuture.get(), requestFuture.get().getProperties());
    }

    @Override
    public RPCResponse<M> get(long l, @NotNull TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        requestFuture.get(l, timeUnit);
        return new RPCResponse<>(authenticationFuture.get(), requestFuture.get().getProperties());
    }
}
