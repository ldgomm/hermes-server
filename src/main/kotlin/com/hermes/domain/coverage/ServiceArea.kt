package com.hermes.domain.coverage

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class ServiceAreaType { RADIUS_KM, PROVINCE_CITY, POLYGON }
enum class ServiceAreaStatus { ACTIVE, PAUSED, ARCHIVED }

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        if (latitude !in -90.0..90.0) throw DomainRuleViolation("Latitude must be between -90 and 90.")
        if (longitude !in -180.0..180.0) throw DomainRuleViolation("Longitude must be between -180 and 180.")
    }
}

data class ServiceArea(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val type: ServiceAreaType,
    val status: ServiceAreaStatus,
    val center: GeoPoint? = null,
    val radiusKm: Double? = null,
    val province: String? = null,
    val city: String? = null,
    val polygon: List<GeoPoint> = emptyList(),
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Service area id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Service area organization id cannot be blank.")
    }
}

object ServiceAreaRules {
    fun validate(area: ServiceArea) {
        when (area.type) {
            ServiceAreaType.RADIUS_KM -> {
                if (area.center == null || area.radiusKm == null) throw DomainRuleViolation("Radius service area requires center and radiusKm.")
                if (area.radiusKm <= 0.0) throw DomainRuleViolation("Service area radius must be greater than zero.")
            }
            ServiceAreaType.PROVINCE_CITY -> {
                if (area.province.isNullOrBlank() || area.city.isNullOrBlank()) throw DomainRuleViolation("Province/city service area requires province and city.")
            }
            ServiceAreaType.POLYGON -> {
                if (area.polygon.size < 3) throw DomainRuleViolation("Polygon service area requires at least three points.")
            }
        }
    }

    fun contains(area: ServiceArea, point: GeoPoint, province: String? = null, city: String? = null): Boolean {
        validate(area)
        if (area.status != ServiceAreaStatus.ACTIVE) return false
        return when (area.type) {
            ServiceAreaType.RADIUS_KM -> distanceKm(area.center!!, point) <= area.radiusKm!!
            ServiceAreaType.PROVINCE_CITY -> area.province.equals(province, true) && area.city.equals(city, true)
            ServiceAreaType.POLYGON -> containsInPolygon(point, area.polygon)
        }
    }

    private fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2).pow(2.0) + sin(dLon / 2).pow(2.0) * cos(lat1) * cos(lat2)
        return 2 * earthRadiusKm * atan2(sqrt(h), sqrt(1 - h))
    }

    private fun containsInPolygon(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            val intersects = ((pi.latitude > point.latitude) != (pj.latitude > point.latitude)) &&
                (point.longitude < (pj.longitude - pi.longitude) * (point.latitude - pi.latitude) / (pj.latitude - pi.latitude) + pi.longitude)
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }
}
