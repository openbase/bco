/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.jp;

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

import org.dc.jps.preset.AbstractJPClass;
import org.dc.jul.extension.rsb.com.RSBRemoteService;

/**
 *
 * @author mpohling
 */
public class JPRemoteService extends AbstractJPClass<RSBRemoteService> {

    public final static String[] COMMAND_IDENTIFIERS = {"-r", "--remote"};

    public JPRemoteService() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public String getDescription() {
        return "Specifies the remote sevice to use for controlling.";
    }

    @Override
    protected Class<RSBRemoteService> getPropertyDefaultValue() {
        return null;
    }
}
