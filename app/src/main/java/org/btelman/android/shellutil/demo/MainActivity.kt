package org.btelman.android.shellutil.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.btelman.android.shellutil.BinaryUpdateChecker
import org.btelman.android.shellutil.Executor
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ffmpegAssetLocation =
            BinaryUpdateChecker.GetPreferredBinaryLocation(
                assets,
                "ffmpeg"
            )
        ffmpegAssetLocation ?: run {
            Toast.makeText(this, "FFmpeg not supported!", Toast.LENGTH_SHORT).show()
            return
            //app does not support ffmpeg?
        }

        val ffmpegFile =
            BinaryUpdateChecker.GetPreferredInstallPath(
                this,
                "ffmpeg"
            )
        val upToDate =
            BinaryUpdateChecker.CheckBinaryCorrectVersion(
                assets.open(ffmpegAssetLocation),
                ffmpegFile
            )
        Log.d("MainActivity", "BinaryUpdateChecker up to date ($ffmpegAssetLocation) = $upToDate")
        textDebugMain.append("BinaryUpdateChecker up to date ($ffmpegAssetLocation) = $upToDate\n")

        if(!upToDate){
            textDebugMain.append("BinaryUpdateChecker updating using $ffmpegAssetLocation...\n")
            Log.d("MainActivity", "BinaryUpdateChecker updating using $ffmpegAssetLocation...")
            BinaryUpdateChecker.copyAsset(
                assets.open(
                    ffmpegAssetLocation
                ), ffmpegFile
            )
            textDebugMain.append("BinaryUpdateChecker Done!\n")
            Log.d("MainActivity", "BinaryUpdateChecker Done!")
        }

        EnsureBigBuckBunnyCopied()
        EncodeToMov()
    }

    /**
     * Sample execution of ffmpeg. Not sure why we want Mov, but good for testing
     */
    private fun EncodeToMov() {
        val ffmpegFile = File(filesDir, "ffmpeg")
        val android = File(filesDir, "bigbuckbunny.mp4")
        val output = File(filesDir, "output.mov")
        if(output.exists())
            output.delete()
        Executor(
            OnStart = {
                textDebugMain.append("OnStart\n")
                Log.d("Activity", "Start")
            },
            OnProcess = {
                textDebugMain.append("OnProcess\n")
                Log.d("OnProcess", "OnProcess")
            },
            OnProgress = {
                textDebugMain.append("$it\n")
                Log.d("Activity", "OnProgress")
            },
            OnError = {
                textDebugMain.append("E: $it\n")
                Log.e("Activity", "OnError $it")
            },
            OnComplete = {
                textDebugMain.append("Completed with exit code $it\n")
                Log.d("Activity", "OnComplete $it")
            }
        ).execute("${ffmpegFile.absolutePath} -i ${android.absolutePath} ${output.absolutePath}")
    }

    private fun EnsureBigBuckBunnyCopied() {
        val filename = "bigbuckbunny.mp4"
        val android = File(filesDir, "")
        val fileAsset = assets.open(filename)

        val upToDate =
            BinaryUpdateChecker.CheckBinaryCorrectVersion(
                fileAsset,
                android
            )
        Log.d("MainActivity", "BinaryUpdateChecker $filename up to date = $upToDate")
        textDebugMain.append("BinaryUpdateChecker $filename up to date = $upToDate\n")

        if(!upToDate){
            textDebugMain.append("BinaryUpdateChecker updating $filename...\n")
            Log.d("MainActivity", "BinaryUpdateChecker updating $filename...")
            BinaryUpdateChecker.copyAsset(
                fileAsset,
                android
            )
            textDebugMain.append("BinaryUpdateChecker $filename Done!\n")
            Log.d("MainActivity", "BinaryUpdateChecker $filename Done!")
        }
    }
}
