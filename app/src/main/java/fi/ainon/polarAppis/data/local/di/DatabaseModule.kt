package fi.ainon.polarAppis.data.local.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.data.local.database.AppDatabase
import fi.ainon.polarAppis.data.local.database.PolarInfoDataDao
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    fun providePolarInfoDataDao(appDatabase: AppDatabase): PolarInfoDataDao {
        return appDatabase.polarInfoDataDao()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "PolarAppis"
        ).build()
    }
}
