package com.ssq.predictor.di

import android.content.Context
import com.ssq.predictor.data.datasource.AssetDataSource
import com.ssq.predictor.data.datasource.NetworkDataSource
import com.ssq.predictor.data.local.db.SSQDatabase
import com.ssq.predictor.data.repository.DrawRepository
import com.ssq.predictor.data.repository.PredictionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SSQDatabase {
        return SSQDatabase.create(context)
    }

    @Provides
    @Singleton
    fun provideDrawRepository(
        db: SSQDatabase,
        assetDataSource: AssetDataSource,
        networkDataSource: NetworkDataSource
    ): DrawRepository {
        return DrawRepository(db.drawDao(), assetDataSource, networkDataSource)
    }

    @Provides
    @Singleton
    fun providePredictionRepository(
        db: SSQDatabase
    ): PredictionRepository {
        return PredictionRepository(db.predictionRecordDao())
    }

    @Provides
    @Singleton
    fun provideAssetDataSource(@ApplicationContext context: Context): AssetDataSource {
        return AssetDataSource(context)
    }
}
