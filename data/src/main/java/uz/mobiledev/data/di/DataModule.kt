package uz.mobiledev.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.mobiledev.data.repository.LocationRepository
import uz.mobiledev.data.repository.LocationRepositoryImpl
import uz.mobiledev.database.dao.LocationDao

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    internal abstract fun provideLocationRepository(
        locationRepository: LocationRepositoryImpl
    ): LocationRepository

}