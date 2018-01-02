package org.openbase.bco.manager.device.binding.openhab.execution;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.openhab.binding.transform.OpenhabCommandTransformer;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.domotic.binding.openhab.OpenhabCommandType;
import rst.domotic.binding.openhab.OpenhabCommandType.OpenhabCommand;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class OpenHABCommandExecutor {

    public static final String ITEM_SUBSEGMENT_DELIMITER = "_";
    public static final String ITEM_SEGMENT_DELIMITER = "__";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenHABCommandExecutor.class);

    private final UnitControllerRegistry unitControllerRegistry;

    public OpenHABCommandExecutor(UnitControllerRegistry unitControllerRegistry) {
        this.unitControllerRegistry = unitControllerRegistry;
    }

    private class OpenhabCommandMetaData {

        private final OpenhabCommandType.OpenhabCommand command;
        private final ServiceTemplate.ServiceType serviceType;
        private final String unitScope;
        private final String locationId;

        public OpenhabCommandMetaData(OpenhabCommand command) throws CouldNotPerformException {
            this.command = command;

            try {
                String[] nameSegment = command.getItem().split(ITEM_SEGMENT_DELIMITER);
                try {
                    locationId = nameSegment[1].replace(ITEM_SUBSEGMENT_DELIMITER, Scope.COMPONENT_SEPARATOR);
                } catch (IndexOutOfBoundsException | NullPointerException ex) {
                    throw new CouldNotPerformException("Could not extract location id out of item name!");
                }
                try {
                    this.unitScope = (Scope.COMPONENT_SEPARATOR + locationId + Scope.COMPONENT_SEPARATOR + nameSegment[2] + Scope.COMPONENT_SEPARATOR + nameSegment[3] + Scope.COMPONENT_SEPARATOR).toLowerCase();
                } catch (IndexOutOfBoundsException | NullPointerException ex) {
                    throw new CouldNotPerformException("Could not extract unit id out of item name!");
                }
                try {
                    serviceType = ServiceTemplate.ServiceType.valueOf(StringProcessor.transformToUpperCase(nameSegment[4]));
                } catch (IndexOutOfBoundsException | NullPointerException ex) {
                    throw new CouldNotPerformException("Could not extract service type out of item name!", ex);
                }
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not extract meta data out of openhab command because Item[" + command.getItem() + "] not compatible!", ex);
            }
        }

        public OpenhabCommand getCommand() {
            return command;
        }

        public ServiceTemplate.ServiceType getServiceType() {
            return serviceType;
        }

        public String getUnitScope() {
            return unitScope;
        }

        public String getLocationId() {
            return locationId;
        }
    }

    public void receiveUpdate(OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
        LOGGER.info("receiveUpdate [" + command.getItem() + "=" + command.getType() + "]");
        OpenhabCommandMetaData metaData = new OpenhabCommandMetaData(command);
        Object serviceData = OpenhabCommandTransformer.getServiceData(command, metaData.getServiceType());

        if (serviceData == null) {
            return;
        }

        final UnitController unitController;
        try {
            unitController = unitControllerRegistry.getUnitByScope(metaData.getUnitScope());
        } catch (NotAvailableException ex) {
            if (!unitControllerRegistry.isInitiallySynchronized()) {
                LOGGER.debug("ItemUpdate[" + command.getItem() + "=" + command.getType() + "] skipped because controller registry was not ready yet!");
                return;
            }
            throw ex;
        }
        unitController.applyDataUpdate(serviceData, metaData.getServiceType());
    }
}
