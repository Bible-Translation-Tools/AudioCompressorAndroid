package org.bibletranslationtools.audiocompressor.ui.main

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
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

class MainViewModel(val app: Application) : AndroidViewModel(app) {

    val workDir = File(app.cacheDir, "workspace")

    val inZipProperty: MutableLiveData<File?> by lazy {
        MutableLiveData<File?>()
    }

    val outZipProperty: MutableLiveData<File?> by lazy {
        MutableLiveData<File?>()
    }

    val outPathUriProperty: MutableLiveData<DocumentFile?> = MutableLiveData<DocumentFile?>(null)

    val inProgressProperty = MutableLiveData(false)
    val completedProperty = MutableLiveData(false)

    fun convertZip() {
        val zip = inZipProperty.value!!
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
                writeOutput()
                outZipProperty.value?.delete()
                workDir.deleteRecursively()
                outZipProperty.value = null
                outPathUriProperty.value = null
                inZipProperty.value = null
                completedProperty.value = true
            }
    }

    private fun convertAudio(dir: File) {
        ConvertAudio().convertDir(dir)
    }

    private fun rezip(dir: File, output: File) {
        val zp = ZipFile(output.absolutePath)
        val params = ZipParameters()
        params.isIncludeRootFolder = false
        zp.createZipFileFromFolder(dir, params, false, 0)
    }

    private fun writeOutput() {
        outPathUriProperty.value?.let {
            val out = it.createFile("application/zip", outZipProperty.value!!.name)
            app.contentResolver.openOutputStream(out!!.uri).use { ofs ->
                outZipProperty.value!!.inputStream().use { ifs ->
                    ifs.copyTo(ofs)
                }
            }
        }
    }
}