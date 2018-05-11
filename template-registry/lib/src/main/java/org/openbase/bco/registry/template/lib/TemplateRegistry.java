package org.openbase.bco.registry.template.lib;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.pattern.provider.DataProvider;
import rst.domotic.activity.ActivityTemplateType.ActivityTemplate;
import rst.domotic.activity.ActivityTemplateType.ActivityTemplate.ActivityType;
import rst.domotic.registry.TemplateRegistryDataType.TemplateRegistryData;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.List;
import java.util.concurrent.Future;

public interface TemplateRegistry extends DataProvider<TemplateRegistryData>, Shutdownable {

    // ===================================== UnitTemplate Methods =============================================================

    /**
     * Method updates the given unit template.
     *
     * @param unitTemplate the updated unit template.
     * @return the updated unit template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    /**
     * Method returns true if the unit template with the given id is
     * registered, otherwise false. The unit template id field is used for the
     * comparison.
     *
     * @param unitTemplate the unit template which is tested
     * @return if the unit template with the given id is registered, otherwise false
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean containsUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    /**
     * Method returns true if the unit template with the given id is
     * registered, otherwise false.
     *
     * @param unitTemplateId the id of the unit template
     * @return if the unit template with the given id is registered, otherwise false
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean containsUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException;

    /**
     * Method returns the unit template which is registered with the given
     * id.
     *
     * @param unitTemplateId the id of the unit template
     * @return the requested unit template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    UnitTemplate getUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException;

    /**
     * Method returns all registered unit template.
     *
     * @return the unit templates stored in this registry.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException;

    /**
     * Method returns the unit template with the given type.
     *
     * @param unitType the unit type
     * @return the requested unit template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    UnitTemplate getUnitTemplateByType(final UnitType unitType) throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as read only.
     *
     * @return if the unit template registry is read only
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as consistent.
     *
     * @return if the unit template registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException;

    // ===================================== ServiceTemplate Methods =============================================================

    /**
     * Method updates the given service template.
     *
     * @param serviceTemplate the updated service template.
     * @return the updated service template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<ServiceTemplate> updateServiceTemplate(final ServiceTemplate serviceTemplate) throws CouldNotPerformException;

    /**
     * Method returns true if the service template with the given id is
     * registered, otherwise false. The service template id field is used for the
     * comparison.
     *
     * @param serviceTemplate the service template which is tested
     * @return if the service template with the given id is registered, otherwise false
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean containsServiceTemplate(final ServiceTemplate serviceTemplate) throws CouldNotPerformException;

    /**
     * Method returns true if the service template with the given id is
     * registered, otherwise false.
     *
     * @param serviceTemplateId the id of the service template
     * @return if the service template with the given id is registered, otherwise false
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean containsServiceTemplateById(final String serviceTemplateId) throws CouldNotPerformException;

    /**
     * Method returns the service template which is registered with the given
     * id.
     *
     * @param serviceTemplateId the id of the service template
     * @return the requested service template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    ServiceTemplate getServiceTemplateById(final String serviceTemplateId) throws CouldNotPerformException;

    /**
     * Method returns all registered service template.
     *
     * @return the service templates stored in this registry.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<ServiceTemplate> getServiceTemplates() throws CouldNotPerformException;

    /**
     * Method returns the service template with the given type.
     *
     * @param serviceType the service type
     * @return the requested service template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    ServiceTemplate getServiceTemplateByType(final ServiceType serviceType) throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as consistent.
     *
     * @return if the service template registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isServiceTemplateRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as consistent.
     *
     * @return if the unit template registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isServiceTemplateRegistryConsistent() throws CouldNotPerformException;

    // ===================================== ActivityTemplate Methods =============================================================

    /**
     * Method updates the given activity template.
     *
     * @param activityTemplate the updated activity template.
     * @return the updated activity template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<ActivityTemplate> updateActivityTemplate(ActivityTemplate activityTemplate) throws CouldNotPerformException;

    /**
     * Method returns true if the activity template with the given id is
     * registered, otherwise false. The activity template id field is used for the
     * comparison.
     *
     * @param activityTemplate the activity template which is tested
     * @return if the activity template with the given id is registered, otherwise false
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean containsActivityTemplate(ActivityTemplate activityTemplate) throws CouldNotPerformException;

    /**
     * Method returns true if the activity template with the given id is
     * registered, otherwise false.
     *
     * @param activityTemplateId the id of the activity template
     * @return if the activity template with the given id is registered, otherwise false
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean containsActivityTemplateById(String activityTemplateId) throws CouldNotPerformException;

    /**
     * Method returns the activity template which is registered with the given
     * id.
     *
     * @param activityTemplateId the id of the activity template
     * @return the requested activity template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    ActivityTemplate getActivityTemplateById(final String activityTemplateId) throws CouldNotPerformException;

    /**
     * Method returns all registered activity template.
     *
     * @return the activity templates stored in this registry.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<ActivityTemplate> getActivityTemplates() throws CouldNotPerformException;

    /**
     * Method returns the activity template with the given type.
     *
     * @param activityType the activity type
     * @return the requested activity template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    ActivityTemplate getActivityTemplateByType(final ActivityType activityType) throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as consistent.
     *
     * @return if the activity template registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isActivityTemplateRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as consistent.
     *
     * @return if the activity template registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isActivityTemplateRegistryConsistent() throws CouldNotPerformException;
}
