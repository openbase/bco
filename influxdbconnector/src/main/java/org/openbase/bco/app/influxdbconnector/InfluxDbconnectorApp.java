package org.openbase.bco.app.influxdbconnector;

/*-
 * #%L
 * BCO InfluxDB Connector
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


import org.influxdata.client.*;
import org.influxdata.client.domain.WritePrecision;
import org.influxdata.client.write.Point;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.influxdata.client.domain.Bucket;
import org.influxdata.client.write.events.WriteErrorEvent;
import org.influxdata.client.write.events.WriteSuccessEvent;
import org.openbase.bco.dal.control.layer.unit.app.AbstractAppController;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.preset.JPTestMode;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceDescriptionType;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.service.ServiceTempusTypeType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.language.LabelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.openbase.bco.dal.lib.layer.service.Services.resolveStateValue;

public class InfluxDbconnectorApp extends AbstractAppController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final Integer READ_TIMEOUT = 60;
    private static final Integer WRITE_TIMEOUT = 60;
    private static final Integer CONNECT_TIMOUT = 40;
    private static final long MAX_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    private static final Integer ADDITIONAL_TIMEOUT = 60000;
    private static final Integer DATABASE_TIMEOUT_DEFAULT = 60000;
    private static final String INFLUXDB_BUCKET = "INFLUXDB_BUCKET";
    private static final String INFLUXDB_BUCKET_DEFAULT = "bco-persistence";
    private static final String INFLUXDB_BATCH_TIME = "INFLUXDB_BATCH_TIME";
    private static final String INFLUXDB_BATCH_TIME_DEFAULT = "1000";
    private static final String INFLUXDB_BATCH_LIMIT = "INFLUXDB_BATCH_LIMIT";
    private static final String INFLUXDB_BATCH_LIMIT_DEFAULT = "100";
    private static final String INFLUXDB_URL = "INFLUXDB_URL";
    private static final String INFLUXDB_URL_DEFAULT = "http://localhost:9999";
    private static final String INFLUXDB_ORG = "INFLUXDB_ORG";
    private static final String INFLUXDB_ORG_DEFAULT = "openbase";
    private static final String INFLUXDB_TOKEN = "INFLUXDB_TOKEN";
    private static final long HEARTBEAT_PERIOD = TimeUnit.MINUTES.toMillis(15);
    private static final Integer HEARTBEAT_INITIAL_DELAY = 0;
    private static final Integer HEARTBEAT_ONLINE_VALUE = 1;
    private static final Integer HEARTBEAT_OFFLINE_VALUE = 0;
    private static final String HEARTBEAT_MEASUREMENT = "heartbeat";
    private static final String HEARTBEAT_FIELD = "alive";


    private WriteApi writeApi;
    private Integer databaseTimeout = DATABASE_TIMEOUT_DEFAULT;
    private Bucket bucket;
    private char[] token;
    private Future task;
    private Future heartbeat;
    private String databaseUrl;
    private String bucketName;
    private InfluxDBClient influxDBClient;
    private Integer batchTime;
    private Integer batchLimit;
    private final CustomUnitPool customUnitPool;
    private final Observer<ServiceStateProvider<Message>, Message> unitStateObserver;
    private String org;


    public InfluxDbconnectorApp() throws InstantiationException {
        this.customUnitPool = new CustomUnitPool();
        this.unitStateObserver = (source, data) -> storeServiceState((Unit) source.getServiceProvider(), source.getServiceType(), false);
    }

    @Override
    public UnitConfigType.UnitConfig applyConfigUpdate(UnitConfigType.UnitConfig config) throws CouldNotPerformException, InterruptedException {
        config = super.applyConfigUpdate(config);

        bucketName = generateVariablePool().getValue(INFLUXDB_BUCKET, INFLUXDB_BUCKET_DEFAULT);
        batchTime = Integer.valueOf(generateVariablePool().getValue(INFLUXDB_BATCH_TIME, INFLUXDB_BATCH_TIME_DEFAULT));
        batchLimit = Integer.valueOf(generateVariablePool().getValue(INFLUXDB_BATCH_LIMIT, INFLUXDB_BATCH_LIMIT_DEFAULT));
        databaseUrl = generateVariablePool().getValue(INFLUXDB_URL, INFLUXDB_URL_DEFAULT);
        token = generateVariablePool().getValue(INFLUXDB_TOKEN).toCharArray();
        org = generateVariablePool().getValue(INFLUXDB_ORG, INFLUXDB_ORG_DEFAULT);
        return config;
    }

    @Override
    protected ActionDescription execute(ActivationState activationState) {

        task = GlobalCachedExecutorService.submit(() -> {

            try {
                logger.debug("Execute influx db connector");

                // connect to db
                connectToDatabase();
                while (!task.isCancelled()) {
                    try {
                        verifyConnection();
                        break;

                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory("Could not reach influxdb server at " + databaseUrl + ". Try again in " + databaseTimeout / 1000 + " seconds!", ex, logger, LogLevel.WARN);
                        Thread.sleep(databaseTimeout);
                        if (databaseTimeout < MAX_TIMEOUT) databaseTimeout += ADDITIONAL_TIMEOUT;
                    }
                }

                // lookup bucked
                while (!task.isCancelled()) {
                    try {
                        // check if bucked found
                        getDatabaseBucket();
                        break;

                    } catch (NotAvailableException ex) {
                        logger.warn("Could not get bucket. Try again in " + databaseTimeout / 1000 + " seconds!");

                        ExceptionPrinter.printHistory(ex, logger);
                        Thread.sleep(databaseTimeout);
                    }
                }

                // start observation
                try {
                    startObservation();
                } catch (InitializationException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            } catch (InterruptedException ex) {
                // finish task because its canceled.
            }

            if (!task.isCancelled() && isConnected()) {
                try {
                    // write initial heartbeat
                    logger.debug("initial heartbeat");
                    writeApi.writePoint(bucketName, org, Point.measurement(HEARTBEAT_MEASUREMENT)
                            .addField(HEARTBEAT_FIELD, HEARTBEAT_OFFLINE_VALUE)
                            .time(System.currentTimeMillis() - 1, WritePrecision.MS));
                    writeApi.writePoint(bucketName, org, Point.measurement(HEARTBEAT_MEASUREMENT)
                            .addField(HEARTBEAT_FIELD, HEARTBEAT_ONLINE_VALUE)
                            .time(System.currentTimeMillis(), WritePrecision.MS));

                    heartbeat = GlobalScheduledExecutorService.scheduleAtFixedRate(() -> {
                        logger.debug("write heartbeat");
                        writeApi.writePoint(bucketName, org, Point.measurement(HEARTBEAT_MEASUREMENT)
                                .addField(HEARTBEAT_FIELD, HEARTBEAT_ONLINE_VALUE)
                                .time(System.currentTimeMillis(), WritePrecision.MS));

                    }, HEARTBEAT_INITIAL_DELAY, HEARTBEAT_PERIOD, TimeUnit.MILLISECONDS);
                } catch (NotAvailableException ex) {
                    ExceptionPrinter.printHistory("Could not write heartbeat!", ex, logger, LogLevel.WARN);
                }
            }
            return null;
        });
        return activationState.getResponsibleAction();
    }

    @Override
    protected void stop(ActivationState activationState) throws CouldNotPerformException, InterruptedException {

        // finish task
        logger.debug("finish task");
        if (task != null && !task.isDone()) {
            task.cancel(true);
            try {
                task.get(5, TimeUnit.SECONDS);
            } catch (CancellationException ex) {
                // that's what we are waiting for.
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(ex, logger);
            }
        }

        logger.debug("finish heartbeat");
        if (heartbeat != null && !heartbeat.isDone()) {
            heartbeat.cancel(true);
            try {
                task.get(5, TimeUnit.SECONDS);
            } catch (CancellationException ex) {
                // that's what we are waiting for.
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(ex, logger);
            }
        }

        if (isConnected()) {
            // write final heartbeat if connection is established.
            writeApi.writePoint(bucketName, org, Point.measurement(HEARTBEAT_MEASUREMENT)
                    .addField(HEARTBEAT_FIELD, HEARTBEAT_ONLINE_VALUE)
                    .time(System.currentTimeMillis() - 1, WritePrecision.MS));
            writeApi.writePoint(bucketName, org, Point.measurement(HEARTBEAT_MEASUREMENT)
                    .addField(HEARTBEAT_FIELD, HEARTBEAT_OFFLINE_VALUE)
                    .time(System.currentTimeMillis(), WritePrecision.MS));
            writeApi.flush();
        }

        // deregister
        customUnitPool.removeObserver(unitStateObserver);
        customUnitPool.deactivate();
        disconnectDatabase();

        super.stop(activationState);
    }


    public void startObservation() throws InitializationException, InterruptedException {
        try {
            // setup pool
            customUnitPool.addObserver(unitStateObserver);
            customUnitPool.activate();

            for (UnitConfigType.UnitConfig unitConfig : Registries.getUnitRegistry(true).getUnitConfigs()) {
                final UnitRemote<?> unit = Units.getUnit(unitConfig, true);

                try {
                    for (ServiceDescriptionType.ServiceDescription serviceDescription : unit.getUnitTemplate().getServiceDescriptionList()) {

                        if (serviceDescription.getPattern() != ServiceTemplateType.ServiceTemplate.ServicePattern.PROVIDER) {
                            continue;
                        }
                        storeServiceState(unit, serviceDescription.getServiceType(), true);
                    }
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not store service state " + unit, ex, logger);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void storeServiceState(Unit<?> unit, ServiceTemplateType.ServiceTemplate.ServiceType serviceType, boolean initialSync) throws CouldNotPerformException {
        final Message currentServiceState = Services.invokeProviderServiceMethod(serviceType, ServiceTempus.CURRENT, unit.getData());
        Message lastServiceState = Services.invokeProviderServiceMethod(serviceType, ServiceTempusTypeType.ServiceTempusType.ServiceTempus.LAST, unit.getData());

        // todo: @david please make sure the current state is stored even when the last state is not available.

        try {
            final long serviceStateTimestamp = TimestampProcessor.getTimestamp(currentServiceState, TimeUnit.MILLISECONDS) - 1l;
            lastServiceState = TimestampProcessor.updateTimestamp(serviceStateTimestamp, lastServiceState, TimeUnit.MILLISECONDS);
            storeServiceState(unit, serviceType, currentServiceState);
            storeServiceState(unit, serviceType, lastServiceState);
        } catch (CouldNotPerformException ex) {

            // filter log if initial timestamps are missing
            if (initialSync) {
                return;
            }

            ExceptionPrinter.printHistory("Could not store service state change into db! " +
                            "UnitType[" + unit.getUnitType().toString() + "] " +
                            "ServiceType[" + serviceType.toString() + "] " +
                            "CurrentServiceState[" + currentServiceState.toString() + "] " +
                            "LastServiceState[" + lastServiceState.toString() + "]"
                    , ex, logger, LogLevel.WARN);
        }
    }

    private void storeServiceState(final Unit<?> unit, final ServiceTemplateType.ServiceTemplate.ServiceType serviceType, final Message serviceState) throws InvalidStateException {

        final long timestamp = TimestampProcessor.getTimestamp(serviceState, TimeUnit.MILLISECONDS);
        try {
            String initiator;
            try {
                initiator = Services.getResponsibleAction(serviceState).getActionInitiator().getInitiatorType().name().toLowerCase();
            } catch (NotAvailableException ex) {
                // in this case we use the system as initiator because responsible actions are not available for pure provider services and those are always system generated.
                initiator = "system";
            }
            Map<String, String> stateValuesMap = resolveStateValueToMap(serviceState);
            Point point = Point.measurement(serviceType.name().toLowerCase())
                    .addTag("alias", unit.getConfig().getAlias(0))
                    .addTag("initiator", initiator)
                    .addTag("unit_id", unit.getId())
                    .addTag("unit_type", unit.getUnitType().name().toLowerCase())
                    .addTag("location_id", unit.getParentLocationConfig().getId())
                    .addTag("location_alias", unit.getParentLocationConfig().getAlias(0))
                    .time(timestamp, WritePrecision.MS);

            Integer values = 0;
            for (Map.Entry<String, String> entry : stateValuesMap.entrySet()) {
                // detect numbers with regex
                if (entry.getValue().matches("-?\\d+(\\.\\d+)?")) {
                    values++;
                    point.addField(entry.getKey(), Double.valueOf(entry.getValue()));

                } else {
                    point.addTag(entry.getKey(), entry.getValue());
                }
            }
            List<LabelType.Label.MapFieldEntry> entryList = unit.getConfig().getLabel().getEntryList();
            for (LabelType.Label.MapFieldEntry entry : entryList) {
                point.addTag("label_" + entry.getKey(), entry.getValue(0));
            }

            if (values > 0) {
                writeApi.writePoint(bucketName, org, point);
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not store service state " + serviceType.name() + " of " + unit, ex, logger);
        }
    }


    public Map<String, String> resolveStateValueToMap(Message serviceState) throws CouldNotPerformException {
        final Map<String, String> stateValues = new HashMap<>();
        for (Descriptors.FieldDescriptor fieldDescriptor : serviceState.getDescriptorForType().getFields()) {
            String stateName = fieldDescriptor.getName();
            String stateType = fieldDescriptor.getType().toString().toLowerCase();

            // filter invalid states
            if (stateName == null || stateType == null) {
                logger.warn("Could not detect datatype of " + stateName);
            }

            // filter general service fields
            switch (stateName) {
                case "last_value_occurrence":
                case "timestamp":
                case "responsible_action":
                case "type":
                case "rgb_color":
                case "frame_id":
                    continue;
            }

            // filter data units
            if (stateName.endsWith("data_unit")) {
                continue;
            }

            String stateValue = serviceState.getField(fieldDescriptor).toString();

            try {
                if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
                    if (fieldDescriptor.isRepeated()) {
                        List<String> types = new ArrayList<>();

                        for (int i = 0; i < serviceState.getRepeatedFieldCount(fieldDescriptor); i++) {
                            final Object repeatedFieldEntry = serviceState.getRepeatedField(fieldDescriptor, i);
                            if (repeatedFieldEntry instanceof Message) {
                                types.add("[" + resolveStateValue((Message) repeatedFieldEntry).toString() + "]");
                            }
                            types.add(repeatedFieldEntry.toString());
                        }
                        stateType = types.toString().toLowerCase();
                    } else {
                        stateValue = resolveStateValue((Message) serviceState.getField(fieldDescriptor)).toString();
                    }
                }
            } catch (InvalidStateException ex) {
                logger.warn("Could not process value of " + fieldDescriptor.getName());
                continue;
            }

            // filter values
            switch (stateValue) {
                case "":
                case "NaN":
                    continue;
                default:
                    break;
            }
            if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.ENUM) {
                String finalStateValue = stateValue;
                stateValue = String.valueOf(fieldDescriptor.getEnumType().getValues().stream().filter(val -> val.getName().equals(finalStateValue)).findFirst().get().getNumber());
            }

            stateValues.put(fieldDescriptor.getName(), stateValue.toLowerCase());
        }
        return stateValues;
    }

    /**
     * Method checks if the connection is established.
     *
     * @return true if the connection to influx db is established, otherwise false.
     */
    private boolean isConnected() {
        try {
            verifyConnection();
        } catch (VerificationFailedException ex) {
            return false;
        }
        return true;
    }

    /**
     * Method verifies the connection state.
     *
     * @throws VerificationFailedException is thrown if the connection is not established.
     */
    private void verifyConnection() throws VerificationFailedException {

        if (influxDBClient == null) {
            throw new VerificationFailedException("Influx db connection has never been initiated.");
        }

        if (influxDBClient.health().getStatus().getValue() != "pass") {
            throw new VerificationFailedException("Could not connect to database server at " + databaseUrl + "!");
        }

        // initiate WriteApi
        WriteOptions writeoptions = WriteOptions.builder().batchSize(batchLimit).flushInterval(batchTime).build();
        writeApi = influxDBClient.getWriteApi(writeoptions);
        writeApi.listenEvents(WriteSuccessEvent.class, event -> {
            logger.debug("Successfully wrote data into db");
        });
        writeApi.listenEvents(WriteErrorEvent.class, event -> {
            Throwable exception = event.getThrowable();
            logger.warn(exception.getMessage());
        });
        logger.debug("Connected to Influxdb at " + databaseUrl);
    }

    private void connectToDatabase() {
        try {
            if (influxDBClient != null) {
                influxDBClient.close();
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("Could not shutdown database connection!", ex, logger);
        }
        logger.debug(" Try to connect to influxDB at " + databaseUrl);
        influxDBClient = InfluxDBClientFactory
                .create(databaseUrl + "?readTimeout=" + READ_TIMEOUT + "&connectTimeout=" + CONNECT_TIMOUT + "&writeTimeout=" + WRITE_TIMEOUT + "&logLevel=BASIC", token);
    }

    private void disconnectDatabase() {
        try {
            if (influxDBClient != null) {
                if (writeApi != null) {
                    writeApi.flush();
                }
                influxDBClient.close();
                writeApi = null;
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("Could not shutdown database connection!", ex, logger);
        }
    }

    private void getDatabaseBucket() throws NotAvailableException {
        logger.debug("Get bucket " + bucketName);
        bucket = influxDBClient.getBucketsApi().findBucketByName(bucketName);
        if (bucket == null) {
            throw new NotAvailableException("bucket", bucketName);
        }
    }
}
