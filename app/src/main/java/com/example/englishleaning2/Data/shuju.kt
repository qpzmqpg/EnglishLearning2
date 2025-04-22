package com.example.englishleaning2.Data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import android.os.Parcel
import android.os.Parcelable

data class WordEntry(
    val id: Long,
    val word: String,
    val sw: String,
    val phonetic: String?,
    val definition: String?,
    val translation: String?,
    val pos: String?,
    val collins: Int,
    val oxford: Int,
    val tag: String?,
    val bnc: Int?,
    val frq: Int?,
    val exchange: String?,
    val detail: JSONObject?,
    val audio: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        word = parcel.readString()!!,
        sw = parcel.readString()!!,
        phonetic = parcel.readString(),
        definition = parcel.readString(),
        translation = parcel.readString(),
        pos = parcel.readString(),
        collins = parcel.readInt(),
        oxford = parcel.readInt(),
        tag = parcel.readString(),
        bnc = parcel.readInt(),
        frq = parcel.readInt(),
        exchange = parcel.readString(),
        detail = parcel.readString()?.let { JSONObject(it) },
        audio = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(word)
        parcel.writeString(sw)
        parcel.writeString(phonetic)
        parcel.writeString(definition)
        parcel.writeString(translation)
        parcel.writeString(pos)
        parcel.writeInt(collins)
        parcel.writeInt(oxford)
        parcel.writeString(tag)
        parcel.writeInt(bnc ?: -1)
        parcel.writeInt(frq ?: -1)
        parcel.writeString(exchange)
        parcel.writeString(detail?.toString())
        parcel.writeString(audio)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WordEntry> {
        override fun createFromParcel(parcel: Parcel): WordEntry {
            return WordEntry(parcel)
        }

        override fun newArray(size: Int): Array<WordEntry?> {
            return arrayOfNulls(size)
        }
    }
}

class StarDict(context: Context, private val verbose: Boolean = false) {
    private val db: SQLiteDatabase
    private val dbPath: String
    init {
        // 确保数据库文件路径正确
        val dbFile = context.getDatabasePath("ecdict.db").apply {
            parentFile?.takeIf { !it.exists() }?.mkdirs()
        }
        // 首次运行时复制数据库
        if (!dbFile.exists()) {
            copyDatabase(context, dbFile)
        }
        dbPath = dbFile.absolutePath  // ✅ 正确初始化路径
        db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
        createTablesIfNeeded()  // 创建必要的表结构
    }
    private fun copyDatabase(context: Context, target: File) {
        try {
            context.assets.open("ecdict.db").use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output, bufferSize = 1024)
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Database copy failed: ${e.message}")
        }
    }
    private fun createTablesIfNeeded() {
        val createSQL = """
            CREATE TABLE IF NOT EXISTS stardict (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,
                word VARCHAR(64) NOT NULL UNIQUE COLLATE NOCASE,
                sw VARCHAR(64) NOT NULL COLLATE NOCASE,
                phonetic VARCHAR(64),
                definition TEXT,
                translation TEXT,
                pos VARCHAR(16),
                collins INTEGER DEFAULT 0,
                oxford INTEGER DEFAULT 0,
                tag VARCHAR(64),
                bnc INTEGER,
                frq INTEGER,
                exchange TEXT,
                detail TEXT,
                audio TEXT
            );
        """.trimIndent()
        db.execSQL(createSQL)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_word ON stardict(word);")
    }


    fun query(word: String): WordEntry? {
        val cursor = db?.rawQuery(
            "SELECT * FROM stardict WHERE word = ? COLLATE NOCASE",
            arrayOf(word)
        )

        return cursor?.use {
            if (it.moveToFirst()) parseEntry(it) else null
        }
    }

    fun match(word: String, limit: Int = 10, strip: Boolean = false): List<Pair<Long, String>> {
        val sql = if (strip) {
            "SELECT id, word FROM stardict WHERE sw >= ? ORDER BY sw, word COLLATE NOCASE LIMIT ?"
        } else {
            "SELECT id, word FROM stardict WHERE word >= ? ORDER BY word COLLATE NOCASE LIMIT ?"
        }

        return db?.rawQuery(sql, arrayOf(word, limit.toString()))?.use { cursor ->
            val result = mutableListOf<Pair<Long, String>>()
            while (cursor.moveToNext()) {
                result.add(cursor.getLong(0) to cursor.getString(1))
            }
            result
        } ?: emptyList()
    }

    private fun parseEntry(cursor: android.database.Cursor): WordEntry {
        return WordEntry(
            id = cursor.getLong(0),
            word = cursor.getString(1),
            sw = cursor.getString(2),
            phonetic = cursor.getString(3),
            definition = cursor.getString(4),
            translation = cursor.getString(5),
            pos = cursor.getString(6),
            collins = cursor.getInt(7),
            oxford = cursor.getInt(8),
            tag = cursor.getString(9),
            bnc = cursor.getIntOrNull(10),
            frq = cursor.getIntOrNull(11),
            exchange = cursor.getString(12),
            detail = cursor.getString(13)?.let {
                try { JSONObject(it) } catch (e: Exception) { null }
            },
            audio = cursor.getString(14)
        )
    }

    fun close() {
        db?.close()
    }

    // 其他方法实现类似，根据具体需求添加...

    private fun android.database.Cursor.getIntOrNull(index: Int): Int? {
        return if (isNull(index)) null else getInt(index)
    }
}
