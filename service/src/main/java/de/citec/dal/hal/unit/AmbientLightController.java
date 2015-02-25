/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.BrightnessService;
import de.citec.dal.hal.service.ColorService;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerType;
import rst.homeautomation.unit.AmbientLightType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author Tamino Huxohl
 * @author Marian Pohling
 */
public class AmbientLightController extends AbstractUnitController<AmbientLightType.AmbientLight, AmbientLightType.AmbientLight.Builder> implements AmbientLightInterface {

	static {
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AmbientLightType.AmbientLight.getDefaultInstance()));
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSVColor.getDefaultInstance()));
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
	}

	private final ColorService colorService;
	private final BrightnessService brightnessService;
	private final PowerService powerService;

	public AmbientLightController(final String label, final Device device, final AmbientLightType.AmbientLight.Builder builder) throws InstantiationException {
		this(label, device, builder, device.getDefaultServiceFactory());
	}

	public AmbientLightController(final String label, final Device device, final AmbientLightType.AmbientLight.Builder builder, final ServiceFactory serviceFactory) throws InstantiationException {
		super(AmbientLightController.class, label, device, builder);
		this.powerService = serviceFactory.newPowerService(device, this);
		this.colorService = serviceFactory.newColorService(device, this);
		this.brightnessService = serviceFactory.newBrightnessService(device, this);
	}

	public void updatePower(final PowerType.Power.PowerState state) {
		logger.debug("Update " + name + "[" + label + "] to PowerState [" + state.name() + "]");
		data.getPowerStateBuilder().setState(state);
		notifyChange();
	}

	@Override
	public void setPower(final PowerType.Power.PowerState state) throws CouldNotPerformException {
		logger.debug("Set " + name + "[" + label + "] to PowerState [" + state.name() + "]");
		powerService.setPower(state);
	}

	@Override
	public PowerType.Power.PowerState getPower() throws CouldNotPerformException {
		return data.getPowerState().getState();
	}

	public void updateColor(final HSVColor color) {
		data.setColor(color);
		notifyChange();
	}

	@Override
	public void setColor(final HSVColor color) throws CouldNotPerformException {
		logger.debug("Set " + name + "[" + label + "] to HSVColor[" + color.getHue() + "|" + color.getSaturation() + "|" + color.getValue() + "]");
		colorService.setColor(color);
	}

	@Override
	public HSVColor getColor() {
		return data.getColor();
	}

	public void updateBrightness(Double value) {
		updateColor(data.getColor().newBuilderForType().setValue(value).build());
	}

	@Override
	public void setBrightness(Double brightness) throws CouldNotPerformException {
		logger.debug("Set " + name + "[" + label + "] to Brightness[" + brightness + "]");
		brightnessService.setBrightness(brightness);
	}

	@Override
	public Double getBrightness() {
		return data.getColor().getValue();
	}

}
