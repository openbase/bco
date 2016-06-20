package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.dal.lib.layer.unit.AmbientLightInterface;
import org.openbase.bco.dal.lib.transform.HSVColorToRGBColorTransformer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.AmbientLightType;
import rst.vision.HSVColorType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author mpohling
 */
public class AmbientLightRemote extends AbstractUnitRemote<AmbientLightType.AmbientLight> implements AmbientLightInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AmbientLightType.AmbientLight.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSVColorType.HSVColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    public AmbientLightRemote() {
        super(AmbientLightType.AmbientLight.class);
    }

    public Future<Void> setColor(final java.awt.Color color) throws CouldNotPerformException {
        return setColor(HSVColorToRGBColorTransformer.transform(color));
    }

    @Override
    public Future<Void> setColor(final HSVColor value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public Future<Void> setBrightness(Double value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public PowerState getPower() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerState", ex);
        }
    }

    @Override
    public Future<Void> setPower(PowerState value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public HSVColor getColor() throws NotAvailableException {
        try {
            return getData().getColor();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Color", ex);
        }
    }

    @Override
    public Double getBrightness() throws NotAvailableException {
        try {
            return this.getData().getColor().getValue();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Brightness", ex);
        }
    }
}
