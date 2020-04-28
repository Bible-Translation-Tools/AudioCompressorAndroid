package org.bibletranslationtools.audiocompressor.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import org.bibletranslationtools.audiocompressor.R
import org.bibletranslationtools.audiocompressor.databinding.MainFragmentBinding
import java.io.File

const val INPUT_RESULT = Activity.RESULT_FIRST_USER
const val OUTPUT_RESULT = Activity.RESULT_FIRST_USER + 1

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
            startActivityForResult(intent, INPUT_RESULT)
        }
        binding.outputBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, OUTPUT_RESULT)
        }
        binding.convertBtn.setOnClickListener {
            if (viewModel.inZipProperty.value != null && viewModel.outPathUriProperty.value != null) {
                viewModel.convertZip()
            }
        }
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

        val inputFileObserver = Observer<File?> {
            it?.let {
                binding.selectedFileView.text = it.name
            } ?: run {
                binding.selectedFileView.text = ""
            }
        }
        val outputFileObserver = Observer<DocumentFile?> {
            it?.let {
                binding.outputPathView.text = it.name
            } ?: run {
                binding.outputPathView.text = ""
            }
        }
        val inProgressObserver = Observer<Boolean> {
            disableUIIfInProgress(it)
        }

        disableUIIfInProgress(viewModel.inProgressProperty.value!!)

        viewModel.inProgressProperty.observe(this, inProgressObserver)
        viewModel.inZipProperty.observe(this, inputFileObserver)
        viewModel.outPathUriProperty.observe(this, outputFileObserver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            INPUT_RESULT -> {
                if (resultCode == Activity.RESULT_OK) {
                    val uri = data?.data
                    if (uri != null) {
                        activity?.let {
                            copyInput(it, uri)
                        }
                    }
                }
            }
            OUTPUT_RESULT -> {
                if (resultCode == Activity.RESULT_OK) {
                    val uri = data?.data
                    if (uri != null) {
                        activity?.let {
                            configureOutput(it, uri)
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
            binding.convertBtn.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.fileSelectBtn.isEnabled = true
            binding.outputBtn.isEnabled = true
            binding.convertBtn.isEnabled = true
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun copyInput(act: Context, uri: Uri) {
        DocumentFile.fromSingleUri(act, uri)?.let { doc ->
            if (hasEnoughSpace(doc.length())) {
                val workspace = viewModel.workDir
                workspace.deleteRecursively()
                workspace.mkdirs()
                val out = File(workspace, doc.name)
                act.contentResolver.openInputStream(uri).use { instream ->
                    instream?.let {
                        out.outputStream().use { outstream ->
                            instream.copyTo(outstream)
                        }
                    }
                }
                if (out.exists() && out.length() > 0) {
                    viewModel.inZipProperty.value = out
                } else {
                    Toast.makeText(act, R.string.load_file_failed, Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(act, R.string.hdd_full, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun configureOutput(act: Context, path: Uri) {
        val tree = DocumentFile.fromTreeUri(act, path)
        tree?.let {
            viewModel.outPathUriProperty.value = it
        }
    }
}
