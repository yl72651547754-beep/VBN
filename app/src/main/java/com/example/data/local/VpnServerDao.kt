package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnServerDao {

    @Query("SELECT * FROM vpn_servers ORDER BY speedBps DESC")
    fun getAllServers(): Flow<List<VpnServerEntity>>

    @Query("SELECT * FROM vpn_servers WHERE isFavorite = 1 ORDER BY speedBps DESC")
    fun getFavoriteServers(): Flow<List<VpnServerEntity>>

    @Query("SELECT * FROM vpn_servers WHERE ip = :ip LIMIT 1")
    suspend fun getServerByIp(ip: String): VpnServerEntity?

    @Query("SELECT isFavorite FROM vpn_servers WHERE ip = :ip LIMIT 1")
    suspend fun isFavorite(ip: String): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<VpnServerEntity>)

    @Update
    suspend fun updateServer(server: VpnServerEntity)

    @Query("UPDATE vpn_servers SET isFavorite = :isFavorite WHERE ip = :ip")
    suspend fun updateFavoriteStatus(ip: String, isFavorite: Boolean)

    @Query("UPDATE vpn_servers SET measuredPingMs = :pingMs WHERE ip = :ip")
    suspend fun updateMeasuredPing(ip: String, pingMs: Long)

    @Query("DELETE FROM vpn_servers WHERE isFavorite = 0")
    suspend fun deleteNonFavorites()

    @Query("DELETE FROM vpn_servers")
    suspend fun clearAll()

    @Query("SELECT DISTINCT countryShort FROM vpn_servers ORDER BY countryShort ASC")
    fun getAvailableCountryCodes(): Flow<List<String>>
}
