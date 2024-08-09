package uz.mobiledev.data.repository

import uz.mobiledev.data.model.Location

interface LocationRepository {

    suspend fun saveLocation(location: Location)
}