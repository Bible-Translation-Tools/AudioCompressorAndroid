package org.bibletranslationtools.audiocompressor.ui.main

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import io.reactivex.disposables.Disposable
import org.bibletranslationtools.audiocompressor.databinding.MainFragmentBinding
import java.io.File

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private var _binding: MainFragmentBinding? = null
    private val binding get() = _binding!!
    private val disposables = mutableSetOf<Disposable>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainFragmentBinding.inflate(inflater, container, false)
        binding.fileSelectBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "application/zip"
            startActivityForResult(intent, 42)
        }
        binding.outputBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, 43)
        }
        binding.outputBtn.visibility = View.INVISIBLE

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        disposables.forEach { it.dispose() }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        val outZipObserver = Observer<File?> {
            it?.let{
                binding.outputBtn.visibility = View.VISIBLE
            } ?: run {
                binding.outputBtn.visibility = View.INVISIBLE
            }
        }

        val inProgressObserver = Observer<Boolean> {
            disableUIIfInProgress(it)
        }

        disableUIIfInProgress(viewModel.inProgressProperty.value!!)

        viewModel.outZipProperty.observe(this, outZipObserver)
        viewModel.inProgressProperty.observe(this, inProgressObserver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            42 -> {
                if (resultCode == Activity.RESULT_OK) {
                    val uri = data?.data
                    uri?.let { uri ->
                        println(uri.path)
                        activity?.let { act ->
                            DocumentFile.fromSingleUri(act, uri)?.let { doc ->
                                if (hasEnoughSpace(doc.length())) {
                                    val root = act.cacheDir
                                    val del = root.deleteRecursively()
                                    println("del is $del")
                                    println("free space is ${root.freeSpace}")
                                    val workspace = File(root, "workspace")
                                    if (workspace.exists()) workspace.deleteRecursively()
                                    workspace.mkdirs()

                                    val out = File(workspace, doc.name)
                                    try {
                                        val instream = act.contentResolver.openInputStream(uri)
                                        val outstream = out.outputStream()
                                        instream.copyTo(outstream)
                                        instream.close()
                                        outstream.close()
                                        viewModel.convertZip(workspace, out)
                                    } finally {
                                        // out.delete()
                                        println("delet")
                                    }
                                } else {
                                    Toast
                                        .makeText(act, "Not Enough Space", Toast.LENGTH_LONG)
                                        .show()
                                }
                            }
                        }
                    }
                }
            }
            43 -> {
                if (resultCode == Activity.RESULT_OK) {
                    val uri = data?.data
                    uri?.let { path ->
                        activity?.let { act ->
                            val tree = DocumentFile.fromTreeUri(act, path)
                            val outFile = tree?.createFile("application/zip", viewModel.outZipProperty.value!!.name)
                            val outstream = act.contentResolver.openOutputStream(outFile!!.uri)
                            val instream = viewModel.outZipProperty.value!!.inputStream()
                            instream.copyTo(outstream)
                            outstream.close()
                            instream.close()
                        }
                    }
                }
            }
        }
    }

    private fun hasEnoughSpace(fileSize: Long): Boolean {
        val stats = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        return stats.availableBytes > fileSize
    }

    private fun disableUIIfInProgress(isRunning: Boolean) {
        if (isRunning) {
            binding.fileSelectBtn.isEnabled = false
            binding.outputBtn.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.fileSelectBtn.isEnabled = true
            binding.outputBtn.isEnabled = true
            binding.progressBar.visibility = View.INVISIBLE
        }
    }
}
