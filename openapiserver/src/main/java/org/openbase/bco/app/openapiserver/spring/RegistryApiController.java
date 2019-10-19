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

import org.openbase.bco.app.openapiserver.RegistryRPCProcessor;
import org.openbase.bco.openapi.server.api.RegistryApi;
import org.openbase.bco.openapi.server.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
@Controller
public class RegistryApiController implements RegistryApi {

    private final Logger logger = LoggerFactory.getLogger(RegistryApiController.class);

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OpenbaseActivityTemplate>> registryTemplateGetActivityTemplatesGet() {
        class Tmp extends ArrayList<OpenbaseActivityTemplate> {}
        return (ResponseEntity) RegistryRPCProcessor.invokeMethodOrFailWithBadRequest(null, Tmp.class, logger);
    }

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OpenbaseServiceTemplate>> registryTemplateGetServiceTemplatesGet() {
        class Tmp extends ArrayList<OpenbaseServiceTemplate> {}
        return (ResponseEntity) RegistryRPCProcessor.invokeMethodOrFailWithBadRequest(null, Tmp.class, logger);
    }

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OpenbaseUnitTemplate>> registryTemplateGetUnitTemplatesGet() {
        class Tmp extends ArrayList<OpenbaseUnitTemplate> {}
        return (ResponseEntity) RegistryRPCProcessor.invokeMethodOrFailWithBadRequest(null, Tmp.class, logger);
    }

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OpenbaseDeviceClass>> registryClassGetDeviceClassesGet() {
        class Tmp extends ArrayList<OpenbaseDeviceClass> {}
        return (ResponseEntity) RegistryRPCProcessor.invokeMethodOrFailWithBadRequest(null, Tmp.class, logger);
    }

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OpenbaseAgentClass>> registryClassGetAgentClassesGet() {
        class Tmp extends ArrayList<OpenbaseAgentClass> {}
        return (ResponseEntity) RegistryRPCProcessor.invokeMethodOrFailWithBadRequest(null, Tmp.class, logger);
    }

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OpenbaseAppClass>> registryClassGetAppClassesGet() {
        class Tmp extends ArrayList<OpenbaseAppClass> {}
        return (ResponseEntity) RegistryRPCProcessor.invokeMethodOrFailWithBadRequest(null, Tmp.class, logger);
    }

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OpenbaseUnitConfig>> registryUnitGetDalUnitConfigsGet() {
        class Tmp extends ArrayList<OpenbaseUnitConfig> {}
        return (ResponseEntity) RegistryRPCProcessor.invokeMethodOrFailWithBadRequest(null, Tmp.class, logger);
    }

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OpenbaseUnitConfig>> registryUnitGetUnitConfigsGet() {
        class Tmp extends ArrayList<OpenbaseUnitConfig> {}
        return (ResponseEntity) RegistryRPCProcessor.invokeMethodOrFailWithBadRequest(null, Tmp.class, logger);
    }

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OpenbaseUnitConfig>> registryUnitGetUnitConfigsByUnitTypePost(@Valid @RequestBody InlineObject133 inlineObject133) {
        class Tmp extends ArrayList<OpenbaseUnitConfig> {}
        return (ResponseEntity) RegistryRPCProcessor.invokeMethodOrFailWithBadRequest(inlineObject133, Tmp.class, logger);
    }

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<List<OpenbaseActivityConfig>> registryActivityGetActivityConfigsGet() {
        class Tmp extends ArrayList<OpenbaseActivityConfig> {}
        return (ResponseEntity) RegistryRPCProcessor.invokeMethodOrFailWithBadRequest(null, Tmp.class, logger);
    }

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<OpenbaseUnitConfig> registryUnitGetUnitConfigByAliasPost(@Valid @RequestBody InlineObject106 inlineObject106) {
        return RegistryRPCProcessor.invokeMethodOrFailWithBadRequest(inlineObject106, OpenbaseUnitConfig.class, logger);
    }

    @Override
    @CrossOrigin(origins = "*")
    public ResponseEntity<Boolean> registryClassContainsAppClassByIdPost(@Valid @RequestBody InlineObject44 inlineObject44) {
        return RegistryRPCProcessor.invokeMethodOrFailWithBadRequest(inlineObject44, Boolean.class, logger);
    }
}
