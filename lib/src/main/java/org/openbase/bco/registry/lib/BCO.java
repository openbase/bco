package org.openbase.bco.registry.lib;

import java.util.Calendar;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface BCO {

    public static final String CURRENT_YEAR = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
    public static final String BCO_LOGO_ASCI_ARTS
            = "                                 \n"
            + "                                 \n"
            + "     #####    ####  #######      \n"
            + "     ##  ##  ##     ##   ##      \n"
            + "     #####   ##     ##   ##      \n"
            + "     ##  ##  ##     ##   ##      \n"
            + "     #####    ####  #######      \n"
            + "     ======================      \n"
            + "                                 \n"
            + "          openbase.org " + CURRENT_YEAR + "   \n"
            + "                                 \n";

    public static void printLogo() {
        System.out.println(BCO_LOGO_ASCI_ARTS);
    }

}
