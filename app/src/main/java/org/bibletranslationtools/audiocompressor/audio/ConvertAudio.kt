package org.bibletranslationtools.audiocompressor.audio

import java.io.File
import javazoom.jl.converter.Converter
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

class ConvertAudio {
    private val pool = Executors.newFixedThreadPool(8) as ThreadPoolExecutor

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
                            File(file.parentFile, file.name.substringBefore(".mp3") + ".wav")
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

    fun mp3ToWav(mp3: File, wav: File) {
        val c = Converter()
        c.convert(mp3.absolutePath, wav.absolutePath)
        mp3.delete()
    }

    fun wavToMp3(wav: File, mp3: File) {
        val mp3Args = arrayOf(
            "-q", "5",
            "-m", "m",
            wav.absolutePath,
            mp3.absolutePath
        )
        de.sciss.jump3r.Main().run(mp3Args)
        wav.delete()
    }
}