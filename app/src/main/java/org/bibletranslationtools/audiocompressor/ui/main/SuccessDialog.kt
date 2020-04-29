package org.bibletranslationtools.audiocompressor.ui.main

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import org.bibletranslationtools.audiocompressor.databinding.SuccessDialogBinding


class SuccessDialog : DialogFragment() {

    private var _binding: SuccessDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = SuccessDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button2.setOnClickListener {
            this@SuccessDialog.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        val window = dialog.window ?: return
        val params = window.attributes
        val displayMetrics = DisplayMetrics()
        window.windowManager.defaultDisplay.getMetrics(displayMetrics)
        var height = displayMetrics.heightPixels - (100 + getNavBarHeight())
        var width = displayMetrics.widthPixels - 100
        params.width = width
        params.height = height
        window.attributes = params
    }

    fun getNavBarHeight(): Int {
        val resources = context!!.resources
        val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }
}