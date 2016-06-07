package org.dc.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import org.dc.bco.dal.lib.layer.service.operation.ColorOperationService;
import org.dc.bco.dal.lib.transform.HSVColorToRGBColorTransformer;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.schedule.GlobalExecutionService;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.com">Marian Pohling</a>
 */
public interface ColorStateOperationServiceCollection extends ColorOperationService {

    /**
     *
     * @param color
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public Future<Void> setColor(final HSVColor color) throws CouldNotPerformException {
        return GlobalExecutionService.allOf((ColorOperationService input) -> input.setColor(color), getColorStateOperationServices());
    }

    /**
     * Returns the average rgb value for a collection of color services.
     * 
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public HSVColor getColor() throws NotAvailableException {
        try {
            double averageRed = 0;
            double averageGreen = 0;
            double averageBlue = 0;
            int amount = getColorStateOperationServices().size();
            Collection<ColorOperationService> colorStateOperationServicCollection = getColorStateOperationServices();
            for (ColorOperationService service : colorStateOperationServicCollection) {
                Color color = HSVColorToRGBColorTransformer.transform(service.getColor());
                averageRed += color.getRed();
                averageGreen += color.getGreen();
                averageBlue += color.getBlue();
            }
            averageRed = averageRed / amount;
            averageGreen = averageGreen / amount;
            averageBlue = averageBlue / amount;

            return HSVColorToRGBColorTransformer.transform(new Color((int) averageRed, (int) averageGreen, (int) averageBlue));
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("HSVColor", ex);
        }
    }

    public Collection<ColorOperationService> getColorStateOperationServices() throws CouldNotPerformException;
}
