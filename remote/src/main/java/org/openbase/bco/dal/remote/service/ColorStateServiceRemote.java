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
import java.awt.Color;
import java.util.Collection;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.collection.ColorStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.lib.transform.HSBColorToRGBColorTransformer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.TypeNotSupportedException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.vision.ColorType;
import rst.vision.HSBColorType.HSBColor;

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
    public Future<Void> setColorState(ColorState colorState) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf(getServices(), (ColorStateOperationService input) -> input.setColorState(colorState));
    }

    @Override
    public Future<Void> setColorState(final ColorState colorState, final UnitType unitType) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf(getServices(unitType), (ColorStateOperationService input) -> input.setColorState(colorState));
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
            Collection<ColorStateOperationService> colorStateOperationServicCollection = getServices(unitType);
            for (ColorStateOperationService service : colorStateOperationServicCollection) {
                if (!((UnitRemote) service).isDataAvailable()) {
                    amount--;
                    continue;
                }

                Color color = HSBColorToRGBColorTransformer.transform(service.getColorState().getColor().getHsbColor());
                averageRed += color.getRed();
                averageGreen += color.getGreen();
                averageBlue += color.getBlue();
                timestamp = Math.max(timestamp, service.getColorState().getTimestamp().getTime());
            }
            averageRed = averageRed / amount;
            averageGreen = averageGreen / amount;
            averageBlue = averageBlue / amount;

            HSBColor hsbColor = HSBColorToRGBColorTransformer.transform(new Color((int) averageRed, (int) averageGreen, (int) averageBlue));
            return TimestampProcessor.updateTimestamp(timestamp, ColorState.newBuilder().setColor(ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(hsbColor)), logger).build();
        } catch (CouldNotTransformException | TypeNotSupportedException ex) {
            throw new NotAvailableException("Could not transform from HSB to RGB or vice-versa!", ex);
        }
    }
}
