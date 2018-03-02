package org.openbase.bco.dal.remote.unit;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.jul.exception.*;
import org.openbase.jul.pattern.Observer;
import rsb.Handler;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.dal.LightDataType.LightData;
import rst.domotic.unit.dal.ObjectDataType.ObjectData;

import java.util.concurrent.CompletableFuture;
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
    public Future<ActionFuture> applyAction(ActionDescription actionDescription) throws CouldNotPerformException, InterruptedException, RejectedException {
        throw new NotSupportedException("Method[applyAction]", this);
    }

    @Override
    public Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return CompletableFuture.completedFuture(Snapshot.getDefaultInstance());
    }

    @Override
    public Future<Void> restoreSnapshot(Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        return CompletableFuture.completedFuture(null);
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
    public void addHandler(Handler handler, boolean wait) throws InterruptedException, CouldNotPerformException {
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
    public ConnectionState getConnectionState() {
        if (isConnected()) {
            return ConnectionState.CONNECTED;
        } else {
            return ConnectionState.DISCONNECTED;
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public <R> Future<R> callMethodAsync(String methodName) throws CouldNotPerformException {
        throw new NotSupportedException("Method[callMethodAsync]", this);
    }

    @Override
    public <R> R callMethod(String methodName) throws CouldNotPerformException, InterruptedException {
        throw new NotSupportedException("Method[callMethod]", this);
    }

    @Override
    public <R, T> R callMethod(String methodName, T argument) throws CouldNotPerformException, InterruptedException {
        throw new NotSupportedException("Method[callMethod]", this);
    }

    @Override
    public <R> R callMethod(String methodName, long timeout) throws CouldNotPerformException, TimeoutException, InterruptedException {
        throw new NotSupportedException("callMethod[applyAction]", this);
    }

    @Override
    public <R, T> R callMethod(String methodName, T argument, long timeout) throws CouldNotPerformException, TimeoutException, InterruptedException {
        throw new NotSupportedException("callMethod[applyAction]", this);
    }

    @Override
    public <R, T> Future<R> callMethodAsync(String methodName, T argument) throws CouldNotPerformException {
        throw new NotSupportedException("Method[callMethodAsync]", this);
    }

    @Override
    public CompletableFuture<ObjectData> requestData() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(data);
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
        return CompletableFuture.completedFuture(0l);
    }

    @Override
    public Long getPing() {
        return 0l;
    }

    @Override
    public CompletableFuture<ObjectData> getDataFuture() {
        return CompletableFuture.completedFuture(data);
    }
}
