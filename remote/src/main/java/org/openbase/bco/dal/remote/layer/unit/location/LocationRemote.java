package org.openbase.bco.dal.remote.layer.unit.location;

import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.lib.layer.unit.location.Location;
import org.openbase.bco.dal.remote.layer.unit.AbstractAggregatedBaseUnitRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.connection.ConnectionRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionEmphasisType.ActionEmphasis.Category;
import org.openbase.type.domotic.database.QueryType;
import org.openbase.type.domotic.database.RecordCollectionType;
import org.openbase.type.domotic.database.RecordType;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.*;
import org.openbase.type.domotic.state.StandbyStateType.StandbyState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import org.openbase.type.domotic.unit.location.LocationDataType;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;
import org.openbase.type.vision.ColorType;
import org.openbase.type.vision.HSBColorType;
import org.openbase.type.vision.RGBColorType;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.openbase.bco.dal.remote.layer.unit.Units.CONNECTION;
import static org.openbase.bco.dal.remote.layer.unit.Units.LOCATION;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationRemote extends AbstractAggregatedBaseUnitRemote<LocationData> implements Location {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationDataType.LocationData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColorType.HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorStateType.ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorType.Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColorType.RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerStateType.PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmStateType.AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionStateType.MotionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionStateType.PowerConsumptionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BlindStateType.BlindState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeStateType.SmokeState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StandbyStateType.StandbyState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperStateType.TamperState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessStateType.BrightnessState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureStateType.TemperatureState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PresenceStateType.PresenceState.getDefaultInstance()));
    }

    public LocationRemote() {
        super(LocationData.class);
    }

    public List<LocationRemote> getNeighborLocationList(final boolean waitForData) throws CouldNotPerformException {
        final List<LocationRemote> neighborList = new ArrayList<>();
        try {
            for (UnitConfig locationUnitConfig : Registries.getUnitRegistry().getNeighborLocationsByLocationId(getId())) {
                neighborList.add(Units.getUnit(locationUnitConfig, waitForData, LOCATION));
            }
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Could not get all neighbors!", ex);
        }
        return neighborList;
    }

    public List<LocationRemote> getChildLocationList(final boolean waitForData) throws CouldNotPerformException {
        final List<LocationRemote> childList = new ArrayList<>();
        for (String childId : getConfig().getLocationConfig().getChildIdList()) {
            try {
                childList.add(Units.getUnit(Registries.getUnitRegistry().getUnitConfigById(childId), waitForData, LOCATION));
            } catch (InterruptedException ex) {
                throw new CouldNotPerformException("Could not get all child locations!", ex);
            }
        }
        return childList;
    }


    public List<ConnectionRemote> getConnectionList(final boolean waitForData) throws CouldNotPerformException {
        if (!getConfig().getLocationConfig().getLocationType().equals(LocationType.TILE)) {
            throw new CouldNotPerformException("Location is not a Tile!");
        }

        List<ConnectionRemote> connectionList = new ArrayList<>();
        try {
            for (UnitConfig connectionUnitConfig : Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.CONNECTION)) {
                ConnectionRemote connection = Units.getUnit(connectionUnitConfig, waitForData, CONNECTION);
                if (connection.getConfig().getConnectionConfig().getTileIdList().contains(getId())) {
                    connectionList.add(connection);
                }
            }
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Could not get all connections!", ex);
        }
        return connectionList;
    }

    /**
     * Method collects all connections between this and the given location and returns those instances.
     *
     * @param locationID  the location id of the location to check.
     * @param waitForData flag defines if the method should block until all needed instances are available.
     * @return a collection of unit connection remotes.
     * @throws CouldNotPerformException is thrown if the check could not be performed e.g. if some data is not available yet.
     */
    public List<ConnectionRemote> getDirectConnectionList(final String locationID, final boolean waitForData) throws CouldNotPerformException {
        if (!getConfig().getLocationConfig().getLocationType().equals(LocationType.TILE)) {
            throw new CouldNotPerformException("Location is not a Tile!");
        }

        final List<ConnectionRemote> connectionList = new ArrayList<>();
        try {
            for (UnitConfig connectionUnitConfig : Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.CONNECTION)) {
                ConnectionRemote connection = Units.getUnit(connectionUnitConfig, waitForData, CONNECTION);
                if (connectionUnitConfig.getConnectionConfig().getTileIdList().contains(getId()) && connectionUnitConfig.getConnectionConfig().getTileIdList().contains(locationID)) {
                    connectionList.add(connection);
                }
            }
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Could not get all connections!", ex);
        }
        return connectionList;
    }

    /**
     * Method checks if an direct connection exists between this and the given location.
     *
     * @param locationID     the location id of the location to check.
     * @param connectionType the type of the connection. To disable this filter use ConnectionType.UNKNOWN
     * @param waitForData    flag defines if the method should block until all needed instances are available.
     * @return true if the specified connection exists.
     * @throws CouldNotPerformException is thrown if the check could not be performed e.g. if some data is not available yet.
     */
    public boolean hasDirectConnection(final String locationID, final ConnectionType connectionType, final boolean waitForData) throws CouldNotPerformException {
        // todo do not iterate over instances if configs providing all needed informations.
        for (ConnectionRemote relatedConnection : getDirectConnectionList(locationID, true)) {
            if (relatedConnection.getConfig().getConnectionConfig().getConnectionType().equals(connectionType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a Map of all units which directly or recursively provided by this location..
     *
     * @return the Map of provided units sorted by their UnitType.
     * @throws org.openbase.jul.exception.NotAvailableException is thrown if the map is not available.
     * @throws java.lang.InterruptedException                   is thrown if the current thread was externally interrupted.
     */
    public Map<UnitType, List<UnitRemote>> getUnitMap() throws NotAvailableException, InterruptedException {
        return getUnitMap(true);
    }

    /**
     * Returns a Map of all units which are directly or recursively provided by this location..
     *
     * @param recursive defines if recursive related unit should be included as well.
     * @return the Map of provided units sorted by their UnitType.
     * @throws org.openbase.jul.exception.NotAvailableException is thrown if the map is not available.
     * @throws java.lang.InterruptedException                   is thrown if the current thread was externally interrupted.
     */
    public Map<UnitType, List<UnitRemote>> getUnitMap(final boolean recursive) throws NotAvailableException, InterruptedException {
        try {
            final Map<UnitType, List<UnitRemote>> unitRemoteMap = new TreeMap<>();
            MultiException.ExceptionStack exceptionStack = null;

            for (final String unitId : getConfig().getLocationConfig().getUnitIdList()) {
                try {
                    UnitRemote<? extends Message> unitRemote = Units.getUnit(unitId, false);

                    // filter recursive units if needed.
                    if (!recursive && !unitRemote.getConfig().getPlacementConfig().getLocationId().equals(getId())) {
                        continue;
                    }

                    if (!unitRemoteMap.containsKey(unitRemote.getUnitType())) {
                        unitRemoteMap.put(unitRemote.getUnitType(), new ArrayList<>());
                    }
                    unitRemoteMap.get(unitRemote.getUnitType()).add(unitRemote);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
            try {
                MultiException.checkAndThrow(() -> "Could not collect all unit remotes of " + this, exceptionStack);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
            }
            return unitRemoteMap;

        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit map of " + this, ex);
        }
    }

    /**
     * Method returns a list of all units filtered by the given unit type which are directly or recursively provided by this location.
     *
     * @param <UR>            the unit remote class type.
     * @param unitType        the unit type.
     * @param waitForData     if this flag is set to true the current thread will block until all unit remotes are fully synchronized with the unit controllers.
     * @param unitRemoteClass the unit remote class.
     * @return a list of instances of the given remote class.
     * @throws CouldNotPerformException is thrown in case something went wrong.
     * @throws InterruptedException     is thrown if the current thread was externally interrupted.
     */
    public <UR extends UnitRemote<?>> List<UR> getUnits(final UnitType unitType, final boolean waitForData, final Class<UR> unitRemoteClass) throws CouldNotPerformException, InterruptedException {
        return getUnits(unitType, waitForData, unitRemoteClass, true);
    }

    /**
     * Method returns a list of all units filtered by the given unit type which are directly provided by this location.
     * In case the {@code recursive} flag is set to true than recursive related units are included as well.
     *
     * @param <UR>            the unit remote class type.
     * @param unitType        the unit type.
     * @param waitForData     if this flag is set to true the current thread will block until all unit remotes are fully synchronized with the unit controllers.
     * @param unitRemoteClass the unit remote class.
     * @param recursive       defines if recursive related unit should be included as well.
     * @return a list of instances of the given remote class.
     * @throws CouldNotPerformException is thrown in case something went wrong.
     * @throws InterruptedException     is thrown if the current thread was externally interrupted.
     */
    public <UR extends UnitRemote<?>> List<UR> getUnits(final UnitType unitType, final boolean waitForData, final Class<UR> unitRemoteClass, final boolean recursive) throws CouldNotPerformException, InterruptedException {
        final List<UR> unitRemote = new ArrayList<>();
        MultiException.ExceptionStack exceptionStack = null;
        Registries.waitForData();
        for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitType(getId(), unitType)) {
            try {
                if (recursive || unitConfig.getPlacementConfig().getLocationId().equals(getId())) {
                    unitRemote.add(Units.getUnit(unitConfig, waitForData, unitRemoteClass));
                }
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
        }
        try {
            MultiException.checkAndThrow(() -> "Could not collect all unit remotes of " + this, exceptionStack);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
        }
        return unitRemote;
    }

    @Override
    public Future<ActionDescription> setStandbyState(final StandbyState standbyState) {
        try {
            return applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(standbyState, ServiceType.STANDBY_STATE_SERVICE, this)
                    .addCategory(Category.ECONOMY));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    /**
     * Returns the average value of specific field from the power_consumption_state_service in a time window.
     *
     * @param window    Time interval in which the measurement is carried out (e.g every 1m, 1s, 1d ...)
     * @param field     Name of the field which should be checked (e.g consumption, current, voltage)
     * @param timeStart Timestamp when the measurement should start
     * @param timeStop  Timestamp when the measurement should stop
     * @return RecordCollection of mean values
     */
    public RecordCollectionType.RecordCollection getAveragePowerConsumptionTables(String window, Long timeStart, Long timeStop, String field) throws ExecutionException, InterruptedException {
        String query = "from(bucket:\"bco-persistence\")" +
                " |> range(start: " + timeStart + ", stop: " + timeStop + ")" +
                " |> filter(fn: (r) => r._measurement == \"power_consumption_state_service\")" +
                " |> filter(fn: (r) => r._field == \"" + field + "\")" +
                " |> aggregateWindow(every:" + window + " , fn: mean)" +
                " |> group(columns: [\"_time\"], mode:\"by\")" +
                "|> mean(column: \"_value\")";

        return queryRecord(QueryType.Query.newBuilder().setRawQuery(query).build()).get();
    }

    /**
     * Returns the average value of specific field from the power_consumption_state_service in a time window.
     *
     * @param window    Time interval in which the measurement is carried out (e.g every 1m, 1s, 1d ...)
     * @param field     Name of the field which should be checked (e.g consumption, current, voltage)
     * @param timeStart Timestamp when the measurement should start
     * @param timeStop  Timestamp when the measurement should stop
     * @return RecordCollection of mean values
     */
    public RecordCollectionType.RecordCollection getAveragePowerConsumptionTables(String window, String unitId, Long timeStart, Long timeStop, String field) throws ExecutionException, InterruptedException {
        String query = "from(bucket:\"bco-persistence\")" +
                " |> range(start: " + timeStart + ", stop: " + timeStop + ")" +
                " |> filter(fn: (r) => r._measurement == \"power_consumption_state_service\")" +
                " |> filter(fn: (r) => r._field == \"" + field + "\")" +
                " |> filter(fn: (r) => r.unit_id == \"" + unitId + "\")" +
                " |> aggregateWindow(every:" + window + " , fn: mean)" +
                " |> group(columns: [\"_time\"], mode:\"by\")" +
                " |> mean(column: \"_value\")";

        return queryRecord(QueryType.Query.newBuilder().setRawQuery(query).build()).get();

    }

    /**
     * Returns the average value of specific field from the power_consumption_state_service in a time window.
     *
     * @param window    Time interval in which the measurement is carried out (e.g every 1m, 1s, 1d ...)
     * @param field     Name of the field which should be checked (e.g consumption, current, voltage)
     * @param timeStart Timestamp when the measurement should start
     * @param timeStop  Timestamp when the measurement should stop
     * @return RecordCollection with one entry
     */
    public RecordCollectionType.RecordCollection getAveragePowerConsumption(String window, Long timeStart, Long timeStop, String field) throws ExecutionException, InterruptedException {

        String query = "from(bucket: \"bco-persistence\")" +
                " |> range(start: " + timeStart + ", stop: " + timeStop + ")" +
                " |> filter(fn: (r) => r._measurement == \"power_consumption_state_service\")" +
                " |> filter(fn: (r) => r._field == \"" + field + "\")" +
                " |> aggregateWindow(every:" + window + " , fn: mean)" +
                " |> group(columns: [\"_field\"], mode:\"by\")" +
                " |> mean(column: \"_value\")";

        return queryRecord(QueryType.Query.newBuilder().setRawQuery(query).build()).get();
    }

    /**
     * Returns the average value of specific field  and unit from the power_consumption_state_service in a time window.
     *
     * @param window    Time interval in which the measurement is carried out (e.g every 1m, 1s, 1d ...)
     * @param unit_id   Id of Unit
     * @param field     Name of the field which should be checked (e.g consumption, current, voltage)
     * @param timeStart Timestamp when the measurement should start
     * @param timeStop  Timestamp when the measurement should stop
     * @return RecordCollection with one entry
     */
    public RecordCollectionType.RecordCollection getAveragePowerConsumption(String window, String unit_id, Long timeStart, Long timeStop, String field) throws ExecutionException, InterruptedException {

        String query = "from(bucket: \"bco-persistence\")" +
                " |> range(start: " + timeStart + ", stop: " + timeStop + ")" +
                " |> filter(fn: (r) => r._measurement == \"power_consumption_state_service\")" +
                " |> filter(fn: (r) => r._field == \"" + field + "\")" +
                " |> filter(fn: (r) => r.unit_id == \"" + unit_id + "\")" +
                " |> aggregateWindow(every:" + window + " , fn: mean)" +
                " |> group(columns: [\"_field\"], mode:\"by\")" +
                " |> mean(column: \"_value\")";

        return queryRecord(QueryType.Query.newBuilder().setRawQuery(query).build()).get();
    }
}
