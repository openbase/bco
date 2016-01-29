/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.user.lib;

import org.dc.jul.exception.InstantiationException;
import org.dc.jul.pattern.Factory;
import rst.authorization.UserConfigType.UserConfig;

/**
 *
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface UserFactory extends Factory<UserController, UserConfig> {

    @Override
    public UserController newInstance(final UserConfig config) throws InstantiationException;

}
