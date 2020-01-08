package org.btelman.ffmpeg

import android.os.AsyncTask
import android.os.Build
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
    private var log = LogUtil("Executor", logInstance)
    private var process: Process? = null

    private var atomicKillSwitch = AtomicBoolean(false)
    private var killWithGarbageData = AtomicBoolean(false)

    override fun onPreExecute() {
        super.onPreExecute()
        OnStart()
        log.d("onPreExecute")
    }

    override fun doInBackground(vararg params: String?): Int? {
        var error : String?
        var inputLine : String?
        var reader: BufferedReader? = null
        var errorReader: BufferedReader? = null
        try {
            process = Runtime.getRuntime().exec(params) ?: return null
            reader = (process?.inputStream?.bufferedReader() ?: return null)
            errorReader = process?.errorStream?.bufferedReader()
            OnProcess(process!!)
            while(!atomicKillSwitch.get()){
                error = null
                inputLine = null
                if(errorReader?.ready() == true){
                    error = errorReader.readLine()
                }
                if(reader.ready()){
                    inputLine = reader.readLine()
                }
                if(inputLine != null || error != null)
                    publishProgress(inputLine, error)
                if(!isProcessRunning())
                    break
            }
            if(killWithGarbageData.get()){ //kill switch was tripped
                process?.outputStream?.write("die".toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            publishProgress(null, e.toString())
        }
        try{
            reader?.close()
            errorReader?.close()
            process?.inputStream?.close()
            process?.destroy()
            process?.waitFor()
            return process?.exitValue()
        } catch (e: Exception) {

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
        values[0]?.let {
            log.d("Progress: $it")
            OnProgress(it)
        }

        values[1]?.let {
            log.e("Error: $it")
            OnError(it)
        }
    }

    override fun onPostExecute(result: Int?) {
        super.onPostExecute(result)
        OnComplete(result)
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
        val list = command.split(" ").toTypedArray()
        executeOnExecutor(THREAD_POOL_EXECUTOR, *list)
    }

    companion object{
        var logInstance = LogUtilInstance("ffmpeg-lib", LogLevel.DEBUG).also {
            LogUtil.addCustomLogUtilInstance("ffmpeg-lib", it)
        }
    }
}