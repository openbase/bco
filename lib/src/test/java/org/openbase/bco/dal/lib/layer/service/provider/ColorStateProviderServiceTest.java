package org.openbase.bco.dal.lib.layer.service.provider;

import org.junit.Assert;
import org.junit.Test;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.ColorStateType.ColorState.Builder;

import static org.junit.Assert.*;

public class ColorStateProviderServiceTest {

    @Test
    public void verifyColorState() throws VerificationFailedException, JPServiceException {

        JPService.setupJUnitTestMode();

        final Builder builder = ColorState.newBuilder();
        builder.getColorBuilder().getHsbColorBuilder().setHue(240);
        builder.getColorBuilder().getHsbColorBuilder().setSaturation(100);
        builder.getColorBuilder().getHsbColorBuilder().setBrightness(50);

        ExceptionPrinter.setBeQuit(true);
        final ColorState verifiedColorState = ColorStateProviderService.verifyColorState(builder.build());
        ExceptionPrinter.setBeQuit(false);

        Assert.assertEquals("Hue value invalid!", builder.getColorBuilder().getHsbColorBuilder().getHue(), verifiedColorState.getColor().getHsbColor().getHue(), 0.00001);
        Assert.assertEquals("Hue value invalid!", 1d, verifiedColorState.getColor().getHsbColor().getSaturation(), 0.00001);
        Assert.assertEquals("Hue value invalid!", 0.5d, verifiedColorState.getColor().getHsbColor().getBrightness(), 0.00001);
    }
}