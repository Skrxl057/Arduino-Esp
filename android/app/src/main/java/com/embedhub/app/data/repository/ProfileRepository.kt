package com.embedhub.app.data.repository

import android.content.Context
import androidx.room.Room
import com.embedhub.app.core.model.*
import com.embedhub.app.data.db.AppDatabase
import com.embedhub.app.data.db.FullProfile
import com.embedhub.app.data.db.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class ProfileRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "embedhub.db"
    ).build()

    private val profileDao = db.profileDao()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    val allProfiles: Flow<List<FullProfile>> = profileDao.getAllProfiles().map { entities ->
        entities.mapNotNull { entity ->
            try {
                json.decodeFromString<FullProfile>(entity.jsonContent)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun saveProfile(profile: FullProfile) {
        val serialized = json.encodeToString(profile)
        val entity = ProfileEntity(
            id = profile.id,
            name = profile.name,
            slotIndex = profile.slotIndex,
            jsonContent = serialized
        )
        profileDao.insertOrUpdate(entity)
    }

    suspend fun duplicateProfile(originalId: String, newName: String): FullProfile? {
        val entity = profileDao.getProfileById(originalId) ?: return null
        val profile = json.decodeFromString<FullProfile>(entity.jsonContent)
        val duplicated = profile.copy(
            id = UUID.randomUUID().toString(),
            name = newName
        )
        saveProfile(duplicated)
        return duplicated
    }

    suspend fun deleteProfile(profileId: String) {
        profileDao.deleteById(profileId)
    }

    fun exportProfileJson(profile: FullProfile): String {
        return json.encodeToString(profile)
    }

    fun importProfileJson(jsonString: String): Result<FullProfile> {
        return try {
            val profile = json.decodeFromString<FullProfile>(jsonString)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
