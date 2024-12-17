package com.rk.libcommons.editor

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.Toast
import com.rk.libcommons.CustomScope
import com.rk.libcommons.application
import io.github.rosemoe.sora.text.ContentIO
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.Charset

@Suppress("NOTHING_TO_INLINE")
class KarbonEditor : CodeEditor {
    constructor(context: Context) : super(context)
    
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    
    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)
    
    val scope = CustomScope()
    
    
    suspend fun loadFile(file:File,encoding:Charset){
        withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream = FileInputStream(file)
                val content = ContentIO.createFrom(inputStream,encoding)
                inputStream.close()
                withContext(Dispatchers.Main) { setText(content) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main){
                    Toast.makeText(context,e.message,Toast.LENGTH_LONG).show()
                }
               
            }
        }
    }
    
    
    
    fun showSuggestions(yes: Boolean) {
        inputType = if (yes) {
            InputType.TYPE_TEXT_VARIATION_NORMAL
        } else {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
    }
    
    inline fun isShowSuggestion(): Boolean {
        return inputType != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }
    
    suspend fun saveToFile(file:File,encoding:Charset){
        try {
            withContext(Dispatchers.IO){
                val content = withContext(Dispatchers.Main) { text }
                val outputStream = FileOutputStream(file, false)
                ContentIO.writeTo(content, outputStream,encoding, true)
            }
        }catch (e:Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(application!!,e.message,Toast.LENGTH_SHORT).show()
            }
        }
        
    }
    private var isSearching: Boolean = false
    
    fun isSearching(): Boolean {
        return isSearching
    }
    
    fun setSearching(s: Boolean) {
        isSearching = s
    }
    
    
    
    
}