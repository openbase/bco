/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.util.configgen;

import static de.citec.dal.bindings.openhab.util.configgen.OpenHABItemConfigGenerator.TAB_SIZE;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.MultiException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.processing.StringProcessor;
import de.citec.jul.processing.VariableProcessor;
import de.citec.jul.processing.VariableProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import rst.configuration.EntryType.Entry;
import rst.configuration.MetaConfigType.MetaConfig;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.OpenHABBindingServiceConfigType.OpenHABBindingServiceConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.service.ServiceTypeHolderType;
import rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.BATTERY_PROVIDER;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.BRIGHTNESS_PROVIDER;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.BRIGHTNESS_SERVICE;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.BUTTON_PROVIDER;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.COLOR_SERVICE;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.DIM_PROVIDER;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.DIM_SERVICE;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.HANDLE_PROVIDER;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.MOTION_PROVIDER;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.OPENING_RATIO_PROVIDER;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.POWER_CONSUMPTION_PROVIDER;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.POWER_PROVIDER;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.POWER_SERVICE;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.REED_SWITCH_PROVIDER;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.SHUTTER_PROVIDER;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.SHUTTER_SERVICE;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.TAMPER_PROVIDER;
import static rst.homeautomation.service.ServiceTypeHolderType.ServiceTypeHolder.ServiceType.TEMPERATURE_PROVIDER;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;

/**
 *
 * @author mpohling
 */
