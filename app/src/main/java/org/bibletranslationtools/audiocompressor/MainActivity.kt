package org.bibletranslationtools.audiocompressor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.bibletranslationtools.audiocompressor.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }

}
