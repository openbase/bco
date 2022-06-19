package org.openbase.bco.dal.remote.layer.unit.object;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.openbase.bco.dal.remote.layer.unit.AbstractUnitRemote;
import org.openbase.jul.communication.data.RPCResponse;
import org.openbase.jul.exception.*;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.communication.EventType.Event;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.dal.ObjectDataType.ObjectData;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ObjectRemote extends AbstractUnitRemote<ObjectData> {

    private final ObjectData data = ObjectData.getDefaultInstance();
    private boolean active;

    public ObjectRemote() {
        super(ObjectData.class);
    }

    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        return;
    }

    @Override
    public void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        return;
    }

    @Override
    public Future<ActionDescription> applyAction(ActionDescription actionDescription) {
        return FutureProcessor.canceledFuture(ActionDescription.class, new NotSupportedException("Method[applyAction]", this));
    }

    @Override
    public Future<Snapshot> recordSnapshot() {
        return FutureProcessor.completedFuture(Snapshot.getDefaultInstance());
    }

    @Override
    public Future<Void> restoreSnapshot(Snapshot snapshot) {
        return FutureProcessor.completedFuture(null);
    }

    @Override
    public void addServiceStateObserver(ServiceType serviceType, Observer observer) {
        // dummy has no function
    }

    @Override
    public void removeServiceStateObserver(ServiceType serviceType, Observer observer) {
        // dummy has no function
    }

    @Override
    public void addHandler(Function1<Event, Unit> handler, boolean wait) {
        // dummy has no function
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        active = true;
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        active = false;
    }

    @Override
    public boolean isConnected() {
        return active;
    }

    @Override
    public ConnectionState.State getConnectionState() {
        if (isConnected()) {
            return ConnectionState.State.CONNECTED;
        } else {
            return ConnectionState.State.DISCONNECTED;
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public <R> Future<RPCResponse<R>> callMethodAsync(String methodName, Class<R> returnClazz) {
        return (Future<RPCResponse<R>>) FutureProcessor.canceledFuture(new NotSupportedException("Method[callMethodAsync]", this));
    }

    @Override
    public <R> R callMethod(String methodName, Class<R> returnClazz) throws CouldNotPerformException, InterruptedException {
        throw new NotSupportedException("Method[callMethod]", this);
    }

    @Override
    public <R, T> R callMethod(String methodName, Class<R> returnClazz, T argument) throws CouldNotPerformException, InterruptedException {
        throw new NotSupportedException("Method[callMethod]", this);
    }

    @Override
    public <R> R callMethod(String methodName, Class<R> returnClazz, long timeout) throws CouldNotPerformException, InterruptedException {
        throw new NotSupportedException("Method[callMethod]", this);
    }

    @Override
    public <R, T> R callMethod(String methodName, Class<R> returnClazz, T argument, long timeout) throws CouldNotPerformException, InterruptedException {
        throw new NotSupportedException("Method[callMethod]", this);
    }

    @Override
    public <R, T> Future<RPCResponse<R>> callMethodAsync(String methodName, Class<R> returnClazz, T argument) {
        return (Future<RPCResponse<R>>) FutureProcessor.canceledFuture(new NotSupportedException("Method[callMethodAsync]", this));
    }

    @Override
    public Future<ObjectData> requestData() {
        return FutureProcessor.completedFuture(data);
    }

    @Override
    public ObjectData getData() throws NotAvailableException {
        return data;
    }

    @Override
    public boolean isDataAvailable() {
        return true;
    }

    @Override
    public void validateMiddleware() throws InvalidStateException {
        // dummy has no function
    }

    @Override
    public void validateData() throws InvalidStateException {
        // dummy has no function
    }

    @Override
    public void waitForMiddleware() throws CouldNotPerformException, InterruptedException {
        // dummy has no function
    }

    @Override
    public Future<Long> ping() {
        return FutureProcessor.completedFuture(0L);
    }

    @Override
    public Long getPing() {
        return 0L;
    }

    @Override
    public Future<ObjectData> getDataFuture() {
        return FutureProcessor.completedFuture(data);
    }
}