public class ItemEntry {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ItemEntry.class);

    public String SERVICE_TEMPLATE_BINDING_TYPE = "OPENHAB_BINDING_TYPE";
    public String SERVICE_TEMPLATE_BINDING_ICON = "OPENHAB_BINDING_ICON";
    public String SERVICE_TEMPLATE_BINDING_COMMAND = "OPENHAB_BINDING_COMMAND";
    public String SERVICE_TEMPLATE_BINDING_CONFIG = "OPENHAB_BINDING_CONFIG";

    private String commandType;
    private final String itemId;
    private final String label;
    private String icon;
    private final List<String> groups;
    private final String bindingConfig;
    private final MetaConfigPool configPool;

    private static int maxCommandTypeSize = 0;
    private static int maxItemIdSize = 0;
    private static int maxLabelSize = 0;
    private static int maxIconSize = 0;
    private static int maxGroupSize = 0;
    private static int maxBindingConfigSize = 0;

    public ItemEntry(final DeviceConfig deviceConfig, final UnitConfig unitConfig, final ServiceConfig serviceConfig, final OpenHABBindingServiceConfig openHABBindingServiceConfig) throws InstantiationException {
        try {
            this.itemId = openHABBindingServiceConfig.getItemId();
            this.label = unitConfig.getLabel();
            this.groups = new ArrayList<>();

            configPool = new MetaConfigPool();
            configPool.register(new MetaConfigVariableProvider("ServiceMetaConfig", serviceConfig.getMetaConfig()));
            configPool.register(new MetaConfigVariableProvider("UnitMetaConfig", unitConfig.getMetaConfig()));
            configPool.register(new MetaConfigVariableProvider("DeviceMetaConfig", deviceConfig.getMetaConfig()));

            try {
                configPool.register(new MetaConfigVariableProvider("ServiceTemplateMetaConfig", lookupServiceTemplate(unitConfig, serviceConfig).getMetaConfig()));
            } catch (NotAvailableException ex) {
                ExceptionPrinter.printHistory(logger, new CouldNotPerformException("Could not load service template meta config for Service[" + serviceConfig.getType().name() + "] of Unit[" + unitConfig.getId() + "]", ex));
            }

            try {
                this.commandType = configPool.getValue(SERVICE_TEMPLATE_BINDING_COMMAND);
            } catch (NotAvailableException ex) {
                this.commandType = getCommand(serviceConfig.getType());
            }

            try {
                this.icon = configPool.getValue(SERVICE_TEMPLATE_BINDING_ICON);
            } catch (NotAvailableException ex) {
                this.icon = "sun";
            }

        // TODO: maybe think of another strategy to name groups
            // Dimmer and Rollershutter are key words in the openhab config and therefor cannot be used in groups
            String templateName = StringProcessor.transformUpperCaseToCamelCase(unitConfig.getTemplate().getType().name());
            if (!(templateName.equals("Dimmer") || templateName.equals("Rollershutter"))) {
                this.groups.add(StringProcessor.transformUpperCaseToCamelCase(unitConfig.getTemplate().getType().name()));
            }
            this.groups.add(StringProcessor.transformUpperCaseToCamelCase(serviceConfig.getType().name()));
            this.groups.add(unitConfig.getPlacementConfig().getLocationId());

            String bindingConfig;
            try {
                bindingConfig = generateBindingConfig(deviceConfig, unitConfig, serviceConfig);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(logger, ex);
                bindingConfig = openHABBindingServiceConfig.getItemHardwareConfig();
            }

            if (bindingConfig.isEmpty()) {
                throw new NotAvailableException("bindingConfig");
            }

            this.bindingConfig = bindingConfig;
            this.calculateGaps();
        } catch (Exception ex) {
            throw new de.citec.jul.exception.InstantiationException(this, ex);
        }
    }

    private String generateBindingConfig(final DeviceConfig deviceConfig, final UnitConfig unitConfig, final ServiceConfig serviceConfig) throws CouldNotPerformException {
        try {
            String config = "";

            config += configPool.getValue(SERVICE_TEMPLATE_BINDING_TYPE);
            config += "=\"";
            config += configPool.getValue(SERVICE_TEMPLATE_BINDING_CONFIG);
            config += "\"";
            return config;
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not generate binding config!", ex);
        }
    }

    /**
     * Lookups the service template of the given ServiceType out of the unit
     * config.
     *
     * @param unitConfig to lookup service template.
     * @param serviceConfig the service config providing the service type.
     * @return the related service template for the given service config.
     */
    private ServiceTemplate lookupServiceTemplate(final UnitConfig unitConfig, final ServiceConfig serviceConfig) throws NotAvailableException {
        return lookupServiceTemplate(unitConfig.getTemplate(), serviceConfig.getType());
    }

    /**
     * Lookups the service template of the given ServiceType out of the unit
     * template.
     *
     * @param unitTemplate to lookup the service template.
     * @param serviceType the service type to resolve the template.
     * @return the related service template for the given service type.
     * @throws NotAvailableException
     */
    private ServiceTemplate lookupServiceTemplate(final UnitTemplateType.UnitTemplate unitTemplate, final ServiceType serviceType) throws NotAvailableException {
        for (ServiceTemplate template : unitTemplate.getServiceTemplateList()) {
            if (template.getServiceType() == serviceType) {
                return template;
            }
        }
        throw new NotAvailableException("service template for ServiceType[" + serviceType.name() + "]");
    }

    private void calculateGaps() {
        maxCommandTypeSize = Math.max(maxCommandTypeSize, getCommandTypeStringRep().length());
        maxItemIdSize = Math.max(maxItemIdSize, getItemIdStringRep().length());
        maxLabelSize = Math.max(maxLabelSize, getLabelStringRep().length());
        maxIconSize = Math.max(maxIconSize, getIconStringRep().length());
        maxGroupSize = Math.max(maxGroupSize, getGroupsStringRep().length());
        maxBindingConfigSize = Math.max(maxBindingConfigSize, getBindingConfigStringRep().length());
    }

    public static void reset() {
        maxCommandTypeSize = 0;
        maxItemIdSize = 0;
        maxLabelSize = 0;
        maxIconSize = 0;
        maxGroupSize = 0;
        maxBindingConfigSize = 0;
    }

    public String getCommandType() {
        return commandType;
    }

    public String getItemId() {
        return itemId;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public List<String> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public String getBindingConfig() {
        return bindingConfig;
    }

    public String getCommandTypeStringRep() {
        return commandType;
    }

    public String getItemIdStringRep() {
        return itemId;
    }

    public String getLabelStringRep() {
        if (label.isEmpty()) {
            return "";
        }
        return "\"" + label + "\"";
    }

    public String getIconStringRep() {
        if (icon.isEmpty()) {
            return "";
        }
        return "<" + icon + ">";
    }

    public String getGroupsStringRep() {
        if (groups.isEmpty()) {
            return "";
        }
        String stringRep = "(";
        boolean firstIteration = true;
        for (String group : groups) {
            if (!firstIteration) {
                stringRep += ",";
            } else {
                firstIteration = false;
            }
            stringRep += group;
        }
        stringRep += ")";
        return stringRep;
    }

    public String getBindingConfigStringRep() {
        return "{ " + bindingConfig + " }";
    }

    public String buildStringRep() {

        String stringRep = "";

        // command type
        stringRep += StringProcessor.fillWithSpaces(getCommandTypeStringRep(), maxCommandTypeSize + TAB_SIZE);

        // unit id
        stringRep += StringProcessor.fillWithSpaces(getItemIdStringRep(), maxItemIdSize + TAB_SIZE);

        // label
        stringRep += StringProcessor.fillWithSpaces(getLabelStringRep(), maxLabelSize + TAB_SIZE);

        // icon
        stringRep += StringProcessor.fillWithSpaces(getIconStringRep(), maxIconSize + TAB_SIZE);

        // groups
        stringRep += StringProcessor.fillWithSpaces(getGroupsStringRep(), maxGroupSize + TAB_SIZE);

        // binding config
        stringRep += StringProcessor.fillWithSpaces(getBindingConfigStringRep(), maxBindingConfigSize + TAB_SIZE);

        return stringRep;
    }

    private String getCommand(ServiceTypeHolderType.ServiceTypeHolder.ServiceType type) {
        switch (type) {
            case COLOR_SERVICE:
                return "Color";
            case OPENING_RATIO_PROVIDER:
            case POWER_CONSUMPTION_PROVIDER:
            case TEMPERATURE_PROVIDER:
            case MOTION_PROVIDER:
            case TAMPER_PROVIDER:
            case BRIGHTNESS_PROVIDER:
            case BATTERY_PROVIDER:
            case SMOKE_ALARM_STATE_PROVIDER:
            case SMOKE_STATE_PROVIDER:
            case TEMPERATURE_ALARM_STATE_PROVIDER:
                return "Number";
            case SHUTTER_PROVIDER:
            case SHUTTER_SERVICE:
                return "Rollershutter";
            case POWER_SERVICE:
            case POWER_PROVIDER:
            case BUTTON_PROVIDER:
                return "Switch";
            case BRIGHTNESS_SERVICE:
            case DIM_PROVIDER:
            case DIM_SERVICE:
                return "Dimmer";
            case REED_SWITCH_PROVIDER:
                return "Contact";
            case HANDLE_PROVIDER:
                return "String";
            default:
                logger.warn("Unkown Service Type: " + type);
                return "";
        }
    }

    /**
     * Resolves the key to the value entry of the given meta config.
     *
     * @param metaConfig key value set
     * @param key the key to resolve
     * @return the related value of the given key.
     */
    public static String getMetaConfig(final MetaConfig metaConfig, final String key) throws NotAvailableException {
        for (Entry entry : metaConfig.getEntryList()) {
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }
        throw new NotAvailableException("value for Key[" + key + "]");
    }

    public static String resolveVariable(final String variable, final Collection<VariableProvider> providers) throws MultiException {
        VariableProvider[] providerArray = new VariableProvider[providers.size()];
        return resolveVariable(variable, providers.toArray(providerArray));
    }

    public static String resolveVariable(final String variable, final VariableProvider... providers) throws MultiException {
        MultiException.ExceptionStack exceptionStack = null;
        for (VariableProvider provider : providers) {

            try {
                return provider.getValue(variable);
            } catch (NotAvailableException ex) {
                exceptionStack = MultiException.push(logger, ex, exceptionStack);
                continue;
            }
        }
        MultiException.checkAndThrow("Could not resolve Variable[" + variable + "]!", exceptionStack);
        throw new AssertionError("Fatal error during variable resolving.");
    }

    public class MetaConfigPool {

        private Collection<VariableProvider> variableProviderPool;

        public MetaConfigPool(Collection<VariableProvider> variableProviderPool) {
            this.variableProviderPool = new ArrayList<>(variableProviderPool);
        }

        public MetaConfigPool() {
            this.variableProviderPool = new ArrayList<>();
        }

        public void register(MetaConfigVariableProvider provider) {
            variableProviderPool.add(provider);
        }

        public String getValue(String variable) throws NotAvailableException {
            try {
                return VariableProcessor.resolveVariables(resolveVariable(variable, variableProviderPool), false, variableProviderPool);
            } catch (MultiException ex) {
                throw new NotAvailableException("Variable[" + variable + "]", ex);
            }
        }
    }

    public class MetaConfigVariableProvider implements VariableProvider {

        private final String name;
        private final MetaConfig metaConfig;

        public MetaConfigVariableProvider(final String name, final MetaConfig metaConfig) {
            this.name = name;
            this.metaConfig = metaConfig;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue(String variable) throws NotAvailableException {
            return getMetaConfig(metaConfig, variable);
        }
    }
}
