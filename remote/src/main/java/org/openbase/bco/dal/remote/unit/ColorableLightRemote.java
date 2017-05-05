package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * BCO DAL Remote
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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.unit.ColorableLight;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.PowerStateType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.dal.ColorableLightDataType;
import rst.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import rst.vision.ColorType;
import rst.vision.HSBColorType.HSBColor;
import rst.vision.RGBColorType.RGBColor;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ColorableLightRemote extends AbstractUnitRemote<ColorableLightData> implements ColorableLight {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorableLightDataType.ColorableLightData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorStateType.ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerStateType.PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorType.Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescription.getDefaultInstance()));
    }

    public ColorableLightRemote() {
        super(ColorableLightData.class);
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConfig = super.applyConfigUpdate(config);
        return unitConfig;
    }

    @Override
    public Future<Void> setColorState(final ColorState colorState) throws CouldNotPerformException {
        System.out.println("SetColorState for remote[" + this + "]");
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
//        ResourceAllocation.Builder resourceAllocation = actionDescription.getResourceAllocationBuilder();

        updateActionDescription(actionDescription, colorState);

//        resourceAllocation.setId("");
//        Interval.Builder slotBuilder = resourceAllocation.getSlotBuilder();
//        slotBuilder.setBegin(TimestampProcessor.getCurrentTimestamp());
//        slotBuilder.setEnd(TimestampProcessor.getCurrentTimestamp());
//        resourceAllocation.setState(ResourceAllocation.State.REQUESTED);
        System.out.println("ApplyAction " + actionDescription.build());
        try {
            return this.applyAction(actionDescription.build());
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException(ex);
        }
    }

    private void updateActionDescription(final ActionDescription.Builder actionDescription, final Object serviceAttribute) throws CouldNotPerformException {
        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        ResourceAllocation.Builder resourceAllocation = actionDescription.getResourceAllocationBuilder();

        serviceStateDescription.setUnitId(getId());
        resourceAllocation.addResourceIds(ScopeGenerator.generateStringRep(getScope()));

        actionDescription.setDescription(actionDescription.getDescription().replace(ActionDescriptionProcessor.LABEL_KEY, getLabel()));
        //TODO: update USER key with authentification
        actionDescription.setLabel(actionDescription.getLabel().replace(ActionDescriptionProcessor.LABEL_KEY, getLabel()));

        Service.upateActionDescription(actionDescription, serviceAttribute);
    }

    @Override
    public Future<Void> setNeutralWhite() throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(this, Void.class);
    }

    @Override
    public Future<Void> setBrightnessState(BrightnessState value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerState", ex);
        }
    }

    @Override
    public Future<Void> setPowerState(PowerState powerState) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(powerState, this, Void.class);
    }

    public Future<Void> setPowerState(PowerState.State powerState) throws CouldNotPerformException {
        return setPowerState(PowerState.newBuilder().setValue(powerState).build());
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        try {
            return getData().getColorState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ColorState", ex);
        }
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        try {
            return getData().getBrightnessState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("BrightnessState", ex);
        }
    }
}
