package com.example.englishleaning2.Data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class HistoryDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "history.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_HISTORY = "history"
        private const val COLUMN_ID = "id"
        private const val COLUMN_WORD = "word"
        private const val COLUMN_PHONETIC = "phonetic"
        private const val COLUMN_TRANSLATION = "translation"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_HISTORY ("
            .plus("$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,")
            .plus("$COLUMN_WORD TEXT NOT NULL,")
            .plus("$COLUMN_PHONETIC TEXT,")
            .plus("$COLUMN_TRANSLATION TEXT,")
            .plus("$COLUMN_TIMESTAMP TEXT NOT NULL)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
    }

    fun addHistory(word: String, phonetic: String?, translation: String?) {
        val db = this.writableDatabase
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val values = ContentValues().apply {
            put(COLUMN_WORD, word)
            put(COLUMN_PHONETIC, phonetic)
            put(COLUMN_TRANSLATION, translation)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        
        // 检查是否已存在相同单词的记录
        val cursor = db.query(TABLE_HISTORY, arrayOf(COLUMN_ID), "$COLUMN_WORD = ?", arrayOf(word), null, null, null)
        if (cursor.moveToFirst()) {
            // 如果存在，更新时间戳
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
            db.update(TABLE_HISTORY, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        } else {
            // 如果不存在，插入新记录
            db.insert(TABLE_HISTORY, null, values)
        }
        cursor.close()
        db.close()
    }

    fun getAllHistory(): List<HistoryEntry> {
        val historyList = mutableListOf<HistoryEntry>()
        val selectQuery = "SELECT * FROM $TABLE_HISTORY ORDER BY $COLUMN_TIMESTAMP DESC"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val entry = HistoryEntry(
                        word = it.getString(it.getColumnIndexOrThrow(COLUMN_WORD)),
                        phonetic = it.getString(it.getColumnIndexOrThrow(COLUMN_PHONETIC)),
                        translation = it.getString(it.getColumnIndexOrThrow(COLUMN_TRANSLATION)),
                        timestamp = it.getString(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    )
                    historyList.add(entry)
                } while (it.moveToNext())
            }
        }
        db.close()
        return historyList
    }

    fun clearHistory() {
        val db = this.writableDatabase
        db.delete(TABLE_HISTORY, null, null)
        db.close()
    }
}

data class HistoryEntry(
    val word: String,
    val phonetic: String?,
    val translation: String?,
    val timestamp: String
)