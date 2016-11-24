package org.openbase.bco.dal.lib.layer.unit.unitgroup;

import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.jul.iface.Identifiable;

/**
 * #%L
 * #L%
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UnitGroup extends BrightnessStateOperationService, ColorStateOperationService, PowerStateOperationService, BlindStateOperationService, StandbyStateOperationService, TargetTemperatureStateOperationService, Identifiable<String> {

}
