package org.openbase.bco.registry.unit.test;

/*
 * #%L
 * BCO Registry Unit Test
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.junit.Test;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.type.configuration.EntryType;
import org.openbase.type.configuration.MetaConfigType;
import org.openbase.type.domotic.binding.BindingConfigType.BindingConfig;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.state.InventoryStateType.InventoryState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.device.DeviceConfigType.DeviceConfig;
import org.openbase.type.domotic.unit.user.UserConfigType.UserConfig;
import org.openbase.type.geometry.AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat;
import org.openbase.type.geometry.TranslationType.Translation;
import org.openbase.type.math.Vec3DDoubleType.Vec3DDouble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.*;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class RegistrySyncTest extends AbstractBCORegistryTest {

    @Test(timeout = 5000)
    public void testRemoteRegistrySync() throws Exception {
        System.out.println("testRegisterUnitConfigWithUnits");

        Registries.shutdown();
        Registries.prepare();

        Registries.waitForData();
    }

    @Test(timeout = 5000)
    public void testUnitRemoteRegistrySync() throws Exception {
        System.out.println("testRegisterUnitConfigWithUnits");

        final UnitRegistryRemote unitRegistryRemote = new UnitRegistryRemote();
        unitRegistryRemote.init();
        unitRegistryRemote.activate();
        unitRegistryRemote.waitForData();
    }
}
