package org.fossify.contacts.databases

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "hanzi_pinyin")
data class HanziPinyin(
    @PrimaryKey
    @ColumnInfo(name = "hanzi")
    val hanzi: String,
    
    @ColumnInfo(name = "pinyin")
    val pinyin: String
)

@Dao
interface HanziPinyinDao {
    @Query("SELECT * FROM hanzi_pinyin WHERE hanzi = :hanzi LIMIT 1")
    fun getHanziPinyin(hanzi: String): HanziPinyin?

    @Query("SELECT * FROM hanzi_pinyin")
    fun getAll(): List<HanziPinyin>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(hanziPinyin: HanziPinyin)
}

@Database(entities = [HanziPinyin::class], version = 1, exportSchema = false)
abstract class HanziPinyinDatabase : RoomDatabase() {
    abstract fun hanziPinyinDao(): HanziPinyinDao

    companion object {
        @Volatile
        private var INSTANCE: HanziPinyinDatabase? = null

        fun getDatabase(context: Context): HanziPinyinDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HanziPinyinDatabase::class.java,
                    "hanzi_pinyin_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
