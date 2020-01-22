package org.btelman.ffmpeg

import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import android.util.Log
import java.io.*
import java.security.MessageDigest
import java.util.*


/**
 * Utility class for updating binaries.
 */
object BinaryUpdateChecker {
    fun GetPreferredBinaryLocation(assetMananager : AssetManager, name : String) : String?{
        if(Build.VERSION.SDK_INT < 21){
            val path = when(Build.CPU_ABI){
                "x86" -> "x86/$name"
                else -> "armeabi-v7a/$name"
            }
            val result = kotlin.runCatching {
                assetMananager.open(path)
            }
            if(result.isSuccess)
                return path
        }else{
            Build.SUPPORTED_ABIS.forEach {
                val path = when(it){
                    "arm64-v8a" -> "arm64-v8a/$name"
                    "x86" -> "x86/$name"
                    "x86_64" -> "x86_64/$name"
                    else -> "armeabi-v7a/$name"
                }
                val result = kotlin.runCatching {
                    assetMananager.open(path)
                }
                if(result.isSuccess)
                    return path
            }
        }
        return null
    }

    /**
     * Path where the file should be stored. This outputs a desired file and not a directory, but can be used as a directory if needed
     */
    fun GetPreferredInstallPath(context : Context, name : String) : File{
        return File(context.filesDir, name)
    }

    fun CheckBinaryCorrectVersion(assetMananager: AssetManager, name : String, file : File) : Boolean{
        val assetPath = GetPreferredBinaryLocation(assetMananager, name)
        return assetPath?.let{
            CheckBinaryCorrectVersion(assetMananager.open(name), file)
        } ?: false
    }

    fun CheckBinaryCorrectVersion(assetInputStream : InputStream, targetFile : File) : Boolean{
        if(!targetFile.exists())
            return false
        return try {
            val assetFileSha1 = assetInputStream.let { fis ->
                createSha1(fis).also {
                    fis.close()
                }
            }
            val fileSha1 = createSha1(targetFile)
            fileSha1 == assetFileSha1
        } catch (e: Exception) {
            false
        }
    }

    fun copyAsset(assetInputStream : InputStream, targetFile : File) {
        if(targetFile.exists()) targetFile.delete()
        targetFile.createNewFile()
        var `in`: InputStream? = null
        var out: OutputStream? = null
        try {
            `in` = assetInputStream
            out = FileOutputStream(targetFile)
            copyFile(`in`, out)
            targetFile.setExecutable(true)
        } catch (e: IOException) {
            Log.e("tag", "Failed to copy asset file: ${targetFile.name}", e)
        } finally {
            if (`in` != null) {
                try {
                    `in`.close()
                } catch (e: IOException) { // NOOP
                }
            }
            if (out != null) {
                try {
                    out.close()
                } catch (e: IOException) { // NOOP
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream?, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`!!.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }

    @Throws(Exception::class)
    fun createSha1(file: File): String {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-1")
        val fis: InputStream = FileInputStream(file)
        var n = 0
        val buffer = ByteArray(8192)
        while (n != -1) {
            n = fis.read(buffer)
            if (n > 0) {
                digest.update(buffer, 0, n)
            }
        }
        val sha1 : ByteArray = digest.digest()
        fis.close()
        return byteArray2Hex(sha1)
    }

    @Throws(Exception::class)
    fun createSha1(fis: InputStream): String {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-1")
        var n = 0
        val buffer = ByteArray(8192)
        while (n != -1) {
            n = fis.read(buffer)
            if (n > 0) {
                digest.update(buffer, 0, n)
            }
        }
        val sha1 : ByteArray = digest.digest()
        return byteArray2Hex(sha1)
    }

    private fun byteArray2Hex(hash: ByteArray): String {
        val formatter = Formatter()
        for (b in hash) {
            formatter.format("%02x", b)
        }
        return formatter.toString()
    }
}