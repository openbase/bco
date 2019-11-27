package org.openbase.bco.app.cloudconnector;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.unit.app.App;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.app.AppRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.database.QueryType.Query;
import org.openbase.type.domotic.database.RecordCollectionType.RecordCollection;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.AggregatedServiceStateType.AggregatedServiceState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.app.AppDataType.AppData;
import org.openbase.type.communication.ScopeType.Scope;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AppRemoteAdapter implements App {

    private final AppRemote appRemote;

    public AppRemoteAdapter(final String appUnitId) throws NotAvailableException, InterruptedException {
        this(Units.getUnit(appUnitId, false, AppRemote.class));
    }

    public AppRemoteAdapter(final UnitConfig appUnitConfig) throws NotAvailableException, InterruptedException {
        this(Units.getUnit(appUnitConfig, false, AppRemote.class));
    }

    public AppRemoteAdapter(final AppRemote appRemote) {
        this.appRemote = appRemote;
    }

    @Override
    public Future<ActionDescription> setActivationState(final ActivationState activationState) {
        return appRemote.setActivationState(activationState);
    }

    @Override
    public UnitType getUnitType() throws NotAvailableException {
        return appRemote.getUnitType();
    }

    @Override
    public UnitTemplate getUnitTemplate() throws NotAvailableException {
        return appRemote.getUnitTemplate();
    }

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) {
        return appRemote.restoreSnapshot(snapshot);
    }

    @Override
    public Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue) {
        return appRemote.applyActionAuthenticated(authenticatedValue);
    }

    @Override
    public void addServiceStateObserver(final ServiceTempus serviceTempus, final ServiceType serviceType, final Observer observer) {
        appRemote.addServiceStateObserver(serviceTempus, serviceType, observer);
    }

    @Override
    public void removeServiceStateObserver(final ServiceTempus serviceTempus, final ServiceType serviceType, final Observer observer) {
        appRemote.removeServiceStateObserver(serviceTempus, serviceType, observer);
    }

    @Override
    public void addDataObserver(final ServiceTempus serviceTempus, final Observer<DataProvider<AppData>, AppData> observer) {
        appRemote.addDataObserver(serviceTempus, observer);
    }

    @Override
    public void removeDataObserver(final ServiceTempus serviceTempus, final Observer<DataProvider<AppData>, AppData> observer) {
        appRemote.removeDataObserver(serviceTempus, observer);
    }

    @Override
    public Future<ActionDescription> cancelAction(ActionDescription actionDescription) {
        return appRemote.cancelAction(actionDescription);
    }

    @Override
    public Future<ActionDescription> extendAction(ActionDescription actionDescription) {
        return appRemote.extendAction(actionDescription);
    }

    @Override
    public Future<AuthenticatedValue> queryAggregatedServiceStateAuthenticated(AuthenticatedValue databaseQuery) {
        return appRemote.queryAggregatedServiceStateAuthenticated(databaseQuery);
    }

    @Override
    public Future<AuthenticatedValue> restoreSnapshotAuthenticated(final AuthenticatedValue authenticatedSnapshot) {
        return appRemote.restoreSnapshotAuthenticated(authenticatedSnapshot);
    }

    @Override
    public Future<ActionDescription> applyAction(final ActionDescription actionDescription) {
        return appRemote.applyAction(actionDescription);
    }

    @Override
    public Message getServiceState(final ServiceType serviceType) throws NotAvailableException {
        return appRemote.getServiceState(serviceType);
    }

    @Override
    public Scope getScope() throws NotAvailableException {
        return appRemote.getScope();
    }

    @Override
    public long getTransactionId() throws NotAvailableException {
        return appRemote.getTransactionId();
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        return appRemote.applyConfigUpdate(unitConfig);
    }

    @Override
    public String getId() throws NotAvailableException {
        return appRemote.getId();
    }

    @Override
    public UnitConfig getConfig() throws NotAvailableException {
        return appRemote.getConfig();
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return appRemote.getLabel();
    }

    @Override
    public boolean isDataAvailable() {
        return appRemote.isDataAvailable();
    }

    @Override
    public Class<AppData> getDataClass() {
        return appRemote.getDataClass();
    }

    @Override
    public AppData getData() throws NotAvailableException {
        return appRemote.getData();
    }

    @Override
    public Future<AppData> getDataFuture() {
        return appRemote.getDataFuture();
    }

    @Override
    public void addDataObserver(final Observer<DataProvider<AppData>, AppData> observer) {
        appRemote.addDataObserver(observer);
    }

    @Override
    public void removeDataObserver(final Observer<DataProvider<AppData>, AppData> observer) {
        appRemote.removeDataObserver(observer);
    }

    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        appRemote.waitForData();
    }

    @Override
    public void waitForData(final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        appRemote.waitForData(timeout, timeUnit);
    }

    @Override
    public Future<AggregatedServiceState> queryAggregatedServiceState(final Query databaseQuery) {
        return appRemote.queryAggregatedServiceState(databaseQuery);
    }

    @Override
    public Future<AuthenticatedValue> queryRecordAuthenticated(AuthenticatedValue databaseQuery) {
        return appRemote.queryRecordAuthenticated(databaseQuery);
    }

    @Override
    public boolean isInfrastructure() {
        return appRemote.isInfrastructure();
    }

    @Override
    public Future<RecordCollection> queryRecord(Query databaseQuery) {
        return appRemote.queryRecord(databaseQuery);
    }

    public AppRemote getAppRemote() {
        return appRemote;
    }
}
