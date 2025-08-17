package com.example.iptvtv.util

import com.example.iptvtv.data.model.EPGProgram
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EPGParser @Inject constructor() {
    
    fun parseEPG(xmlContent: String): List<EPGProgram> {
        val programs = mutableListOf<EPGProgram>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))
            
            var eventType = parser.eventType
            var currentProgram: MutableMap<String, String>? = null
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "programme" -> {
                                currentProgram = mutableMapOf()
                                currentProgram["channelId"] = parser.getAttributeValue(null, "channel")
                                currentProgram["start"] = parser.getAttributeValue(null, "start")
                                currentProgram["stop"] = parser.getAttributeValue(null, "stop")
                            }
                            "title" -> {
                                if (currentProgram != null) {
                                    currentProgram["title"] = parser.nextText()
                                }
                            }
                            "desc" -> {
                                if (currentProgram != null) {
                                    currentProgram["description"] = parser.nextText()
                                }
                            }
                            "category" -> {
                                if (currentProgram != null) {
                                    currentProgram["category"] = parser.nextText()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "programme" && currentProgram != null) {
                            val program = createEPGProgram(currentProgram)
                            if (program != null) {
                                programs.add(program)
                            }
                            currentProgram = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return programs
    }
    
    private fun createEPGProgram(data: Map<String, String>): EPGProgram? {
        return try {
            val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault())
            
            val startTime = data["start"]?.let { 
                dateFormat.parse(it)?.time ?: 0L 
            } ?: 0L
            
            val endTime = data["stop"]?.let { 
                dateFormat.parse(it)?.time ?: 0L 
            } ?: 0L
            
            EPGProgram(
                id = UUID.randomUUID().toString(),
                channelId = data["channelId"] ?: "",
                title = data["title"] ?: "",
                description = data["description"],
                startTime = startTime,
                endTime = endTime,
                category = data["category"]
            )
        } catch (e: Exception) {
            null
        }
    }
}
