package org.openbase.app.openapiserver;

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
import org.junit.Before;
import org.junit.Test;
import org.openbase.app.test.agent.AbstractBCOAppManagerTest;
import org.openbase.bco.app.openapiserver.OpenApiServerApp;
import org.openbase.bco.openapi.server.model.InlineObject106;
import org.openbase.bco.openapi.server.model.InlineObject133;
import org.openbase.bco.openapi.server.model.OpenbaseUnitConfig;
import org.openbase.bco.openapi.server.model.OpenbaseUnitTemplateUnitType;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OpenApiServerAppTest extends AbstractBCOAppManagerTest {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    static final JsonFormat JSON_FORMAT = new JsonFormat();

    private static final String PORT = "12321";
    private static final String URL_BASE = "http://localhost:" + PORT;

    private final Logger logger = LoggerFactory.getLogger(OpenApiServerAppTest.class);

    public Class getAppClass() {
        return OpenApiServerApp.class;
    }

    public UnitConfig.Builder getAppConfig() {
        UnitConfig.Builder appConfigBuilder = UnitConfig.newBuilder();
        appConfigBuilder.getMetaConfigBuilder().addEntryBuilder()
                .setKey(OpenApiServerApp.KEY_PORT)
                .setValue("12321");
        return appConfigBuilder;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        //TODO this has to wait properly for the spring application to start
        //Thread.sleep(5000);
    }

    private <T> T assertResponse(final ResponseEntity<T> responseEntity) {
        assertEquals("Response did not return okay!", HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull("Response body is empty!", responseEntity.getBody());
        return responseEntity.getBody();
    }

    //@Test
    public void testGetDeviceClasses() throws Exception {
        final String url = URL_BASE + "/registry/class/getDeviceClasses";

        final List<DeviceClass> deviceClasses = new ArrayList<>();
        for (final Object object : assertResponse(new RestTemplate().getForEntity(url, List.class))) {
            DeviceClass.Builder deviceClassBuilder = DeviceClass.newBuilder();
            JSON_FORMAT.merge(OBJECT_MAPPER.writeValueAsString(object), ExtensionRegistry.getEmptyRegistry(), deviceClassBuilder);
            deviceClasses.add(deviceClassBuilder.build());
        }
        assertEquals("DeviceClasses are not returned equally!", Registries.getClassRegistry().getDeviceClasses(), deviceClasses);
    }

    //@Test
    public void testGetUnitConfigsByUnitType() throws Exception {
        final String url = URL_BASE + "/registry/unit/getUnitConfigsByUnitType";

        final List<UnitConfig> expected = Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitTemplateType.UnitTemplate.UnitType.COLORABLE_LIGHT);

        final InlineObject133 parameter = new InlineObject133().arg0(OpenbaseUnitTemplateUnitType.COLORABLE_LIGHT);
        final List<UnitConfig> is = new ArrayList<>();
        for (final Object object : assertResponse(new RestTemplate().postForEntity(url, parameter, List.class))) {
            UnitConfig.Builder unitConfigBuilder = UnitConfig.newBuilder();
            JSON_FORMAT.merge(OBJECT_MAPPER.writeValueAsString(object), ExtensionRegistry.getEmptyRegistry(), unitConfigBuilder);
            is.add(unitConfigBuilder.build());
        }

        assertEquals("Unexpected result for getUnitConfigsByUnitType!", expected, is);
    }

    //@Test
    public void testGetUnitConfigByAlias() throws Exception {
        final String url = URL_BASE + "/registry/unit/getUnitConfigByAlias";
        final String alias = Registries.getUnitRegistry().getRootLocationConfig().getAlias(0);

        final UnitConfig expected = Registries.getUnitRegistry().getUnitConfigByAlias(alias);

        final InlineObject106 parameter = new InlineObject106().arg0(alias);
        final OpenbaseUnitConfig openbaseUnitConfig = assertResponse(new RestTemplate().postForEntity(url, parameter, OpenbaseUnitConfig.class));
        final UnitConfig.Builder unitConfigBuilder = UnitConfig.newBuilder();
        JSON_FORMAT.merge(OBJECT_MAPPER.writeValueAsString(openbaseUnitConfig), ExtensionRegistry.getEmptyRegistry(), unitConfigBuilder);
        final UnitConfig is = unitConfigBuilder.build();

        assertEquals(expected, is);
    }
}
