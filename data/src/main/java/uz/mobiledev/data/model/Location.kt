package uz.mobiledev.data.model

import uz.mobiledev.database.models.LocationDBO

data class Location(
    val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val createdTime: Long,
)

fun Location.toDBO() = LocationDBO(
    id = id,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    createdTime = createdTime
)