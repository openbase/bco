package org.openbase.bco.registry.tools;

import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class CLRegistryQuery {

    /**
     * This is a command line tool to query registry entries.
     * Currently this is just a prototype implementation.
     *
     * TODOs
     * - JPService should be used in a future release.
     * - Detail usage page should be generated.
     * - Implement more generic on protobuf types.
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            if ("type".equals(args[0])) {
                printUnitIdByType(UnitTemplateType.UnitTemplate.UnitType.valueOf(args[1]));
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not query!", ex), System.err);
        }
    }

    public static void printUnitIdByType(final UnitType unitType) throws InterruptedException, CouldNotPerformException {
        for (UnitConfig unitConfig : CachedDeviceRegistryRemote.getRegistry().getUnitConfigs()) {
            if (unitConfig.getType().equals(unitType)) {
                System.out.println(unitConfig.getId());
            }
        }
    }
}
