package com.example.englishleaning2

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.englishleaning2.Data.StarDict
import com.example.englishleaning2.Data.WordEntry
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.englishleaning2.Data.HistoryDatabaseHelper
import com.example.englishleaning2.Data.HistoryEntry

class MainActivity : AppCompatActivity() {
    private lateinit var starDict: StarDict
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyDb: HistoryDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化字典和历史数据库
        starDict = StarDict(applicationContext)
        historyDb = HistoryDatabaseHelper(applicationContext)

        // 历史记录RecyclerView初始化
        historyRecyclerView = findViewById(R.id.history_recyclerview)
        historyAdapter = HistoryAdapter(historyDb.getAllHistory()) { word ->
            queryWord(word)
        }
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.adapter = historyAdapter

        // 搜索框与按钮
        val searchEditText = findViewById<EditText>(R.id.search_edittext)
        val searchButton = findViewById<ImageButton>(R.id.search_button)

        // 搜索按钮点击事件
        val searchAction = {
            val word = searchEditText.text.toString().trim()
            if (word.isNotEmpty()) {
                queryWord(word)
                searchEditText.setText("") // 清空搜索框
            }
        }

        // 设置搜索按钮点击监听
        searchButton.setOnClickListener { searchAction() }

        // 设置搜索框回车键监听
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)
            ) {
                searchAction()
                true
            } else {
                false
            }
        }

        // 示例查询（注意：应在线程中执行）
        // Thread { queryWord("example") }.start()
    }

    private fun queryWord(word: String) {
        Thread {
            val entry = starDict.query(word)
            runOnUiThread {
                entry?.let {
                    // 启动WordDetailActivity并传递WordEntry数据
                    val intent = android.content.Intent(this@MainActivity, WordDetailActivity::class.java)
                    intent.putExtra("word_entry", it)
                    startActivity(intent)
                    addToHistory(word)
                }
            }
        }.start()
    }

    override fun onDestroy() {
        starDict.close()
        super.onDestroy()
    }


    private fun addToHistory(word: String) {
        Thread {
            val entry = starDict.query(word)
            entry?.let {
                historyDb.addHistory(word, it.phonetic, it.translation)
                runOnUiThread {
                    historyAdapter.updateData(historyDb.getAllHistory())
                    historyRecyclerView.scrollToPosition(0)
                }
            }
        }.start()
    }
}

// 历史记录适配器
class HistoryAdapter(private var data: List<HistoryEntry>, val onClick: (String) -> Unit) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    inner class HistoryViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val wordText: TextView = view.findViewById(R.id.word_text)
        val phoneticText: TextView = view.findViewById(R.id.phonetic_text)
        val translationText: TextView = view.findViewById(R.id.translation_text)
        val lastReviewTime: TextView = view.findViewById(R.id.last_review_time)
        val playPronunciation: ImageButton = view.findViewById(R.id.play_pronunciation)
        val addToWordbook: ImageButton = view.findViewById(R.id.add_to_wordbook)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): HistoryViewHolder {
        val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_word_history, parent, false)
        return HistoryViewHolder(v)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val entry = data[position]
        holder.wordText.text = entry.word
        holder.phoneticText.text = entry.phonetic ?: ""
        holder.translationText.text = entry.translation ?: ""
        holder.lastReviewTime.text = "上次查询时间：${entry.timestamp}"
        
        holder.itemView.setOnClickListener { onClick(entry.word) }
        holder.playPronunciation.setOnClickListener { /* 实现发音功能 */ }
        holder.addToWordbook.setOnClickListener { /* 实现添加到单词本功能 */ }
    }
    override fun getItemCount() = data.size

    fun updateData(newData: List<HistoryEntry>) {
        data = newData
        notifyDataSetChanged()
    }
}
