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
import com.google.protobuf.ExtensionRegistry;
import com.googlecode.protobuf.format.JsonFormat;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.openapi.server.api.UnitApi;
import org.openbase.bco.openapi.server.model.OpenbaseActionDescription;
import org.openbase.bco.openapi.server.model.OpenbaseActionParameter;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
@Controller
public class UnitApiController implements UnitApi {

    private static final long DEFAULT_SSE_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    static final JsonFormat JSON_FORMAT = new JsonFormat();

    private final Logger logger = LoggerFactory.getLogger(UnitApiController.class);

    @Override
    public ResponseEntity<OpenbaseActionDescription> unitApplyActionPost(@Valid @RequestBody OpenbaseActionParameter openbaseActionParameter) {
        final ActionParameter.Builder actionParameterBuilder = ActionParameter.newBuilder();
        try {
            JSON_FORMAT.merge(OBJECT_MAPPER.writeValueAsString(openbaseActionParameter), ExtensionRegistry.getEmptyRegistry(), actionParameterBuilder);
            ActionDescription actionDescription = new RemoteAction(actionParameterBuilder.build()).execute().get(5, TimeUnit.SECONDS);
            return ResponseEntity.ok(OBJECT_MAPPER.readValue(JSON_FORMAT.printToString(actionDescription), OpenbaseActionDescription.class));
        } catch (IOException | InstantiationException | ExecutionException | TimeoutException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/unit/events")
    public SseEmitter getAllUnitUpdates() {
        try {
            final SseEmitter sseEmitter = new SseEmitter(DEFAULT_SSE_TIMEOUT);
            final Observer<DataProvider<ColorableLightData>, ColorableLightData> observer = (source, data) -> sseEmitter.send(JSON_FORMAT.printToString(data));
            final ColorableLightRemote colorableLightRemote = Units.getUnitByAlias("ColorableLight-1", false, ColorableLightRemote.class);
            GlobalScheduledExecutorService.schedule(() -> colorableLightRemote.removeDataObserver(observer), DEFAULT_SSE_TIMEOUT, TimeUnit.MILLISECONDS);
            colorableLightRemote.addDataObserver(observer);
            return sseEmitter;
        } catch (NotAvailableException | InterruptedException e) {
            //TODO handle error correctly here
            return new SseEmitter();
        }
    }
}
