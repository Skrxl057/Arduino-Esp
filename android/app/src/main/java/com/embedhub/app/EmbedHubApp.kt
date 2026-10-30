package com.embedhub.app

import android.app.Application
import com.embedhub.app.core.manager.ConnectionManager
import com.embedhub.app.core.service.ConnectionForegroundService
import com.embedhub.app.data.repository.ProfileRepository

class EmbedHubApp : Application() {

    lateinit var connectionManager: ConnectionManager
        private set

    lateinit var profileRepository: ProfileRepository
        private set

    override fun onCreate() {
        super.onCreate()
        connectionManager = ConnectionManager(this)
        profileRepository = ProfileRepository(this)

        // Start background service to maintain wireless hardware links
        ConnectionForegroundService.startService(this)
    }
}
