package org.bibletranslationtools.audiocompressor.audio

import java.io.File
import javazoom.jl.converter.Converter
import org.bibletranslationtools.cuesheetmanager.CueSheetWriter
import org.bibletranslationtools.cuesheetmanager.CueWavWriter
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

class ConvertAudio {
    private val pool = Executors.newFixedThreadPool(4) as ThreadPoolExecutor

    fun convertDir(dir: File) {
        val start = System.currentTimeMillis()
        val tasks = arrayListOf<Future<*>>()
        if (!dir.isDirectory) {
            return
        }
        val walk = dir.walkTopDown()
        walk.forEach { file ->
            tasks.add(
                pool.submit {
                    when (file.extension) {
                        "mp3" -> mp3ToWav(
                            file,
                            File(file.parentFile, file.name.substringBefore(".mp3") + ".wav"),
                            File(file.parentFile, file.name.substringBefore(".mp3") + ".cue")
                        )
                        "wav" -> wavToMp3(
                            file,
                            File(file.parentFile, file.name.substringBefore(".wav") + ".mp3")
                        )
                    }
                }
            )
        }
        tasks.forEach { it.get() }
        pool.shutdown()
    }

    fun mp3ToWav(mp3: File, wav: File, cue: File?) {
        val c = Converter()
        c.convert(mp3.absolutePath, wav.absolutePath)
        mp3.delete()

        if (cue != null && cue.exists()) {
            CueWavWriter(cue).write()
            cue.delete()
        }
    }

    fun wavToMp3(wav: File, mp3: File) {
        val mp3Args = arrayOf(
            "-q", "9",
            "-m", "m",
            wav.absolutePath,
            mp3.absolutePath
        )
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.M) {
            try {
                de.sciss.jump3r.Main().run(mp3Args)
            } catch(ex: IOException) {
                val message = ex.message
                if (message == null || !message.contains("BufferedOutputStream is closed")) {
                    throw ex
                }
            }
        } else {
            de.sciss.jump3r.Main().run(mp3Args)
        }
        CueSheetWriter(wav).write()
        wav.delete()
    }
}