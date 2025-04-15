package com.mparticle.rokt.example

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val attributes = mapOf(
            Pair("lastname", "Smith"),
            Pair("mobile", "1112223333"),
            Pair("country", "USA"),
            Pair("noFunctional", "true"),
            Pair("email", "testEmail_${System.currentTimeMillis()}@example.com"),
            Pair("sandbox", "true")
        )
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val roktKit = (this.application as MainApplication).kit
        findViewById<Button>(R.id.executeButton).setOnClickListener {
            Log.d("ExampleActivity", "Button clicked")
            roktKit.execute(
                "mauitest",
                attributes,
                Runnable { },
                Runnable { },
                Runnable { },
                Runnable { },
                mutableMapOf("Location1" to WeakReference(findViewById(R.id.roktEmbeddedView))),
                null,
                null
            )
        }
    }
}
