package org.openbase.bco.dal.remote.service;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.collection.ColorStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.extension.rst.transform.HSBColorToRGBColorTransformer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.vision.ColorType;
import rst.vision.HSBColorType.HSBColor;
import rst.vision.RGBColorType.RGBColor;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ColorStateServiceRemote extends AbstractServiceRemote<ColorStateOperationService, ColorState> implements ColorStateOperationServiceCollection {

    public ColorStateServiceRemote() {
        super(ServiceType.COLOR_STATE_SERVICE, ColorState.class);
    }

    public Collection<ColorStateOperationService> getColorStateOperationServices() {
        return getServices();
    }

    /**
     * {@inheritDoc}
     * Computes the average RGB color.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected ColorState computeServiceState() throws CouldNotPerformException {
        return getColorState(UnitType.UNKNOWN);
    }

    @Override
    public Future<ActionFuture> setColorState(final ColorState colorState) throws CouldNotPerformException {
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);

        try {
            return applyAction(Service.upateActionDescription(actionDescription, colorState, getServiceType()).build());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not set colorState", ex);
        }
    }

    @Override
    public Future<ActionFuture> setColorState(final ColorState colorState, final UnitType unitType) throws CouldNotPerformException {
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        serviceStateDescription.setUnitType(unitType);
        
        try {
            return applyAction(Service.upateActionDescription(actionDescription, colorState, getServiceType()).build());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not set colorState", ex);
        }
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        return getServiceState();
    }

    @Override
    public ColorState getColorState(final UnitType unitType) throws NotAvailableException {
        try {
            double averageRed = 0;
            double averageGreen = 0;
            double averageBlue = 0;
            int amount = getColorStateOperationServices().size();
            long timestamp = 0;
            Collection<ColorStateOperationService> colorStateOperationServiceCollection = getServices(unitType);
            for (ColorStateOperationService service : colorStateOperationServiceCollection) {
                if (!((UnitRemote) service).isDataAvailable()) {
                    amount--;
                    continue;
                }

                RGBColor rgbColor = HSBColorToRGBColorTransformer.transform(service.getColorState().getColor().getHsbColor());
                averageRed += rgbColor.getRed();
                averageGreen += rgbColor.getGreen();
                averageBlue += rgbColor.getBlue();
                timestamp = Math.max(timestamp, service.getColorState().getTimestamp().getTime());
            }
            averageRed = averageRed / amount;
            averageGreen = averageGreen / amount;
            averageBlue = averageBlue / amount;

            HSBColor hsbColor = HSBColorToRGBColorTransformer.transform(RGBColor.newBuilder().setRed((int) averageRed).setGreen((int) averageGreen).setBlue((int) averageBlue).build());
            return TimestampProcessor.updateTimestamp(timestamp, ColorState.newBuilder().setColor(ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(hsbColor)), TimeUnit.MICROSECONDS, logger).build();
        } catch (CouldNotTransformException ex) {
            throw new NotAvailableException("Could not transform from HSB to RGB or vice-versa!", ex);
        }
    }

    @Override
    public Future<ActionFuture> setNeutralWhite() throws CouldNotPerformException {
        List<Future> futureList = new ArrayList<>();
        for(ColorStateOperationService colorStateOperationService : getColorStateOperationServices()) {
            futureList.add(colorStateOperationService.setNeutralWhite());
        }
        return GlobalCachedExecutorService.allOf(ActionFuture.getDefaultInstance(), futureList);
    }
}
