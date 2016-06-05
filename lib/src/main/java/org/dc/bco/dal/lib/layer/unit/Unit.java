package org.dc.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.MultiException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rst.iface.ScopeProvider;
import org.dc.jul.iface.Identifiable;
import org.dc.jul.iface.Snapshotable;
import org.dc.jul.iface.provider.ConfigProvider;
import org.dc.jul.iface.provider.LabelProvider;
import rst.homeautomation.control.action.ActionAuthorityType;
import rst.homeautomation.control.action.ActionConfigType;
import rst.homeautomation.control.action.ActionPriorityType;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.service.ServiceTemplateType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author mpohling
 */
public interface Unit extends Service, LabelProvider, ScopeProvider, Identifiable<String>, ConfigProvider<UnitConfig>, Snapshotable<SceneConfig> {

    /**
     * Returns the unit type.
     * @return UnitType
     * @throws NotAvailableException
     */
    public UnitType getType() throws NotAvailableException;

    /**
     * Returns the related template for this unit.
     * @return UnitTemplate
     * @throws NotAvailableException in case the unit template is not available.
     */
    public UnitTemplateType.UnitTemplate getTemplate() throws NotAvailableException;

    @Override
    public default Future<SceneConfig> recordSnaphot() throws CouldNotPerformException, InterruptedException {
        MultiException.ExceptionStack exceptionStack = null;
        SceneConfig.Builder snapshotBuilder = SceneConfig.newBuilder();
        for (ServiceTemplateType.ServiceTemplate.ServiceType serviceType : getTemplate().getServiceTypeList()) {
            try {
                ActionConfigType.ActionConfig.Builder actionConfig = ActionConfigType.ActionConfig.newBuilder().setServiceType(serviceType).setServiceHolder(getId());

                // skip non operation services.
                if (Service.detectServiceBaseType(serviceType) != ServiceBaseType.OPERATION) {
                    continue;
                }

                // load service attribute by related provider service
                Object serviceAttribute = Service.invokeServiceMethod(Service.getProviderForOperationService(serviceType), this);

                // fill action config
                actionConfig.setServiceAttribute(ServiceJSonProcessor.serialize(serviceAttribute));
                actionConfig.setServiceAttributeType(ServiceJSonProcessor.getServiceAttributeType(serviceAttribute));
                actionConfig.setActionAuthority(ActionAuthorityType.ActionAuthority.newBuilder().setAuthority(ActionAuthorityType.ActionAuthority.Authority.USER)).setActionPriority(ActionPriorityType.ActionPriority.newBuilder().setPriority(ActionPriorityType.ActionPriority.Priority.NORMAL));

                // add action config
                snapshotBuilder.addActionConfig(actionConfig.build());
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
        }
        MultiException.checkAndThrow("Could not record snapshot!", exceptionStack);
        return CompletableFuture.completedFuture(snapshotBuilder.build());
    }

    @Override
    public default Future<Void> restoreSnapshot(SceneConfig snapshot) throws CouldNotPerformException, InterruptedException {
        try {
            for (final ActionConfigType.ActionConfig actionConfig : snapshot.getActionConfigList()) {
                applyAction(actionConfig);
            }
            return CompletableFuture.completedFuture(null);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not record snapshot!", ex);
        }
    }
}
