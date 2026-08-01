package com.quangthe.nhacnho_uongthuoc

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PillDao {
    @Query("SELECT * FROM PillList WHERE IsDeleted = 0")
    fun getAllPills(): Flow<List<Pill>>

    @Query("SELECT * FROM PillList WHERE IsDeleted = 0")
    suspend fun getAllPillsSync(): List<Pill>

    @Query("SELECT * FROM PillList WHERE IsDeleted = 0")
    fun getAllPillsNonSuspend(): List<Pill>

    @Query("SELECT * FROM PillList WHERE IsDeleted = 1")
    fun getTrashPills(): Flow<List<Pill>>

    @Query("SELECT * FROM PillList WHERE IsDeleted = 1")
    suspend fun getTrashPillsSync(): List<Pill>

    @Query("SELECT * FROM PillList WHERE PrimaryKey = :pk LIMIT 1")
    suspend fun getPill(pk: Int): Pill?

    @Query("SELECT * FROM PillList WHERE PrimaryKey = :pk LIMIT 1")
    fun getPillSync(pk: Int): Pill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPill(pill: Pill): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPillSync(pill: Pill): Long

    @Update
    suspend fun updatePill(pill: Pill)

    @Update
    fun updatePillSync(pill: Pill)

    @Delete
    suspend fun deletePill(pill: Pill)

    @Query("UPDATE PillList SET IsDeleted = 1 WHERE PrimaryKey = :pk")
    suspend fun softDeletePill(pk: Int)

    @Query("UPDATE PillList SET IsDeleted = 1 WHERE PrimaryKey = :pk")
    fun softDeletePillSync(pk: Int)

    @Query("UPDATE PillList SET IsDeleted = 0 WHERE PrimaryKey = :pk")
    suspend fun restorePill(pk: Int)
}
