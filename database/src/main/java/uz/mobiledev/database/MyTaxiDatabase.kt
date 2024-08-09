package uz.mobiledev.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import uz.mobiledev.database.dao.LocationDao
import uz.mobiledev.database.models.LocationDBO

class MyTaxiDatabase internal constructor(private val database: MyTaxiRoomDatabase) {
    val locationDao: LocationDao
        get() = database.locationDao()
}

@Database(
    entities = [LocationDBO::class],
    version = 1
)
internal abstract class MyTaxiRoomDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}

fun MyTaxiDatabase(applicationContext: Context): MyTaxiDatabase {
    val newsRoomDatabase =
        Room.databaseBuilder(
            checkNotNull(applicationContext.applicationContext),
            MyTaxiRoomDatabase::class.java,
            "taxi"
        ).build()
    return MyTaxiDatabase(newsRoomDatabase)
}