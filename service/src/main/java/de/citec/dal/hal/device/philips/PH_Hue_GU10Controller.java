/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.philips;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.bindings.openhab.transform.HSVColorTransformer;
import de.citec.dal.bindings.openhab.transform.PowerStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.unit.AmbientLightController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.philips.PH_Hue_GU10Type;
import rst.devices.philips.PH_Hue_GU10Type.PH_Hue_GU10;
import rst.homeautomation.openhab.HSBType.HSB;
import rst.homeautomation.openhab.OnOffHolderType.OnOffHolder.OnOff;

/**
 *
 * @author mpohling
 */
public class PH_Hue_GU10Controller extends AbstractOpenHABDeviceController<PH_Hue_GU10, PH_Hue_GU10.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PH_Hue_GU10Type.PH_Hue_GU10.getDefaultInstance()));
    }

    private final AmbientLightController ambientLight;

    public PH_Hue_GU10Controller(final String id, final String label,final Location location) throws InstantiationException  {
        super(id, label,location, PH_Hue_GU10.newBuilder());
        super.data.setId(id);
        this.ambientLight = new AmbientLightController(label,this, data.getAmbientLightBuilder());
        this.registerUnit(ambientLight);
    }

    public void updateAmbientLight(final HSB type) throws RSBBindingException {
        try {
            ambientLight.updateColor(HSVColorTransformer.transform(type));
        } catch (CouldNotPerformException ex) {
            logger.error("Could not updateAmbientLight!", ex);
        }
    }

    public void updatePowerSwitch(final OnOff type) throws RSBBindingException {
        try {
            ambientLight.updatePowerState(PowerStateTransformer.transform(type));
        } catch (CouldNotPerformException ex) {
            logger.error("Not able to transform from OnOffType to PowerState!", ex);
        }
    }
}
