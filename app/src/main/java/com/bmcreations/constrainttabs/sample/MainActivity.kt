package com.bmcreations.constrainttabs.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bmcreations.constrainttabs.ConstraintTabLayout
import com.bmcreations.constrainttabs.Tab
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

class MainActivity: AppCompatActivity() {

    val tabList by lazy {
        arrayOf(Tab("first"), Tab("second"), Tab("third"), Tab("fourth"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabs.addTabs(*tabList)

        tabs.callback = object : ConstraintTabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: Tab) {
                toast("selected ${tab.string()}")
            }

            override fun onTabReselected(tab: Tab) {
                toast("reselected ${tab.string()}")
            }
        }
    }
}

private fun Tab.string(): String {
    return "Tab{index=${this.index}, label=${this.label}}"
}