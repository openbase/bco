/*-
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.vision.ColorType.Color;
import rst.vision.HSBColorType.HSBColor;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class ResourceBlockingTest {

    @Test
    public void blockColorableLight() throws Exception {
        Registries.waitForData();
        UnitConfig location = Registries.getLocationRegistry().getLocationConfigsByLabel("Wardrobe").get(0);
        List<UnitConfig> lights = Registries.getLocationRegistry().getUnitConfigsByLocation(UnitType.COLORABLE_LIGHT, location.getId());
        ColorableLightRemote remote = Units.getUnit(lights.get(0), true, ColorableLightRemote.class);

        ColorState.Builder colorState = ColorState.newBuilder();
        Color.Builder color = colorState.getColorBuilder();
        HSBColor.Builder HSBColor = color.getHsbColorBuilder();
        HSBColor.setBrightness(100);
        HSBColor.setSaturation(100);
        HSBColor.setSaturation(0);
        color.setType(Color.Type.HSB);
        
        ActionDescription.Builder powerAction = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        PowerState.Builder powerState = PowerState.newBuilder();
        powerState.setValue(PowerState.State.ON);
        updateActionDescription(powerAction, powerState.build(), remote);
        
        Future<ActionFuture> applyAction = remote.applyAction(powerAction.build());
        applyAction.get();
        
        LocationRemote locationRemote = Units.getUnit(location, true, LocationRemote.class);
        // should work without exception
        locationRemote.setColorState(colorState.build()).get();
        
        try{
            locationRemote.setPowerState(powerState.build()).get();
        } catch(CouldNotPerformException | ExecutionException ex) {
            System.out.println("Exception as expected");
        }
    }

    public ActionDescription.Builder updateActionDescription(final ActionDescription.Builder actionDescription, final Object serviceAttribute, final UnitRemote unitRemote) throws CouldNotPerformException {
        // 5 minute retaining:
        actionDescription.setExecutionTimePeriod(1000 * 60 * 5);
        
        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        ResourceAllocation.Builder resourceAllocation = actionDescription.getResourceAllocationBuilder();

        serviceStateDescription.setUnitId((String) unitRemote.getId());
        resourceAllocation.addResourceIds(ScopeGenerator.generateStringRep(unitRemote.getScope()));

        actionDescription.setDescription(actionDescription.getDescription().replace(ActionDescriptionProcessor.LABEL_KEY, unitRemote.getLabel()));
        //TODO: update USER key with authentification
        actionDescription.setLabel(actionDescription.getLabel().replace(ActionDescriptionProcessor.LABEL_KEY, unitRemote.getLabel()));

        return Service.upateActionDescription(actionDescription, serviceAttribute);
    }
}
