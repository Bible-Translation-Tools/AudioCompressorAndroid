package org.bibletranslationtools.audiocompressor.audio

import java.io.File
import javazoom.jl.converter.Converter

class ConvertAudio {
    companion object {
        fun convertDir(dir: File) {
            if (!dir.isDirectory) {
                return
            }
            val walk = dir.walkTopDown()
            walk.forEach { file ->
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
}