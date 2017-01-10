package org.openbase.bco.dal.lib.layer.service.operation;

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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.provider.ColorStateProviderService;
import org.openbase.bco.dal.lib.transform.HSBColorToRGBColorTransformer;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.state.ColorStateType.ColorState;
import rst.vision.ColorType.Color;
import rst.vision.HSBColorType.HSBColor;
import rst.vision.RGBColorType.RGBColor;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface ColorStateOperationService extends OperationService, ColorStateProviderService {

    public Future<Void> setColorState(final ColorState colorState) throws CouldNotPerformException;

    default public Future<Void> setColor(final Color color) throws CouldNotPerformException {
        return setColorState(ColorState.newBuilder().setColor(color).build());
    }

    default public Future<Void> setColor(final HSBColor color) throws CouldNotPerformException {
        return setColor(Color.newBuilder().setType(Color.Type.HSB).setHsbColor(color).build());
    }

    default public Future<Void> setColor(final RGBColor color) throws CouldNotPerformException {
        return setColor(Color.newBuilder().setType(Color.Type.RGB).setRgbColor(color).build());
    }
    
    default public Future<Void> setColor(final java.awt.Color color) throws CouldNotPerformException {
        return setColor(HSBColorToRGBColorTransformer.transform(color));
    }
}
