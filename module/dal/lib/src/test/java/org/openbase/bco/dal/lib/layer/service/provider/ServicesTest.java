package org.openbase.bco.dal.lib.layer.service.provider;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.state.States.Power;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.bco.registry.unit.test.AbstractBCORegistryTest;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription.Builder;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ServicesTest extends AbstractBCORegistryTest {
    
    @Test
    @Timeout(value = 30)
    public void testComputeActionImpact() throws CouldNotPerformException, InterruptedException {

        final UnitRegistryRemote unitRegistry = Registries.getUnitRegistry(true);

        final Builder serviceStateBuilder = ServiceStateDescription.newBuilder();
        serviceStateBuilder.setUnitId(unitRegistry.getRootLocationConfig().getId());
        serviceStateBuilder.setServiceState(Services.serializeServiceState(Power.ON, true));
        serviceStateBuilder.setServiceType(ServiceType.POWER_STATE_SERVICE);

        final Set<ActionDescription> impact = Services.computeActionImpact(serviceStateBuilder.build());

        impact.forEach(
                it -> assertNotEquals("Computed impact does not offer an valid action id!", "", it.getActionId())
        );

        final List<String> impactedUnitIdList = impact.stream()
                .map(it -> it.getServiceStateDescription().getUnitId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        final List<String> affectedUnitsUnsorted =
                unitRegistry.getUnitConfigsByLocationIdAndServiceType(unitRegistry.getRootLocationConfig().getId(), serviceStateBuilder.getServiceType())
                        .stream()
                        .map(UnitConfig::getId)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

        assertEquals(affectedUnitsUnsorted, impactedUnitIdList, "impacted unit id list differs as expected");
    }
}
