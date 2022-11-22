package org.openbase.bco.registry.unit.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.openbase.bco.registry.unit.lib.filter.UnitConfigFilterImpl
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.UnitFilterType.UnitFilter

class UnitConfigFilterTest {

    @Test
    fun `test filter for username`() {
        val username1 = "Guybrush"
        val username2 = "LeChuck"

        val userGuybrush = UnitConfig.newBuilder()
        userGuybrush.userConfigBuilder.userName = username1
        val userLeChuck = UnitConfig.newBuilder()
        userLeChuck.userConfigBuilder.userName = username2

        val filterConfig = UnitFilter.newBuilder()
        filterConfig.propertiesBuilder.userConfigBuilder.userName = username1
        val guybrushFilter = UnitConfigFilterImpl(filterConfig.build())

        guybrushFilter.match(userGuybrush.build()) shouldBe true
        guybrushFilter.match(userLeChuck.build()) shouldBe false
    }
}
