package org.btelman.ffmpeg.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.btelman.ffmpeg.Executor

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Executor(
            OnStart={
                Log.d("Activity", "Start ${Thread.currentThread().id}")
            },
            OnProcess = {
                Log.d("OnProcess", "OnProcess ${Thread.currentThread().id}")
            },
            OnProgress = {
                Log.d("Activity", "OnProgress $it ${Thread.currentThread().id}")
            },
            OnError = {
                Log.e("Activity", "OnError $it ${Thread.currentThread().id}")
            },
            OnComplete = {
                Log.d("Activity", "OnComplete $it ${Thread.currentThread().id}")
            }
        ).execute("ip addr")
    }
}
