package uz.mobiledev.data.repository

import uz.mobiledev.data.model.Location
import uz.mobiledev.data.model.toDBO
import uz.mobiledev.database.MyTaxiDatabase
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val database: MyTaxiDatabase
) : LocationRepository {

    override suspend fun saveLocation(location: Location) {
        database.locationDao.insertLocation(location.toDBO())
    }

}