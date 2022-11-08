package org.openbase.bco.dal.lib.layer.service.provider;

/*-
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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.ColorStateType.ColorState.Builder;

public class ColorStateProviderServiceTest {

    @Test
    @Timeout(10)
    public void verifyColorState() throws VerificationFailedException, JPServiceException {

        JPService.setupJUnitTestMode();

        final Builder builder = ColorState.newBuilder();
        builder.getColorBuilder().getHsbColorBuilder().setHue(240);
        builder.getColorBuilder().getHsbColorBuilder().setSaturation(100);
        builder.getColorBuilder().getHsbColorBuilder().setBrightness(50);

        ExceptionPrinter.setBeQuit(true);
        final ColorState verifiedColorState = ColorStateProviderService.verifyColorState(builder.build());
        ExceptionPrinter.setBeQuit(false);

        assertEquals(builder.getColorBuilder().getHsbColorBuilder().getHue(), verifiedColorState.getColor().getHsbColor().getHue(), 0.00001, "Hue value invalid!");
        assertEquals(1d, verifiedColorState.getColor().getHsbColor().getSaturation(), 0.00001, "Hue value invalid!");
        assertEquals(0.5d, verifiedColorState.getColor().getHsbColor().getBrightness(), 0.00001, "Hue value invalid!");
    }
}
