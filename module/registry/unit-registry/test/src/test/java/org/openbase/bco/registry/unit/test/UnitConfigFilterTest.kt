package org.openbase.bco.registry.unit.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.openbase.bco.registry.unit.lib.filter.UnitConfigFilterImpl
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.UnitFilterType.UnitFilter

class UnitConfigFilterTest {

    @Test
    fun `test filter for username with exact cases`() {
        val username1 = "Guybrush"
        val username2 = "LeChuck"

        val userGuybrush = UnitConfig
            .newBuilder()
            .also { it.userConfigBuilder.userName = username1 }
            .build()

        val userLeChuck = UnitConfig
            .newBuilder()
            .also { it.userConfigBuilder.userName = username2 }
            .build()

        val guybrushFilter = UnitFilter
            .newBuilder()
            .also { it.propertiesBuilder.userConfigBuilder.userName = username1 }
            .build()
            .let {  UnitConfigFilterImpl(it) }

        val lechuckFilter = UnitFilter
            .newBuilder()
            .also {it.propertiesBuilder.userConfigBuilder.userName = username1 }
            .build()
            .let { UnitConfigFilterImpl(it) }

        guybrushFilter.match(userGuybrush) shouldBe true
        guybrushFilter.match(userLeChuck) shouldBe false

        lechuckFilter.match(userGuybrush) shouldBe true
        lechuckFilter.match(userLeChuck) shouldBe false
    }

    @Test
    fun `test filter for username with alternating cases`() {
        val username1 = "Guybrush"
        val username2 = "LeChuck"

        val userGuybrush = UnitConfig
            .newBuilder()
            .also { it.userConfigBuilder.userName = username1.uppercase() }
            .build()

        val userLeChuck = UnitConfig
            .newBuilder()
            .also { it.userConfigBuilder.userName = username2.lowercase() }
            .build()

        val guybrushFilter = UnitFilter
            .newBuilder()
            .also { it.propertiesBuilder.userConfigBuilder.userName = username1 }
            .build()
            .let {  UnitConfigFilterImpl(it) }

        val lechuckFilter = UnitFilter
            .newBuilder()
            .also {it.propertiesBuilder.userConfigBuilder.userName = username1 }
            .build()
            .let { UnitConfigFilterImpl(it) }

        guybrushFilter.match(userGuybrush) shouldBe true
        guybrushFilter.match(userLeChuck) shouldBe false

        lechuckFilter.match(userGuybrush) shouldBe true
        lechuckFilter.match(userLeChuck) shouldBe false
    }
}
