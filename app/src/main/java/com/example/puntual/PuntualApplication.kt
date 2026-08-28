package com.example.puntual

import android.app.Application
import com.example.puntual.domain.repository.PeriodRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class PuntualApplication : Application() {

    @Inject
    lateinit var periodRepository: PeriodRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            periodRepository.ensureDefaultPeriodExists()
        }
    }
}
