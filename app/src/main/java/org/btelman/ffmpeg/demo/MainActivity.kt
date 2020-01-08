package org.btelman.ffmpeg.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.btelman.ffmpeg.Executor

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Executor(
            OnStart={

            },
            OnProgress = {

            },
            OnError = {

            },
            OnComplete = {

            }
        ).execute("ip addr")
    }
}
