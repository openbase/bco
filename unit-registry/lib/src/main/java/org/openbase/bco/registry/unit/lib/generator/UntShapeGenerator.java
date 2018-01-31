package org.openbase.bco.registry.unit.lib.generator;

import org.openbase.bco.registry.lib.provider.DeviceClassCollectionProvider;
import org.openbase.bco.registry.lib.provider.UnitConfigCollectionProvider;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.spatial.ShapeType.Shape;

public class UntShapeGenerator {

    /**
     * Method returns the unit shape of the given unit referred by the unit config.
     * <p>
     * If this unit configuration does not provide any shape information the shape of the unit host will be returned.
     * In case the unit host even does not provide any shape information and the unit is a device than the shape of the device class will be used.
     *
     * @param unitConfig the unit config to generate the shape for.
     * @param unitConfigCollectionProvider provider to resolve the unit config by id.
     * @param deviceClassCollectionProvider provider to resolve the unit class by id.
     * @return the shape representing the unit.
     * @throws NotAvailableException is thrown if the unit shape is not available or the resolution has been failed.
     */
    public static Shape generateUnitShape(final UnitConfig unitConfig, final UnitConfigCollectionProvider unitConfigCollectionProvider, final DeviceClassCollectionProvider deviceClassCollectionProvider) throws NotAvailableException {
        try {
            // resolve shape via unit config
            if (unitConfig.hasPlacementConfig() && unitConfig.getPlacementConfig().hasShape()) {
                Shape shape = unitConfig.getPlacementConfig().getShape();
                if (shape.hasBoundingBox() || shape.getCeilingCount() != 0 || shape.getFloorCount() != 0 || shape.getFloorCeilingEdgeCount() != 0) {
                    // Only if shape is not empty!
                    return unitConfig.getPlacementConfig().getShape();
                }
            }

            // resolve shape via unit host
            if (unitConfig.hasUnitHostId()) {
                return generateUnitShape(unitConfig.getUnitHostId(), unitConfigCollectionProvider, deviceClassCollectionProvider);
            }

            // resolve shape via device class
            if (unitConfig.getType().equals(UnitType.DEVICE)) {
                return deviceClassCollectionProvider.getDeviceClassById(unitConfig.getDeviceConfig().getDeviceClassId()).getShape();
            }

            // inform that the resolution is not possible.
            throw new CouldNotPerformException("Shape could not be resolved by any source.");

        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("Shape", "of Unit [" + unitConfig.getLabel() + "]", ex);
        }
    }

    /**
     * Method returns the unit shape of the given unit referred by the id.
     * <p>
     * If this unit configuration does not provide any shape information the shape of the unit host will be returned.
     * In case the unit host even does not provide any shape information and the unit is a device than the shape of the device class will be used.
     *
     * @param unitId the id to resolve the unit shape.
     * @param unitConfigCollectionProvider provider to resolve the unit config by id.
     * @param deviceClassCollectionProvider provider to resolve the unit class by id.
     * @return the shape representing the unit.
     * @throws NotAvailableException is thrown if the unit shape is not available or the resolution has been failed.
     */
    public static Shape generateUnitShape(final String unitId, final UnitConfigCollectionProvider unitConfigCollectionProvider, final DeviceClassCollectionProvider deviceClassCollectionProvider) throws NotAvailableException {
        try {
            return generateUnitShape(unitConfigCollectionProvider.getUnitConfigById(unitId), unitConfigCollectionProvider, deviceClassCollectionProvider);
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("Shape", "of unit " + unitId, ex);
        }
    }
}
