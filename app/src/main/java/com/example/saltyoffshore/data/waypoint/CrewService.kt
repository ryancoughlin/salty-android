package com.example.saltyoffshore.data.waypoint

import android.util.Log
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.SupabaseClientProvider
import com.example.saltyoffshore.data.CrewError
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TAG = "CrewService"

/**
 * CRUD service for crew management via Supabase.
 * Port of iOS CrewStore.swift (I/O methods only — state lives in ViewModel).
 */
object CrewService {

    private val mutex = Mutex()
    private val supabase get() = SupabaseClientProvider.client

    // -- Insert payloads --

    @Serializable
    private data class CrewInsert(
        val name: String,
        @SerialName("created_by") val createdBy: String,
    )

    @Serializable
    private data class CrewMemberInsert(
        @SerialName("crew_id") val crewId: String,
        @SerialName("user_id") val userId: String,
    )

    // -- Response wrapper for JOIN on user_preferences --

    @Serializable
    private data class CrewMemberResponse(
        val id: String,
        @SerialName("crew_id") val crewId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("joined_at") val joinedAt: String,
        val user: UserInfo? = null,
    ) {
        @Serializable
        data class UserInfo(
            @SerialName("first_name") val firstName: String? = null,
            @SerialName("last_name") val lastName: String? = null,
        )

        fun toCrewMember(): CrewMember = CrewMember(
            id = id,
            crewId = crewId,
            userId = userId,
            joinedAt = joinedAt,
            userName = listOfNotNull(user?.firstName, user?.lastName)
                .joinToString(" ")
                .ifBlank { null },
        )
    }

    // MARK: - Fetch

    suspend fun fetchUserCrews(): List<Crew> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val userId = AuthManager.currentUserId ?: throw CrewError.NotAuthenticated

            val members = try {
                supabase.from("crew_members")
                    .select()
                    { filter { eq("user_id", userId) } }
                    .decodeList<CrewMember>()
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }

            val crewIds = members.map { it.crewId }
            if (crewIds.isEmpty()) return@withContext emptyList()

            try {
                supabase.from("crews")
                    .select()
                    { filter { isIn("id", crewIds) } }
                    .decodeList<Crew>()
                    .sortedByDescending { it.createdAt }
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }
        }
    }

    // MARK: - Create

    suspend fun createCrew(name: String): Crew = mutex.withLock {
        withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) throw CrewError.EmptyName
            if (trimmed.length > 50) throw CrewError.NameTooLong
            val userId = AuthManager.currentUserId ?: throw CrewError.NotAuthenticated

            val crew = try {
                supabase.from("crews")
                    .insert(CrewInsert(name = trimmed, createdBy = userId))
                    { select() }
                    .decodeSingle<Crew>()
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }

            try {
                supabase.from("crew_members")
                    .insert(CrewMemberInsert(crewId = crew.id, userId = userId))
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }

            Log.d(TAG, "Created crew ${crew.id}")
            crew
        }
    }

    // MARK: - Join

    suspend fun joinCrewByCode(code: String): Crew = mutex.withLock {
        withContext(Dispatchers.IO) {
            val formatted = code.trim().uppercase()
            if (formatted.length != 6) throw CrewError.InvalidCodeFormat
            val userId = AuthManager.currentUserId ?: throw CrewError.NotAuthenticated

            val crews = try {
                supabase.from("crews")
                    .select()
                    { filter { eq("invite_code", formatted) } }
                    .decodeList<Crew>()
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }

            val crew = crews.firstOrNull() ?: throw CrewError.CrewNotFound

            val existing = try {
                supabase.from("crew_members")
                    .select()
                    {
                        filter {
                            eq("crew_id", crew.id)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<CrewMember>()
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }

            if (existing.isNotEmpty()) throw CrewError.AlreadyMember

            try {
                supabase.from("crew_members")
                    .insert(CrewMemberInsert(crewId = crew.id, userId = userId))
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }

            Log.d(TAG, "Joined crew ${crew.id} via code $formatted")
            crew
        }
    }

    // MARK: - Members

    suspend fun fetchCrewMembers(crewId: String): List<CrewMember> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                supabase.from("crew_members")
                    .select(
                        columns = Columns.raw(
                            """
                            id,
                            crew_id,
                            user_id,
                            joined_at,
                            user:user_preferences!user_id (
                                first_name,
                                last_name
                            )
                            """.trimIndent()
                        )
                    ) {
                        filter { eq("crew_id", crewId) }
                        order("joined_at", Order.ASCENDING)
                    }
                    .decodeList<CrewMemberResponse>()
                    .map { it.toCrewMember() }
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }
        }
    }

    // MARK: - Update

    suspend fun updateCrewName(crewId: String, newName: String): Crew = mutex.withLock {
        withContext(Dispatchers.IO) {
            val trimmed = newName.trim()
            if (trimmed.isEmpty()) throw CrewError.EmptyName
            if (trimmed.length > 50) throw CrewError.NameTooLong
            val userId = AuthManager.currentUserId ?: throw CrewError.NotAuthenticated

            val crew = try {
                supabase.from("crews")
                    .select()
                    { filter { eq("id", crewId) } }
                    .decodeSingle<Crew>()
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }

            if (crew.createdBy != userId) throw CrewError.NotCreator

            try {
                supabase.from("crews")
                    .update(mapOf("name" to trimmed)) {
                        select()
                        filter { eq("id", crewId) }
                    }
                    .decodeSingle<Crew>()
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }
        }
    }

    // MARK: - Leave

    suspend fun leaveCrew(crewId: String): Unit = mutex.withLock {
        withContext(Dispatchers.IO) {
            val userId = AuthManager.currentUserId ?: throw CrewError.NotAuthenticated

            try {
                supabase.from("crew_members")
                    .delete {
                        filter {
                            eq("crew_id", crewId)
                            eq("user_id", userId)
                        }
                    }
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }

            Log.d(TAG, "Left crew $crewId")
        }
    }

    // MARK: - Delete

    suspend fun deleteCrew(crewId: String): Unit = mutex.withLock {
        withContext(Dispatchers.IO) {
            val userId = AuthManager.currentUserId ?: throw CrewError.NotAuthenticated

            val crew = try {
                supabase.from("crews")
                    .select()
                    { filter { eq("id", crewId) } }
                    .decodeSingle<Crew>()
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }

            if (crew.createdBy != userId) throw CrewError.NotCreator

            try {
                supabase.from("crews")
                    .delete { filter { eq("id", crewId) } }
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }

            Log.d(TAG, "Deleted crew $crewId")
        }
    }

    // MARK: - Remove Member

    suspend fun removeMember(crewId: String, memberId: String): Unit = mutex.withLock {
        withContext(Dispatchers.IO) {
            val userId = AuthManager.currentUserId ?: throw CrewError.NotAuthenticated

            val crew = try {
                supabase.from("crews")
                    .select()
                    { filter { eq("id", crewId) } }
                    .decodeSingle<Crew>()
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }

            if (crew.createdBy != userId) throw CrewError.NotCreator
            if (memberId == userId) throw CrewError.CannotRemoveSelf

            try {
                supabase.from("crew_members")
                    .delete {
                        filter {
                            eq("crew_id", crewId)
                            eq("user_id", memberId)
                        }
                    }
            } catch (e: Exception) {
                throw CrewError.DatabaseError(e.message ?: "Unknown error")
            }

            Log.d(TAG, "Removed member $memberId from crew $crewId")
        }
    }

    // MARK: - Helpers

    fun isCreator(crew: Crew): Boolean {
        val userId = AuthManager.currentUserId ?: return false
        return crew.createdBy == userId
    }
}
