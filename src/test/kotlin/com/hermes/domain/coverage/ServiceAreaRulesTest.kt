package com.hermes.domain.coverage

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ServiceAreaRulesTest {
    @Test
    fun `radius area contains nearby point`() {
        val area = ServiceArea(
            id = "area_1",
            organizationId = "org_1",
            branchId = "br_1",
            type = ServiceAreaType.RADIUS_KM,
            status = ServiceAreaStatus.ACTIVE,
            center = GeoPoint(latitude = -0.4, longitude = -78.55),
            radiusKm = 5.0,
        )

        assertTrue(ServiceAreaRules.contains(area, GeoPoint(latitude = -0.401, longitude = -78.551)))
        assertFalse(ServiceAreaRules.contains(area, GeoPoint(latitude = -0.80, longitude = -78.90)))
    }

    @Test
    fun `province city area matches case insensitive`() {
        val area = ServiceArea(
            id = "area_1",
            organizationId = "org_1",
            branchId = null,
            type = ServiceAreaType.PROVINCE_CITY,
            status = ServiceAreaStatus.ACTIVE,
            province = "Pichincha",
            city = "Mejía",
        )

        assertTrue(ServiceAreaRules.contains(area, GeoPoint(latitude = -0.4, longitude = -78.55), province = "pichincha", city = "mejía"))
    }

    @Test
    fun `paused area never contains point`() {
        val area = ServiceArea(
            id = "area_1",
            organizationId = "org_1",
            branchId = null,
            type = ServiceAreaType.RADIUS_KM,
            status = ServiceAreaStatus.PAUSED,
            center = GeoPoint(latitude = -0.4, longitude = -78.55),
            radiusKm = 5.0,
        )

        assertFalse(ServiceAreaRules.contains(area, GeoPoint(latitude = -0.401, longitude = -78.551)))
    }

    @Test
    fun `rejects radius area without radius`() {
        assertFailsWith<DomainRuleViolation> {
            ServiceAreaRules.validate(
                ServiceArea(
                    id = "area_1",
                    organizationId = "org_1",
                    branchId = null,
                    type = ServiceAreaType.RADIUS_KM,
                    status = ServiceAreaStatus.ACTIVE,
                    center = GeoPoint(latitude = -0.4, longitude = -78.55),
                    radiusKm = null,
                ),
            )
        }
    }
}
