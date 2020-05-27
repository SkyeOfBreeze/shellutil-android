package org.btelman.android.shellutil

import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.btelman.logutil.kotlin.LogLevel
import org.btelman.logutil.kotlin.LogUtil
import org.btelman.logutil.kotlin.LogUtilInstance
import java.io.BufferedReader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Brendon on 1/7/2020.
 */
class Executor(
    val OnStart : () -> Unit,
    val OnProcess : (Process) -> Unit,
    val OnProgress : (String) -> Unit,
    val OnError : (String) -> Unit,
    val OnComplete : (Int?)->Unit
) : AsyncTask<String, String?, Int?>() {
    private var log = LogUtil("Executor",
        logInstance
    )
    private var process: Process? = null

    private var atomicKillSwitch = AtomicBoolean(false)
    private var killWithGarbageData = AtomicBoolean(false)
    private var handler : Handler? = null
    var allowReadLogsOnInputStream = true

    /**
     * Set thread for the events to run on. If this is not called, the UI thread will be used
     */
    fun setEventThread(){
        log.d("setEventThread")
        Looper.myLooper() ?: runCatching { Looper.prepare() }  //create a looper if none exists
        handler = Handler(Looper.myLooper())
    }

    override fun onPreExecute() {
        super.onPreExecute()
        handler?: run{
            handler = Handler(Looper.getMainLooper())
        }
        handler?.post(OnStart)
        log.d("onPreExecute")
    }

    override fun doInBackground(vararg params: String?): Int? {
        var error : String?
        var errorReader: BufferedReader? = null
        var inputReader: BufferedReader? = null
        try {
            process = Runtime.getRuntime().exec(params) ?: return null
            errorReader = process?.errorStream?.bufferedReader()
            if(allowReadLogsOnInputStream)
                inputReader = process?.inputStream?.bufferedReader()
            handler?.post { process ?.let(OnProcess) }
            while(!atomicKillSwitch.get()){
                error = null
                if(errorReader?.ready() == true){
                    error = errorReader.readLine()
                }
                else if(inputReader?.ready() == true){
                    error = inputReader.readLine()
                }

                if(error != null)
                    publishProgress(error)
                else if(!isProcessRunning()) //only see about breaking out of loop if there is no more data to read
                    break
            }
            if(killWithGarbageData.get()){ //kill switch was tripped
                process?.outputStream?.write("die".toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            publishProgress(e.toString())
        }
        try{
            errorReader?.close()
            inputReader?.close()
            process?.inputStream?.close()
            process?.destroy()
            process?.waitFor()
            return process?.exitValue()
        } catch (e: Exception) {
            handler?.post {
                OnError(e.toString())
            }
        }
        return null
    }

    private fun isProcessRunning(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process?.isAlive == true
            } else {
                process?.exitValue() == null //will only return false usually
            }
        }catch (e : Exception){
            true //if process?.exitValue() crashes, the process is running.
            // Need to find a better way to check if it is running...
        }
    }

    override fun onProgressUpdate(vararg values: String?) {
        super.onProgressUpdate(*values)
        handler?.post {
            values[0]?.let {
                log.d("Progress: $it")
                OnProgress(it)
            }
        }
    }

    override fun onPostExecute(result: Int?) {
        super.onPostExecute(result)
        handler?.post { OnComplete(result) }
        log.d("OnComplete: $result")
    }

    /**
     * Kill the thread by sending garbage data to the process output stream.
     * if killSafely does not appear to work.
     *
     * Sends a bytearray that says "die" to the process.
     * If the program likes UTF8 Text being sent to it, this will not work
     */
    fun killWithGarbageData(){
        killWithGarbageData.set(true)
        killSafely()
    }

    fun killSafely(){
        atomicKillSwitch.set(true)
    }

    fun execute(command : String){
        val list = command.split(" ").map { //split by space
            it.replace(CHARACTER_SPACE, " ") //now add spaces back to the original params
        }.toTypedArray()
        executeOnExecutor(THREAD_POOL_EXECUTOR, *list)
    }

    companion object{
        /**
         * Space character to be used to preserve spaces after splitting based on space
         *
         * "\u0020"
         */
        const val CHARACTER_SPACE = "\\u0020"
        var logInstance = LogUtilInstance("ffmpeg-lib", LogLevel.DEBUG).also {
            LogUtil.addCustomLogUtilInstance("ffmpeg-lib", it)
        }
    }
}