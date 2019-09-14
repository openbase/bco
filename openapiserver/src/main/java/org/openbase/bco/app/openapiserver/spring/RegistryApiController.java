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
import org.openbase.bco.openapi.server.model.InlineObject106;
import org.openbase.bco.openapi.server.model.OpenbaseUnitConfig;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import javax.validation.Valid;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
@Controller
//@RequestMapping("${openapi.sample.base-path:}") TODO: this can be used for a common base path - the RegistryApi defines this for every method...
public class RegistryApiController implements RegistryApi {

    private final Logger logger = LoggerFactory.getLogger(RegistryApiController.class);

    @Override
    public ResponseEntity<OpenbaseUnitConfig> registryUnitGetUnitConfigByAliasPost(@Valid InlineObject106 inlineObject106) {
        logger.info("Get unit config by alias: " + inlineObject106);
        try {
            return ResponseEntity.ok(RegistryRPCProcessor.invokeMethod(inlineObject106, OpenbaseUnitConfig.class));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Getting unit config by alias failed!", ex, logger);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
