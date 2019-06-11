package com.bmcreations.constrainttabs

import android.content.Context
import android.util.AttributeSet
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

/**
 * Class which translates a tabs XML file into a [List<Tab>]
 */
class TabInflater(val context: Context) {

    companion object {
        const val TAG_ID = "id"
        const val TAG_LABEL = "label"
        const val TAG_ICON = "icon"
        const val TAG_HIDE_LABEL = "hideLabel"
    }

    fun inflate(tabsResId: Int): List<Tab> {
        val res = context.resources
        val xmlParser = res.getXml(tabsResId)
        val attrs = Xml.asAttributeSet(xmlParser)

        var tab: Tab?
        val tabs: MutableList<Tab> = mutableListOf()

        try {
            var type = xmlParser.eventType
            while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
                type = xmlParser.next()
            }

            if (type != XmlPullParser.START_TAG) {
                throw XmlPullParserException("No start tag found")
            }

            while (type != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    val rootElement = xmlParser.name
                    if (rootElement.equals("tab", ignoreCase = true)) {
                        tab = inflateTab(attrs)
                        tab?.let { tabs.add(it) }
                    }
                }
                type = xmlParser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return tabs
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun inflateTab(attrs: AttributeSet): Tab? {
        var label: String? = null
        var hideLabel = false
        var id: Int? = null
        for (i in 0 until attrs.attributeCount) {
            attrs.getAttributeName(i).also {
                when (it) {
                    TAG_ID -> {
                        attrs.getAttributeResourceValue(i, -1).let { ret ->
                            if (ret > 0) {
                                id = ret
                            }
                        }
                    }
                    TAG_LABEL -> label = attrs.getAttributeValue(i)
                    TAG_HIDE_LABEL -> hideLabel = attrs.getAttributeBooleanValue(i, false)
                }
            }
        }

        if (label == null) {
            throw XmlPullParserException("tab doesn't specify a label")
        }

        return Tab(id, label = label!!, hideLabel = hideLabel)
    }
}
