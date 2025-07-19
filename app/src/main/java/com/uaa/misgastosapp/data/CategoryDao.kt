// CategoryDao

package com.uaa.misgastosapp.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    fun getAll(userId: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: Int, userId: Int): CategoryEntity?

    @Query("SELECT name FROM categories WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getCategoryNameById(id: Int, userId: Int): String?

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("UPDATE categories SET name = :newName WHERE id = :id AND userId = :userId")
    suspend fun update(id: Int, newName: String, userId: Int)
}