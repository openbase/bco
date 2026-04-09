package org.openbase.bco.dal.lib.layer.service.provider

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.jps.core.JPService
import org.openbase.jps.exception.JPServiceException
import org.openbase.jul.exception.VerificationFailedException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.type.domotic.state.ColorStateType.ColorState

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

class ColorStateProviderServiceTest {
    @Test
//  @Timeout(10)
    fun `should fix color values once they are defined out of range`() {
        JPService.setupJUnitTestMode()

        val builder = ColorState.newBuilder()
        builder.getColorBuilder().getHsbColorBuilder().setHue(240.0)
        builder.getColorBuilder().getHsbColorBuilder().setSaturation(100.0)
        builder.getColorBuilder().getHsbColorBuilder().setBrightness(50.0)

        ExceptionPrinter.setBeQuit(true)
        val verifiedColorState = ColorStateProviderService.verifyColorState(builder.build())
        ExceptionPrinter.setBeQuit(false)

        Assertions.assertEquals(
            builder.getColorBuilder().getHsbColorBuilder().getHue(),
            verifiedColorState.getColor().getHsbColor().getHue(),
            0.00001,
            "Hue value invalid!"
        )
        Assertions.assertEquals(
            1.0,
            verifiedColorState.getColor().getHsbColor().getSaturation(),
            0.00001,
            "Hue value invalid!"
        )
        Assertions.assertEquals(
            0.5,
            verifiedColorState.getColor().getHsbColor().getBrightness(),
            0.00001,
            "Hue value invalid!"
        )
    }

    @Test
//  @Timeout(10)
    fun `should handle color state comparison with equal state`() {
        JPService.setupJUnitTestMode()

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
        ) shouldBe true

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(0.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(360.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
        ) shouldBe true
    }

    @Test
//  @Timeout(10)
    fun `should handle color state comparison with non equal state`() {
        JPService.setupJUnitTestMode()

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(50.0)
                    setBrightness(50.0)
                }
            }.build(),
        ) shouldBe false

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(100.0)
                }
            }.build(),
        ) shouldBe false

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(30.0)
                    setSaturation(0.0)
                    setBrightness(50.0)
                }
            }.build(),
        ) shouldBe false

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(50.0)
                    setBrightness(1.0)
                }
            }.build(),
        ) shouldBe false

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(10.0)
                    setSaturation(100.0)
                    setBrightness(20.0)
                }
            }.build(),
        ) shouldBe false

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(233.0)
                    setSaturation(23.0)
                    setBrightness(12.0)
                }
            }.build(),
        ) shouldBe false

    }

    @Test
//  @Timeout(10)
    fun `should handle color state comparison with neutral state`() {
        JPService.setupJUnitTestMode()

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().build(),
        ) shouldBe false

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
        ) shouldBe false

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
        ) shouldBe false

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
        ) shouldBe false

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
        ) shouldBe false

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
        ) shouldBe false

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setBrightness(50.0)
                }
            }.build(),
        ) shouldBe false

        ColorStateProviderService.equalServiceStates(
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                    setBrightness(50.0)
                }
            }.build(),
            ColorState.newBuilder().apply {
                getColorBuilder().getHsbColorBuilder().apply {
                    setHue(240.0)
                    setSaturation(100.0)
                }
            }.build(),
        ) shouldBe false
    }
}
