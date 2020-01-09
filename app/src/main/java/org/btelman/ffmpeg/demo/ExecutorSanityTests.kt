package org.btelman.ffmpeg.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.android.synthetic.main.activity_executor_sanity_tests.*
import org.btelman.ffmpeg.Executor

class ExecutorSanityTests : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_executor_sanity_tests)
        startOnBackgroundThread()
        startOnUIThread()
    }

    fun startOnUIThread(){
        Log.e("Activity", "Thread ${Thread.currentThread().id}")
        val threadId = Thread.currentThread().id
        textView1.append("UIThread\n")
        Executor(
            OnStart = {
                assertCurrentThread(threadId)
                textView1.append("OnStart\n")
                Log.d("Activity", "Start ${Thread.currentThread().id}")
            },
            OnProcess = {
                textView1.append("OnProcess\n")
                assertCurrentThread(threadId)
                Log.d("OnProcess", "OnProcess ${Thread.currentThread().id}")
            },
            OnProgress = {
                textView1.append("$it\n")
                assertCurrentThread(threadId)
                Log.d("Activity", "OnProgress $it ${Thread.currentThread().id}")
            },
            OnError = {
                textView1.append("E: $it\n")
                assertCurrentThread(threadId)
                Log.e("Activity", "OnError $it ${Thread.currentThread().id}")
            },
            OnComplete = {
                textView1.append("Completed with exit code $it\n")
                assertCurrentThread(threadId)
                Log.d("Activity", "OnComplete $it ${Thread.currentThread().id}")
            }
        ).execute("ip addr")
    }

    fun startOnBackgroundThread(){
        val ht = HandlerThread("Test").also { it.start() }
        val handlerHt = Handler(ht.looper)
        handlerHt.post {
            Log.e("Activity", "Thread ${Thread.currentThread().id}")
            val threadId = Thread.currentThread().id
            runOnUiThread { textView2.append("Background Thread\n") }
            Executor(
                OnStart = {
                    assertCurrentThread(threadId)
                    runOnUiThread { textView2.append("OnStart\n") }
                    Log.d("Activity", "Start ${Thread.currentThread().id}")
                },
                OnProcess = {
                    runOnUiThread { textView2.append("OnProcess\n") }
                    assertCurrentThread(threadId)
                    Log.d("OnProcess", "OnProcess ${Thread.currentThread().id}")
                },
                OnProgress = {
                    runOnUiThread { textView2.append("$it\n") }
                    assertCurrentThread(threadId)
                    Log.d("Activity", "OnProgress $it ${Thread.currentThread().id}")
                },
                OnError = {
                    runOnUiThread { textView2.append("E: $it\n") }
                    assertCurrentThread(threadId)
                    Log.e("Activity", "OnError $it ${Thread.currentThread().id}")
                },
                OnComplete = {
                    runOnUiThread { textView2.append("Completed with exit code $it\n") }
                    assertCurrentThread(threadId)
                    Log.d("Activity", "OnComplete $it ${Thread.currentThread().id}")
                }
            ).also { it.setEventThread() }.execute("ip addr")
        }
    }

    private fun assertCurrentThread(threadId: Long) {
        val currentThread = Thread.currentThread().id
        assert(threadId == currentThread){
            "$threadId does not match current thread of $currentThread"
        }
    }
}
