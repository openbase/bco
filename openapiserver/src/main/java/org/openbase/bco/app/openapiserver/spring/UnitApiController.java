package org.openbase.bco.app.openapiserver.spring;

/*-
 * #%L
 * BCO OpenAPI Server
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.openapi.server.api.UnitApi;
import org.openbase.bco.openapi.server.model.OpenbaseActionDescription;
import org.openbase.bco.openapi.server.model.OpenbaseActionParameter;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
@Controller
public class UnitApiController implements UnitApi {

    //TODO: this should be configurable via the app meta config
    private static final long DEFAULT_SSE_TIMEOUT = TimeUnit.HOURS.toMillis(12);

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    static final JsonFormat JSON_FORMAT = new JsonFormat();

    private final Logger logger = LoggerFactory.getLogger(UnitApiController.class);

    private final SyncObject observerMapLock;
    private final Map<String, UnitSSEObserver> observerMap;

    public UnitApiController() {
        this.observerMapLock = new SyncObject("ObserverMapLock");
        this.observerMap = new HashMap<>();
    }

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<OpenbaseActionDescription> unitApplyActionPost(@Valid @RequestBody OpenbaseActionParameter openbaseActionParameter) {
        final ActionParameter.Builder actionParameterBuilder = ActionParameter.newBuilder();
        try {
            JSON_FORMAT.merge(OBJECT_MAPPER.writeValueAsString(openbaseActionParameter), ExtensionRegistry.getEmptyRegistry(), actionParameterBuilder);
            ActionDescription actionDescription = new RemoteAction(actionParameterBuilder.build()).execute().get(5, TimeUnit.SECONDS);
            return ResponseEntity.ok(OBJECT_MAPPER.readValue(JSON_FORMAT.printToString(actionDescription), OpenbaseActionDescription.class));
        } catch (IOException | InstantiationException | ExecutionException | TimeoutException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/unit/events")
    public SseEmitter getUnitEvents(
            @RequestParam(name = "unitId") String unitId,
            @RequestParam(name = "serviceType", required = false, defaultValue = "UNKNOWN") ServiceType serviceType,
            @RequestParam(name = "serviceTempus", required = false, defaultValue = "CURRENT") ServiceTempus serviceTempus,
            @RequestParam(name = "recursive", required = false, defaultValue = "false") boolean recursive) {
        try {
            logger.debug("Create SSEEmitter for unit[{}] with type[{}] and tempus[{}]. Recursive={}", unitId, serviceType, serviceTempus, recursive);

            // retrieve units which should be observed
            final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
            final List<UnitRemote<?>> unitRemoteList = new ArrayList<>();
            if (unitConfig.getUnitType() == UnitType.LOCATION) {
                if (recursive) {
                    for (String internalUnitId : unitConfig.getLocationConfig().getUnitIdList()) {
                        unitRemoteList.add(Units.getUnit(internalUnitId, false));
                    }
                } else {
                    unitRemoteList.add(Units.getUnit(unitId, false));
                }
            } else {
                unitRemoteList.add(Units.getUnit(unitId, false));
            }

            // create an emitter an add/create according observers
            final SseEmitter sseEmitter = new SseEmitter(DEFAULT_SSE_TIMEOUT);
            synchronized (observerMapLock) {
                for (final UnitRemote<?> unitRemote : unitRemoteList) {
                    if (observerMap.containsKey(unitRemote.getId())) {
                        // add emitter to existing observer
                        observerMap.get(unitRemote.getId()).addEmitter(sseEmitter);
                    } else {
                        // create new observer
                        final UnitSSEObserver unitSSEObserver = new UnitSSEObserver(unitRemote);
                        unitSSEObserver.addEmitter(sseEmitter);
                        observerMap.put(unitRemote.getId(), unitSSEObserver);
                        unitRemote.addServiceStateObserver(serviceTempus, serviceType, unitSSEObserver);
                    }

                }
            }

            sseEmitter.onError(throwable -> {
                logger.debug("Remove emitter from sse observers because of an error:", throwable);
                synchronized (observerMapLock) {
                    for (final UnitRemote<?> unitRemote : unitRemoteList) {
                        try {
                            observerMap.get(unitRemote.getId()).removeEmitter(sseEmitter);
                        } catch (NotAvailableException e) {
                            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not remove SSEEmitter from observer", e), logger);
                        }
                    }
                }
            });
            // remove emitters from observers on completion
            sseEmitter.onCompletion(() -> {
                logger.debug("Remove emitter from sse observers");
                synchronized (observerMapLock) {
                    for (final UnitRemote<?> unitRemote : unitRemoteList) {
                        try {
                            observerMap.get(unitRemote.getId()).removeEmitter(sseEmitter);
                        } catch (NotAvailableException e) {
                            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not remove SSEEmitter from observer", e), logger);
                        }
                    }
                }
            });

            // return emitter
            return sseEmitter;
        } catch (NotAvailableException e) {
            ExceptionPrinter.printHistory(e, logger);
            throw new UnitNotAvailableException(e);
        } catch (InterruptedException e) {
            // this should never happen, since it not waited for data
            ExceptionPrinter.printHistory(new FatalImplementationErrorException("Interruption even though it is never waited", this, e), logger);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            ExceptionPrinter.printHistory(e, logger);
            //TODO: remove and handle correctly when issue https://github.com/openbase/bco.dal/issues/158 is solved
            throw new IllegalArgumentException("ServiceType[" + serviceType.name() + "] is not available!", e);
        }
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "invalid unit id")
    private class UnitNotAvailableException extends RuntimeException {
        UnitNotAvailableException(final Throwable cause) {
            super(cause);
        }
    }

    private class UnitSSEObserver implements Observer<ServiceStateProvider<Message>, Message> {

        private final SyncObject emitterLock;
        private final List<SseEmitter> emitters;

        private final String unitId;
        private final UnitRemote<?> unitRemote;

        UnitSSEObserver(final UnitRemote<?> unitRemote) throws NotAvailableException {
            this.unitRemote = unitRemote;
            this.unitId = unitRemote.getId();

            this.emitterLock = new SyncObject("SSEEmitterLock");
            this.emitters = new ArrayList<>();
        }

        @Override
        public void update(final ServiceStateProvider<Message> source, final Message data) throws Exception {
            final JsonReader jsonReader = new JsonReader(new StringReader(JSON_FORMAT.printToString(data)));
            jsonReader.setLenient(true);
            final JsonObject serviceStateAsJsonObject = new JsonParser().parse(jsonReader).getAsJsonObject();
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("unitId", unitId);
            jsonObject.addProperty("serviceTempus", source.getServiceTempus().name());
            jsonObject.addProperty("serviceType", source.getServiceType().name());
            jsonObject.add("serviceState", serviceStateAsJsonObject);

            synchronized (emitterLock) {
                for (final SseEmitter emitter : emitters) {
                    emitter.send(jsonObject.toString());
                }
            }
        }

        void addEmitter(final SseEmitter emitter) {
            synchronized (emitterLock) {
                this.emitters.add(emitter);
            }
        }

        void removeEmitter(final SseEmitter emitter) {
            synchronized (observerMapLock) {
                synchronized (emitterLock) {
                    this.emitters.remove(emitter);

                    if (this.emitters.isEmpty()) {
                        unitRemote.removeServiceStateObserver(ServiceTempus.UNKNOWN, ServiceType.UNKNOWN, this);
                        observerMap.remove(unitId);
                    }
                }
            }
        }
    }
}
