package com.quangthe.nhacnho_uongthuoc

import android.content.Context
import kotlinx.coroutines.flow.Flow

class PillRepository(private val context: Context, private val pillDao: PillDao) {

    val allPills: Flow<List<Pill>> = pillDao.getAllPills()

    val trashPills: Flow<List<Pill>> = pillDao.getTrashPills()

    suspend fun getAllPillsSync(): List<Pill> = pillDao.getAllPillsSync()

    suspend fun getPill(pk: Int): Pill? = pillDao.getPill(pk)

    suspend fun insertPill(pill: Pill): Long {
        val id = pillDao.insertPill(pill)
        pill.primaryKey = id.toInt()
        pill.setAlarmRequestCodes()
        scheduleAlarms(pill)
        pill.alarmsSet = 1
        pillDao.updatePill(pill)
        return id
    }

    suspend fun updatePill(pill: Pill) {
        scheduleAlarms(pill)
        pill.alarmsSet = 1
        pillDao.updatePill(pill)
    }

    suspend fun deletePill(pill: Pill) {
        pill.cancelAlarms(context)
        pillDao.deletePill(pill)
    }

    suspend fun softDeletePill(pill: Pill) {
        pill.cancelAlarms(context)
        pill.isDeleted = 1
        pillDao.updatePill(pill)
    }

    suspend fun restorePill(pill: Pill) {
        pill.isDeleted = 0
        pill.alarmsSet = 1
        scheduleAlarms(pill)
        pillDao.updatePill(pill)
    }

    private fun scheduleAlarms(pill: Pill) {
        pill.setAlarm(context)
        pill.setStockupAlarm(context)
    }
}
