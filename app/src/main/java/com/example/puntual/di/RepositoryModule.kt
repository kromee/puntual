package com.example.puntual.di

import com.example.puntual.data.repository.CheckInRepositoryImpl
import com.example.puntual.data.repository.AbsenceRepositoryImpl
import com.example.puntual.data.repository.AuthRepositoryImpl
import com.example.puntual.data.repository.PeriodRepositoryImpl
import com.example.puntual.data.repository.QuoteRepositoryImpl
import com.example.puntual.domain.repository.AbsenceRepository
import com.example.puntual.domain.repository.AuthRepository
import com.example.puntual.domain.repository.CheckInRepository
import com.example.puntual.domain.repository.PeriodRepository
import com.example.puntual.domain.repository.QuoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCheckInRepository(
        impl: CheckInRepositoryImpl,
    ): CheckInRepository

    @Binds
    @Singleton
    abstract fun bindAbsenceRepository(
        impl: AbsenceRepositoryImpl,
    ): AbsenceRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPeriodRepository(
        impl: PeriodRepositoryImpl,
    ): PeriodRepository

    @Binds
    @Singleton
    abstract fun bindQuoteRepository(
        impl: QuoteRepositoryImpl,
    ): QuoteRepository
}
