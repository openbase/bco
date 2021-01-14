package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.iface.Session;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.pattern.controller.ConfigurableRemote;
import org.openbase.type.communication.ScopeType;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import rsb.Scope;

import java.util.concurrent.Future;

/**
 * @param <M> Message
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UnitRemote<M extends Message> extends Unit<M>, ConfigurableRemote<String, M, UnitConfig> {

    /**
     * Method initializes this unit remote instance via it's remote controller scope.
     *
     * @param scope the scope which is used to reach the remote controller.
     *
     * @throws InitializationException is thrown in case the remote could not be initialized with the given scope.
     * @throws InterruptedException    is thrown in case the thread is externally interrupted.
     */
    void init(ScopeType.Scope scope) throws InitializationException, InterruptedException;

    /**
     * Method initializes this unit remote instance via it's remote controller scope.
     *
     * @param scope the scope which is used to reach the remote controller.
     *
     * @throws InitializationException is thrown in case the remote could not be initialized with the given scope.
     * @throws InterruptedException    is thrown in case the thread is externally interrupted.
     */
    void init(Scope scope) throws InitializationException, InterruptedException;

    /**
     * Method initializes this unit remote instance via the given id.
     *
     * @param id the unit id which is used to resolve the remote controller scope.
     *
     * @throws InitializationException is thrown in case the remote could not be initialized with the given id.
     * @throws InterruptedException    is thrown in case the thread is externally interrupted.
     */
    void initById(final String id) throws InitializationException, InterruptedException;

    /**
     * This method returns if the unit of this remote is enabled.
     * An unit is marked as disabled if the related unit host is not available. For instance all units are automatically disabled when the providing device is currently borrowed or at least marked as not installed.
     * <p>
     * Note: Method returns false if the state could not be detected. This can happen if the unit was never initialized or the related unit configuration is not available.
     *
     * @return returns true if the unit is enabled otherwise false.
     */
    boolean isEnabled();

    /**
     * Set the session manager for a unit remote. The session manager is
     * used to determine who triggers actions using the unit remote.
     *
     * @param sessionManager the session manager containing authorization information for the usage of the remote.
     */
    void setSessionManager(final SessionManager sessionManager);

    /**
     * Sets the session which is used for the authentication of the client/user
     *
     * @param session a session instance.
     */
    default void setSession(final Session session) {
        setSessionManager(session.getSessionManager());
    }

    /**
     * Get the current session manager of the unit remote.
     *
     * @return the current session manager.
     */
    SessionManager getSessionManager();

    /**
     * Method applies the action on this instance with permission given through authToken. The provided authToken can be null.
     *
     * @param actionDescription the description of the action.
     *
     * @return a future which gives feedback about the action execution state.
     */
    Future<ActionDescription> applyAction(final ActionDescription actionDescription, final AuthToken authToken);

    /**
     * Method applies the action on this instance with permission given through authToken. The provided authToken can be null.
     *
     * @param actionDescriptionBuilder the description of the action.
     *
     * @return a future which gives feedback about the action execution state.
     */
    default Future<ActionDescription> applyAction(final ActionDescription.Builder actionDescriptionBuilder, final AuthToken authToken) {
        return applyAction(actionDescriptionBuilder.build(), authToken);
    }

    /**
     * Cancel an action with permission given through authToken. The provided authToken can be null.
     *
     * @param actionDescription the action to cancel.
     * @param authToken         the authToken used to get permission for the cancellation.
     *
     * @return a future of the cancel request.
     */
    default Future<ActionDescription> cancelAction(final ActionDescription actionDescription, final AuthToken authToken) {
        return applyAction(actionDescription.toBuilder().setCancel(true), authToken);
    }

    /**
     * Extends an action with permission given through authToken. The provided authToken can be null.
     *
     * @param actionDescription the action to extend.
     * @param authToken         the authToken used to get permission for the extension.
     *
     * @return a future of the extension request.
     */
    default Future<ActionDescription> extendAction(final ActionDescription actionDescription, final AuthToken authToken) {
        return applyAction(actionDescription.toBuilder().setExtend(true), authToken);
    }

    /**
     * Cancel an action with permission given through authToken. The provided authToken can be null.
     *
     * @param actionDescription the action to cancel.
     *
     * @return a future of the cancel request.
     */
    default Future<ActionDescription> cancelAction(final ActionDescription actionDescription) {
        return cancelAction(actionDescription, null);
    }

    /**
     * Extends an action with permission given through authToken. The provided authToken can be null.
     *
     * @param actionDescription the action to extend.
     *
     * @return a future of the extension request.
     */
    default Future<ActionDescription> extendAction(final ActionDescription actionDescription) {
        return extendAction(actionDescription, null);
    }
}
