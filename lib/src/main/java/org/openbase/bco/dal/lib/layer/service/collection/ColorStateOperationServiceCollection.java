package org.openbase.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * BCO DAL Library
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
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.transform.HSBColorToRGBColorTransformer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.state.ColorStateType.ColorState;
import rst.vision.ColorType;
import rst.vision.HSBColorType.HSBColor;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.com">Marian Pohling</a>
 */
public interface ColorStateOperationServiceCollection extends ColorStateOperationService {

    /**
     *
     * @param colorState
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public Future<Void> setColorState(final ColorState colorState) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf((ColorStateOperationService input) -> input.setColorState(colorState), getColorStateOperationServices());
    }

    //TODO: is implemented in the service remotes but still used in the LocationController because else it would lead to too many unitRemots
    //remove when remote cashing is implemented
    /**
     * Returns the average rgb value for a collection of color services.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public ColorState getColorState() throws NotAvailableException {
        try {
            double averageRed = 0;
            double averageGreen = 0;
            double averageBlue = 0;
            int amount = getColorStateOperationServices().size();
            Collection<ColorStateOperationService> colorStateOperationServicCollection = getColorStateOperationServices();
            for (ColorStateOperationService service : colorStateOperationServicCollection) {
                Color color = HSBColorToRGBColorTransformer.transform(service.getColorState().getColor().getHsbColor());
                averageRed += color.getRed();
                averageGreen += color.getGreen();
                averageBlue += color.getBlue();
            }
            averageRed = averageRed / amount;
            averageGreen = averageGreen / amount;
            averageBlue = averageBlue / amount;

            HSBColor hsbColor = HSBColorToRGBColorTransformer.transform(new Color((int) averageRed, (int) averageGreen, (int) averageBlue));
            ColorType.Color color = ColorType.Color.newBuilder().setHsbColor(hsbColor).setType(ColorType.Color.Type.HSB).build();
            return ColorState.newBuilder().setColor(color).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("HSBColor", ex);
        }
    }

    public Collection<ColorStateOperationService> getColorStateOperationServices() throws CouldNotPerformException;
}
