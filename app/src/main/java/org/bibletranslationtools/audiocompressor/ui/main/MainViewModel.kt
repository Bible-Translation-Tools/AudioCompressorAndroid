package org.bibletranslationtools.audiocompressor.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import org.bibletranslationtools.audiocompressor.audio.ConvertAudio
import java.io.File

class MainViewModel : ViewModel() {

    val outZipProperty: MutableLiveData<File?> by lazy {
        MutableLiveData<File?>()
    }

    val inProgressProperty = MutableLiveData(false)

    fun convertZip(workDir: File, zip: File) {
        val zipper = ZipFile(zip)
        inProgressProperty.value = true
        val dest = File(workDir, "/unzip/" + zip.nameWithoutExtension)
        val zipName = zip.name
        val final = File(workDir, zipName)
        Completable.fromCallable {
            try {
                zipper.extractAll(dest.absolutePath)
            } catch (e: ZipException) {
                e.printStackTrace()
            }
            zip.delete()
            convertAudio(dest)
            rezip(dest, final)
            Unit
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                inProgressProperty.value = false
                outZipProperty.value = final
            }
    }

    fun convertAudio(dir: File) {
        ConvertAudio.convertDir(dir)
    }

    fun rezip(dir: File, output: File){
        val zp = ZipFile(output.absolutePath)
        zp.createZipFileFromFolder(dir, ZipParameters(), false, 0)
    }
}
