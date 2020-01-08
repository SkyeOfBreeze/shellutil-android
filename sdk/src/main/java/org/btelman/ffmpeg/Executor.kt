package org.btelman.ffmpeg

import android.os.AsyncTask
import org.btelman.logutil.kotlin.LogUtil
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Brendon on 1/7/2020.
 */
class Executor(
    val OnStart : () -> Unit,
    val OnProgress : (String) -> Unit,
    val OnError : (String) -> Unit,
    val OnComplete : (Int?)->Unit
) : AsyncTask<String, String?, Int?>() {
    private var log = LogUtil("Executor")
    private var process: Process? = null

    private var atomicKillSwitch = AtomicBoolean(false)
    private var killWithGarbageData = AtomicBoolean(false)

    override fun onPreExecute() {
        super.onPreExecute()
        OnStart()
        log.d("onPreExecute")
    }

    override fun doInBackground(vararg params: String?): Int? {
        process = Runtime.getRuntime().exec(params) ?: return null
        val reader = process?.inputStream?.bufferedReader() ?: return null
        val errorReader = process?.errorStream?.bufferedReader()
        var error : String?
        var inputLine : String?
        try {
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
            }
            if(killWithGarbageData.get()){ //kill switch was tripped
                process?.outputStream?.write("die".toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            publishProgress(null, e.toString())
        }
        try{
            reader.close()
            errorReader?.close()
            process?.inputStream?.close()
            process?.destroy()
            process?.waitFor()
            return process?.exitValue()
        } catch (e: Exception) {

        }
        return null
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
}