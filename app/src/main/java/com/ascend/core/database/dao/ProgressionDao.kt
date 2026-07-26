package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ascend.core.database.entity.AttributeTransactionEntity
import com.ascend.core.database.entity.XpTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface XpDao {
    /** Returns the inserted row id, or -1 if the unique guard blocked a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(entity: XpTransactionEntity): Long

    @Query(
        "SELECT * FROM xp_transaction WHERE transactionType = :type " +
            "AND sourceType = :sourceType AND sourceId = :sourceId LIMIT 1",
    )
    suspend fun find(
        type: String,
        sourceType: String,
        sourceId: String,
    ): XpTransactionEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_transaction WHERE userId = :userId")
    suspend fun totalXp(userId: String): Long

    @Query("SELECT * FROM xp_transaction WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeTransactions(userId: String): Flow<List<XpTransactionEntity>>
}

@Dao
interface AttributeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(entity: AttributeTransactionEntity): Long

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM attribute_transaction " +
            "WHERE userId = :userId AND attributeType = :attributeType",
    )
    suspend fun totalForAttribute(
        userId: String,
        attributeType: String,
    ): Long

    @Query("SELECT * FROM attribute_transaction WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeTransactions(userId: String): Flow<List<AttributeTransactionEntity>>
}
