package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ascend.core.database.entity.QuestTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestTemplateDao {
    @Upsert
    suspend fun upsertAll(entities: List<QuestTemplateEntity>)

    @Upsert
    suspend fun upsert(entity: QuestTemplateEntity)

    @Query("SELECT * FROM quest_template ORDER BY name ASC")
    fun observeAll(): Flow<List<QuestTemplateEntity>>

    @Query("SELECT * FROM quest_template ORDER BY name ASC")
    suspend fun getAll(): List<QuestTemplateEntity>

    @Query("SELECT * FROM quest_template WHERE id = :id")
    suspend fun getById(id: String): QuestTemplateEntity?

    @Query("SELECT COUNT(*) FROM quest_template")
    suspend fun count(): Int
}
