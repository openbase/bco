package org.openbase.bco.registry.unit.core.consistency.sceneconfig;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.scene.SceneConfigType.SceneConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This consistency handler makes sure that only one of several colliding service state descriptions is kept in a scene.
 * Required service state descriptions are always preferred over optional ones. Else the service hierarchy as defined
 * via service templates is consulted.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceTypeHiararchyConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {


    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, Builder> entry, ProtoBufMessageMap<String, UnitConfig, Builder> entryMap, ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();
        final SceneConfig.Builder sceneConfig = unitConfig.getSceneConfigBuilder();
        boolean modification = false;

        // iterate over all required service state descriptions
        for (int i = 0; i < sceneConfig.getRequiredServiceStateDescriptionCount(); i++) {
            final String unitId = sceneConfig.getRequiredServiceStateDescription(i).getUnitId();

            // remove all optional service state descriptions with the same unit id
            for (int j = 0; i < sceneConfig.getOptionalServiceStateDescriptionCount(); j++) {
                if (sceneConfig.getOptionalServiceStateDescription(j).getUnitId().equals(unitId)) {
                    sceneConfig.removeOptionalServiceStateDescription(j);
                    j--;
                    modification = true;
                }
            }

            // iterate over all following required service state descriptions
            if (i < sceneConfig.getRequiredServiceStateDescriptionCount() - 1) {
                // save all indices of service state descriptions with the same unit id
                final List<Integer> indexList = new ArrayList<>();
                indexList.add(i);
                for (int j = i + 1; j < sceneConfig.getRequiredServiceStateDescriptionCount(); j++) {
                    if (unitId.equals(sceneConfig.getRequiredServiceStateDescription(j).getUnitId())) {
                        indexList.add(j);
                    }
                }

                // if there is only one with this id continue
                if (indexList.size() == 1) {
                    continue;
                }

                // there are more than one so a modification will occur
                modification = true;
                // sort the list by comparing the services state descriptions in their importance
                indexList.sort(new Comparator<Integer>() {
                    @Override
                    public int compare(Integer o1, Integer o2) {
                        //TODO write comparator here which compares service state descriptions based on their type
                        return o1.compareTo(o2);
                    }
                });

                // the first one is the most important so remove all following service state descriptions
                for (int j = 1; j < indexList.size(); j++) {
                    sceneConfig.removeRequiredServiceStateDescription(j);
                    i--;
                }
            }
        }

        // iterate over all optional service state descriptions
        for (int i = 0; i < sceneConfig.getOptionalServiceStateDescriptionCount(); i++) {
            final String unitId = sceneConfig.getOptionalServiceStateDescription(i).getUnitId();

            // iterate over all following optional service state descriptions
            if (i < sceneConfig.getOptionalServiceStateDescriptionCount() - 1) {
                // save all indices of service state descriptions with the same unit id
                final List<Integer> indexList = new ArrayList<>();
                indexList.add(i);
                for (int j = i + 1; j < sceneConfig.getOptionalServiceStateDescriptionCount(); j++) {
                    if (unitId.equals(sceneConfig.getOptionalServiceStateDescription(j).getUnitId())) {
                        indexList.add(j);
                    }
                }

                // if there is only one with this id continue
                if (indexList.size() == 1) {
                    continue;
                }

                // there are more than one so a modification will occur
                modification = true;
                // sort the list by comparing the services state descriptions in their importance
                indexList.sort(new Comparator<Integer>() {
                    @Override
                    public int compare(Integer o1, Integer o2) {
                        //TODO write comparator here which compares service state descriptions based on their type
                        return o1.compareTo(o2);
                    }
                });

                // the first one is the most important so remove all following service state descriptions
                for (int j = 1; j < indexList.size(); j++) {
                    sceneConfig.removeOptionalServiceStateDescription(j);
                    i--;
                }
            }
        }

        if (modification) {
            throw new EntryModification(unitConfig, this);
        }
    }
}
