package org.openbase.bco.dal.remote.printer;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.ProtocolMessageEnum;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.preset.JPTmpDirectory;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.pattern.Filter;
import org.openbase.jul.pattern.consumer.Consumer;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class UnitModelPrinter {


    private static final Logger LOGGER = LoggerFactory.getLogger(UnitModelPrinter.class);

    public static File getModelFile() throws NotAvailableException {
        try {
            return new File(JPService.getProperty(JPTmpDirectory.class).getValue(), "bco-model.pl");
        } catch (JPNotAvailableException ex) {
            throw new NotAvailableException("File", "Model", ex);
        }
    }

    public static PrintStream getModelPrintStream() throws NotAvailableException {
        try {
            return new PrintStream(new FileOutputStream(getModelFile(), false));
        } catch (FileNotFoundException | NotAvailableException ex) {
            throw new NotAvailableException("PrintStream", "Model", ex);
        }
    }

    public static void printStaticRelations(final PrintStream printStream) {
        printStaticRelations(data -> printStream.println(data), true);
    }

    public static void printStaticRelations(final Consumer<String> outputConsumer, final boolean printHeader) {
        try {
            // print unit templates
            if (printHeader) {
                outputConsumer.consume("/**\n" +
                        " * Unit Templates\n" +
                        " * --> syntax: unit_template(unit_type, [service_type]).\n" +
                        " */");
            }
            for (UnitTemplate unitTemplate : Registries.getTemplateRegistry(true).getUnitTemplates()) {
                outputConsumer.consume("unit_template("
                        + unitTemplate.getType().name().toLowerCase() + ", ["
                        + StringProcessor.transformCollectionToString(
                        unitTemplate.getServiceDescriptionList(),
                        serviceDescription -> serviceDescription.getServiceType().name().toLowerCase(),
                        ", ",
                        (ServiceDescription sd) -> sd.getPattern() != ServicePattern.PROVIDER)
                        + "]).");
            }

            // print service type mapping
            if (printHeader) {
                outputConsumer.consume("");
                outputConsumer.consume("/**\n" +
                        " * Service Templates\n" +
                        " * --> syntax: service_template(service_type, [service_state_values], physical_properties).\n" +
                        " */");
            }
            for (ServiceTemplate serviceTemplate : Registries.getTemplateRegistry(true).getServiceTemplates()) {
                // detect physical properties
                String physicalProperties;
                try {
                    physicalProperties = new MetaConfigVariableProvider("ServiceTemplate", serviceTemplate.getMetaConfig()).getValue("PHYSICAL_PROPERTIES");
                } catch (CouldNotPerformException ex) {
                    // if not available just leave it empty
                    physicalProperties = "";
                }
                try {
                    // print discrete service state values
                    outputConsumer.consume("service_template("
                            + serviceTemplate.getType().name().toLowerCase() + ", [" + StringProcessor.transformCollectionToString(
                            Services.getServiceStateEnumValues(serviceTemplate.getType())
                            , (ProtocolMessageEnum o) -> o.getValueDescriptor().getName().toLowerCase(),
                            ", ",
                            type -> type.getValueDescriptor().getName().equals("UNKNOWN")) + "], " +
                            "[" + physicalProperties + "]).");
                } catch (CouldNotPerformException ex) {
                    try {
                        // print continuous service state values
                        outputConsumer.consume("service_template(" +
                                serviceTemplate.getType().name().toLowerCase() + ", [" + StringProcessor.transformCollectionToString(
                                Services.getServiceStateFieldDataTypes(serviceTemplate.getType()),
                                (String o) -> o.toLowerCase(),
                                ", ") + "], " +
                                "[" + physicalProperties + "]).");
                    } catch (CouldNotPerformException exx) {
                        try {
                            MultiException.checkAndThrow(() -> "Skip ServiceState[" + serviceTemplate.getType().name() + "]", MultiException.push(UnitModelPrinter.class, ex, MultiException.push(UnitModelPrinter.class, exx, null)));
                        } catch (CouldNotPerformException exxx) {
                            ExceptionPrinter.printHistory(exxx, LOGGER, LogLevel.WARN);
                        }
                    }
                }
            }

            // print units
            if (printHeader) {
                outputConsumer.consume("");
                outputConsumer.consume("/**\n" +
                        " * Units\n" +
                        " * --> syntax: unit(unit_id, unit_alias, unit_type, parent_location, [labels], [operation_services], [provider_services]).\n" +
                        " */");
            }
            for (UnitConfig unitConfig : Registries.getUnitRegistry(true).getUnitConfigs()) {
                outputConsumer.consume( "unit("
                        + "'" + unitConfig.getId() + "', "
                        + "'" + unitConfig.getAlias(0) + "', "
                        + "'" + unitConfig.getUnitType().name().toLowerCase() + "', "
                        + "'" + unitConfig.getPlacementConfig().getLocationId() + "', ["
                        + StringProcessor.transformCollectionToString(unitConfig.getLabel().getEntryList(), mapFieldEntry -> mapFieldEntry.getKey() + "='" + mapFieldEntry.getValue(0) + "'", ", ") + "], ["
                        + StringProcessor.transformCollectionToString(unitConfig.getServiceConfigList(), serviceConfig -> "'" + serviceConfig.getServiceDescription().getServiceType().name().toLowerCase() + "'", ", ", (Filter<ServiceConfig>) serviceConfig -> serviceConfig.getServiceDescription().getPattern() == ServicePattern.OPERATION) + "], ["
                        + StringProcessor.transformCollectionToString(unitConfig.getServiceConfigList(), serviceConfig -> "'" + serviceConfig.getServiceDescription().getServiceType().name().toLowerCase() + "'", ", ", (Filter<ServiceConfig>) serviceConfig -> serviceConfig.getServiceDescription().getPattern() == ServicePattern.PROVIDER) + "]"
                        + ")."
                );
            }

            // print locations
            if (printHeader) {
                outputConsumer.consume("");
                outputConsumer.consume("/**\n" +
                        " * Locations\n" +
                        " * --> syntax: location(unit_id, unit_alias, location_type, [labels]).\n" +
                        " */");
            }
            for (UnitConfig unitConfig : Registries.getUnitRegistry(true).getUnitConfigs(UnitType.LOCATION)) {
                outputConsumer.consume(unitConfig.getUnitType().name().toLowerCase() + "("
                        + "'" + unitConfig.getId() + "', "
                        + "'" + unitConfig.getAlias(0) + "', "
                        + "'" + unitConfig.getLocationConfig().getType().name().toLowerCase() + "', ["
                        + StringProcessor.transformCollectionToString(
                        unitConfig.getLabel().getEntryList(),
                        mapFieldEntry -> mapFieldEntry.getKey() + "='" + mapFieldEntry.getValue(0) + "'",
                        ", ")
                        + "]).");
            }

            // print connections
            if (printHeader) {
                outputConsumer.consume("");
                outputConsumer.consume("/**\n" +
                        " * Connections\n" +
                        " * --> syntax: connection(unit_id, unit_alias, connection_type, [labels], [locations]).\n" +
                        " */");
            }
            for (UnitConfig unitConfig : Registries.getUnitRegistry(true).getUnitConfigs(UnitType.CONNECTION)) {
                outputConsumer.consume(unitConfig.getUnitType().name().toLowerCase() + "("
                        + "'" + unitConfig.getId() + "', "
                        + "'" + unitConfig.getAlias(0) + "', "
                        + "'" + unitConfig.getConnectionConfig().getType().name().toLowerCase() + "', ["
                        + StringProcessor.transformCollectionToString(
                        unitConfig.getLabel().getEntryList(),
                        mapFieldEntry -> mapFieldEntry.getKey() + "='" + mapFieldEntry.getValue(0) + "'",
                        ", ")
                        + "], ["
                        + StringProcessor.transformCollectionToString(
                        unitConfig.getConnectionConfig().getTileIdList(),
                        tile_id -> {
                            try {
                                return "'" + Registries.getUnitRegistry().getUnitConfigById(tile_id, UnitType.LOCATION).getId() + "'";
                            } catch (CouldNotPerformException e) {
                                return "na";
                            }
                        },
                        ", ")
                        + "]).");
            }
            if (printHeader) {
                outputConsumer.consume("");
            }
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory("Could not print unit templates.", ex, LOGGER);
        }
    }
}
