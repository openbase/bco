package org.openbase.bco.device.openhab.communication;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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

import com.google.gson.*;
import org.eclipse.smarthome.core.internal.service.CommandDescriptionServiceImpl;
import org.eclipse.smarthome.core.internal.types.CommandDescriptionImpl;
import org.eclipse.smarthome.core.types.CommandDescription;
import org.eclipse.smarthome.core.types.CommandDescriptionBuilder;
import org.eclipse.smarthome.core.types.CommandOption;
import org.openbase.bco.device.openhab.jp.JPOpenHABURI;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class OpenHABRestConnection implements Shutdownable {

    public static final String SEPARATOR = "/";
    public static final String REST_TARGET = "rest";

    public static final String APPROVE_TARGET = "approve";
    public static final String EVENTS_TARGET = "events";

    public static final String TOPIC_KEY = "topic";
    public static final String TOPIC_SEPARATOR = SEPARATOR;

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenHABRestConnection.class);

    private final SyncObject topicObservableMapLock = new SyncObject("topicObservableMapLock");
    private final SyncObject connectionStateSyncLock = new SyncObject("connectionStateSyncLock");
    private final Map<String, ObservableImpl<Object, JsonObject>> topicObservableMap;

    private final Client restClient;
    private final WebTarget restTarget;
    private SseEventSource sseSource;

    private boolean shutdownInitiated = false;

    protected final JsonParser jsonParser;
    protected final Gson gson;

    private ScheduledFuture<?> connectionTask;

    protected ConnectionState.State openhabConnectionState = State.DISCONNECTED;

    public OpenHABRestConnection() throws InstantiationException {
        try {
            this.topicObservableMap = new HashMap<>();
            this.gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                    return false;
                }

                @Override
                public boolean shouldSkipClass(Class<?> aClass) {
                    // ignore Command Description because its an interface and can not be serialized without any instance creator.
                    if(aClass.equals(CommandDescription.class)) {
                        return true;
                    }
                    return false;
                }
            }).create();
            this.jsonParser = new JsonParser();
            this.restClient = ClientBuilder.newClient();
            this.restTarget = restClient.target(JPService.getProperty(JPOpenHABURI.class).getValue().resolve(SEPARATOR + REST_TARGET));
            this.setConnectState(State.CONNECTING);
        } catch (JPNotAvailableException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private boolean isTargetReachable() {
        try {
            testConnection();
        } catch (CouldNotPerformException e) {
            if (e.getCause() instanceof ProcessingException) {
                return false;
            }
        }
        return true;
    }

    protected abstract void testConnection() throws CouldNotPerformException;

    public void waitForConnectionState(final ConnectionState.State connectionState, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        synchronized (connectionStateSyncLock) {
            while (getOpenhabConnectionState() != connectionState) {
                connectionStateSyncLock.wait(timeUnit.toMillis(timeout));
            }
        }
    }

    public void waitForConnectionState(final ConnectionState.State connectionState) throws InterruptedException {
        synchronized (connectionStateSyncLock) {
            while (getOpenhabConnectionState() != connectionState) {
                connectionStateSyncLock.wait();
            }
        }
    }

    private void setConnectState(final ConnectionState.State connectState) {
        synchronized (connectionStateSyncLock) {

            // filter non changing states
            if (connectState == this.openhabConnectionState) {
                return;
            }
            LOGGER.trace("Openhab Connection State changed to: "+connectState);

            // update state
            this.openhabConnectionState = connectState;

            // handle state change
            switch (connectState) {
                case CONNECTING:
                    LOGGER.info("Wait for openHAB...");
                    try {
                        connectionTask = GlobalScheduledExecutorService.scheduleWithFixedDelay(() -> {
                            if (isTargetReachable()) {
                                // set connected
                                setConnectState(State.CONNECTED);

                                // cleanup own task
                                connectionTask.cancel(false);
                            }
                        }, 0, 15, TimeUnit.SECONDS);
                    } catch (NotAvailableException | RejectedExecutionException ex) {
                        // if global executor service is not available we have no chance to connect.
                        LOGGER.warn("Wait for openHAB...", ex);
                        setConnectState(State.DISCONNECTED);
                    }
                    break;
                case CONNECTED:
                    LOGGER.info("Connection to OpenHAB established.");
                    initSSE();
                    break;
                case RECONNECTING:
                    LOGGER.warn("Connection to OpenHAB lost!");
                    resetConnection();
                    setConnectState(State.CONNECTING);
                    break;
                case DISCONNECTED:
                    LOGGER.info("Connection to OpenHAB closed.");
                    resetConnection();
                    break;
            }

            // notify state change
            connectionStateSyncLock.notifyAll();

            // apply next state if required
            switch (connectState) {
                case RECONNECTING:
                    setConnectState(State.CONNECTING);
                    break;
            }
        }
    }

    private void initSSE() {
        // activate sse source if not already done
        if (sseSource != null) {
            LOGGER.warn("SSE already initialized!");
            return;
        }

        final WebTarget webTarget = restTarget.path(EVENTS_TARGET);
        sseSource = SseEventSource.target(webTarget).reconnectingEvery(15, TimeUnit.SECONDS).build();
        sseSource.open();

        final Consumer<InboundSseEvent> evenConsumer = inboundSseEvent -> {
            // dispatch event
            try {
                final JsonObject payload = jsonParser.parse(inboundSseEvent.readData()).getAsJsonObject();
                for (Entry<String, ObservableImpl<Object, JsonObject>> topicObserverEntry : topicObservableMap.entrySet()) {
                    try {
                        if (payload.get(TOPIC_KEY).getAsString().matches(topicObserverEntry.getKey())) {
                            topicObserverEntry.getValue().notifyObservers(payload);
                        }
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not notify listeners on topic[" + topicObserverEntry.getKey() + "]", ex), LOGGER);
                    }
                }
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not handle SSE payload!", ex), LOGGER);
            }
        };

        final Consumer<Throwable> errorHandler = ex -> {
            ExceptionPrinter.printHistory("Openhab connection error detected!", ex, LOGGER, LogLevel.DEBUG);
            checkConnectionState();
        };

        final Runnable reconnectHandler = () -> {
            checkConnectionState();
        };

        sseSource.register(evenConsumer, errorHandler, reconnectHandler);
    }

    public State getOpenhabConnectionState() {
        return openhabConnectionState;
    }

    public void checkConnectionState() {
        synchronized (connectionStateSyncLock) {
            // only validate if connected
            if (!isConnected()) {
                return;
            }

            // if not reachable init a reconnect
            if (!isTargetReachable()) {
                setConnectState(State.RECONNECTING);
            }
        }
    }

    public boolean isConnected() {
        return getOpenhabConnectionState() == State.CONNECTED;
    }

    public void addSSEObserver(Observer<Object, JsonObject> observer) {
        addSSEObserver(observer, "");
    }

    public void addSSEObserver(final Observer<Object, JsonObject> observer, final String topicRegex) {
        synchronized (topicObservableMapLock) {
            if (topicObservableMap.containsKey(topicRegex)) {
                topicObservableMap.get(topicRegex).addObserver(observer);
                return;
            }

            final ObservableImpl<Object, JsonObject> observable = new ObservableImpl<>(this);
            observable.addObserver(observer);
            topicObservableMap.put(topicRegex, observable);
        }
    }

    public void removeSSEObserver(Observer<Object, JsonObject> observer) {
        removeSSEObserver(observer, "");
    }

    public void removeSSEObserver(Observer<Object, JsonObject> observer, final String topicFilter) {
        synchronized (topicObservableMapLock) {
            if (topicObservableMap.containsKey(topicFilter)) {
                topicObservableMap.get(topicFilter).removeObserver(observer);
            }
        }
    }

    private void resetConnection() {
        // cancel ongoing connection task
        if (!connectionTask.isDone()) {
            connectionTask.cancel(false);
        }

        // close sse
        if (sseSource != null) {
            sseSource.close();
            sseSource = null;
        }
    }

    public void validateConnection() throws CouldNotPerformException {
        if (!isConnected()) {
            throw new InvalidStateException("Openhab not reachable yet!");
        }
    }

    private String validateResponse(final Response response) throws CouldNotPerformException, ProcessingException {
        return validateResponse(response, false);
    }

    private String validateResponse(final Response response, final boolean skipConnectionValidation) throws CouldNotPerformException, ProcessingException {
        final String result = response.readEntity(String.class);

        if (response.getStatus() == 200 || response.getStatus() == 201 || response.getStatus() == 202) {
            return result;
        } else if (response.getStatus() == 404) {
            if (!skipConnectionValidation) {
                checkConnectionState();
            }
            throw new NotAvailableException("URL");
        } else if (response.getStatus() == 503) {
            if (!skipConnectionValidation) {
                checkConnectionState();
            }
            // throw a processing exception to indicate that openHAB is still not fully started, this is used to wait for openHAB
            throw new ProcessingException("OpenHAB server not ready");
        } else {
            throw new CouldNotPerformException("Response returned with ErrorCode[" + response.getStatus() + "], Result[" + result + "] and ErrorMessage[" + response.getStatusInfo().getReasonPhrase() + "]");
        }
    }

    protected String get(final String target) throws CouldNotPerformException {
        return get(target, false);
    }

    protected String get(final String target, final boolean skipValidation) throws CouldNotPerformException {
        try {

            // handle validation
            if (!skipValidation) {
                validateConnection();
            }

            final WebTarget webTarget = restTarget.path(target);
            final Response response = webTarget.request().get();

            return validateResponse(response, skipValidation);
        } catch (CouldNotPerformException | ProcessingException ex) {
            if (isShutdownInitiated()) {
                ExceptionProcessor.setInitialCause(ex, new ShutdownInProgressException(this));
            }
            throw new CouldNotPerformException("Could not get sub-URL[" + target + "]", ex);
        }
    }

    protected String delete(final String target) throws CouldNotPerformException {
        try {
            validateConnection();
            final WebTarget webTarget = restTarget.path(target);
            final Response response = webTarget.request().delete();

            return validateResponse(response);
        } catch (CouldNotPerformException | ProcessingException ex) {
            if (isShutdownInitiated()) {
                ExceptionProcessor.setInitialCause(ex, new ShutdownInProgressException(this));
            }
            throw new CouldNotPerformException("Could not delete sub-URL[" + target + "]", ex);
        }
    }

    protected String putJson(final String target, final Object value) throws CouldNotPerformException {
        return put(target, gson.toJson(value), MediaType.APPLICATION_JSON_TYPE);
    }

    protected String put(final String target, final String value, final MediaType mediaType) throws CouldNotPerformException {
        try {
            validateConnection();
            final WebTarget webTarget = restTarget.path(target);
            final Response response = webTarget.request().put(Entity.entity(value, mediaType));

            return validateResponse(response);
        } catch (CouldNotPerformException | ProcessingException ex) {
            if (isShutdownInitiated()) {
                ExceptionProcessor.setInitialCause(ex, new ShutdownInProgressException(this));
            }
            throw new CouldNotPerformException("Could not put value[" + value + "] on sub-URL[" + target + "]", ex);
        }
    }

    protected String postJson(final String target, final Object value) throws CouldNotPerformException {
        return post(target, gson.toJson(value), MediaType.APPLICATION_JSON_TYPE);
    }

    protected String post(final String target, final String value, final MediaType mediaType) throws CouldNotPerformException {
        try {
            validateConnection();
            final WebTarget webTarget = restTarget.path(target);
            final Response response = webTarget.request().post(Entity.entity(value, mediaType));

            return validateResponse(response);
        } catch (CouldNotPerformException | ProcessingException ex) {
            if (isShutdownInitiated()) {
                ExceptionProcessor.setInitialCause(ex, new ShutdownInProgressException(this));
            }
            throw new CouldNotPerformException("Could not post Value[" + value + "] of MediaType[" + mediaType + "] on sub-URL[" + target + "]", ex);
        }
    }

    public boolean isShutdownInitiated() {
        return shutdownInitiated;
    }

    @Override
    public void shutdown() {

        // prepare shutdown
        shutdownInitiated = true;
        setConnectState(State.DISCONNECTED);

        // stop rest service
        restClient.close();

        // stop sse service
        synchronized (topicObservableMapLock) {
            for (final Observable<Object, JsonObject> jsonObjectObservable : topicObservableMap.values()) {
                jsonObjectObservable.shutdown();
            }

            topicObservableMap.clear();
            resetConnection();
        }
    }
}
