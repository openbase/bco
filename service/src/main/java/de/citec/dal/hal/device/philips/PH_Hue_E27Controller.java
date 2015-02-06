/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.philips;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.AbstractUnitController;
import de.citec.dal.hal.unit.AmbientLightController;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.philips.PH_Hue_E27Type;
import rst.devices.philips.PH_Hue_E27Type.PH_Hue_E27;

/**
 *
 * @author mpohling
 */
public class PH_Hue_E27Controller extends AbstractOpenHABDeviceController<PH_Hue_E27, PH_Hue_E27.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PH_Hue_E27Type.PH_Hue_E27.getDefaultInstance()));
    }

    public PH_Hue_E27Controller(final String id, final String label, final Location location) throws InstantiationException {
        super(id, label, location, PH_Hue_E27.newBuilder());
        this.registerUnit(new AmbientLightController(label, this, data.getAmbientLightBuilder(), getDefaultServiceFactory()));
    }

//    @Override
//    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
//        halFunctionMapping.put(COMPONENT_AMBIENT_LIGHT, getClass().getMethod("updateAmbientLight", HSB.class));
//        halFunctionMapping.put(COMPONENT_POWER_SWITCH, getClass().getMethod("updatePowerSwitch", OnOff.class));
//    }

    public void requestUpdate(AbstractUnitController unit, Object data) {

    }

//    public void updateAmbientLight(final HSB type) throws CouldNotPerformException {
//        try {
//            ambientLight.updateColor(HSVColorTransformer.transform(type));
//        } catch (CouldNotTransformException ex) {
//            throw new CouldNotPerformException("Could not updateAmbientLight!", ex);
//        }
//    }
//
//    public void updatePowerSwitch(final OnOff type) throws CouldNotPerformException {
//        try {
//            ambientLight.updatePowerState(PowerStateTransformer.transform(type));
//        } catch (CouldNotTransformException ex) {
//            throw new CouldNotPerformException("Could not updatePowerSwitch!", ex);
//        }
//    }
}
