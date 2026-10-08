package com.lago.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RealTimeModule {
    // SmartStockWebSocketService, RealTimeStockCache, SmartUpdateScheduler는
    // @Inject constructor 사용으로 자동 바인딩됨
}