package org.openbase.bco.dal.remote.layer.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.collection.ColorStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.extension.type.transform.HSBColorToRGBColorTransformer;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.vision.ColorType;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.openbase.type.vision.RGBColorType.RGBColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ColorStateServiceRemote extends AbstractServiceRemote<ColorStateOperationService, ColorState> implements ColorStateOperationServiceCollection {

    public ColorStateServiceRemote() {
        super(ServiceType.COLOR_STATE_SERVICE, ColorState.class);
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
    public Future<ActionDescription> setColorState(final ColorState colorState, final UnitType unitType) {
        try {
            return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(colorState, getServiceType(), unitType));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    public ColorState getColorState(final UnitType unitType) throws NotAvailableException {
        try {
            double averageRed = 0;
            double averageGreen = 0;
            double averageBlue = 0;
            long timestamp = 0;
            final Collection<ColorStateOperationService> colorStateOperationServiceCollection = getServices(unitType);
            int amount = colorStateOperationServiceCollection.size();

            for (ColorStateOperationService service : colorStateOperationServiceCollection) {
                if (!((UnitRemote) service).isDataAvailable()
                        || !service.getColorState().hasColor()
                        || !service.getColorState().getColor().hasHsbColor()
                        || !service.getColorState().getColor().getHsbColor().hasHue()
                        || !service.getColorState().getColor().getHsbColor().hasSaturation()
                        || !service.getColorState().getColor().getHsbColor().hasBrightness()) {
                    amount--;
                    continue;
                }

                RGBColor rgbColor = HSBColorToRGBColorTransformer.transform(service.getColorState().getColor().getHsbColor());
                averageRed += rgbColor.getRed();
                averageGreen += rgbColor.getGreen();
                averageBlue += rgbColor.getBlue();
                timestamp = Math.max(timestamp, service.getColorState().getTimestamp().getTime());
            }

            if (amount == 0) {
                throw new NotAvailableException("ColorState");
            }

            averageRed = averageRed / amount;
            averageGreen = averageGreen / amount;
            averageBlue = averageBlue / amount;

            HSBColor hsbColor = HSBColorToRGBColorTransformer.transform(RGBColor.newBuilder().setRed(averageRed).setGreen(averageGreen).setBlue(averageBlue).build());
            return TimestampProcessor.updateTimestamp(timestamp, ColorState.newBuilder().setColor(ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(hsbColor)), TimeUnit.MICROSECONDS, logger).build();
        } catch (CouldNotTransformException ex) {
            throw new NotAvailableException("Could not transform from HSB to RGB or vice-versa!", ex);
        }
    }

    @Override
    public Future<ActionDescription> setNeutralWhite() {
        List<Future<?>> futureList = new ArrayList<>();
        for(ColorStateOperationService colorStateOperationService : getServices()) {
            futureList.add(colorStateOperationService.setNeutralWhite());
        }
        return FutureProcessor.allOf(ActionDescription.getDefaultInstance(), futureList);
    }
}
