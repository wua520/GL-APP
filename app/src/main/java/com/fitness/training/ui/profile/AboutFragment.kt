package com.fitness.training.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fitness.training.R

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_about, container, false)
        
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // 显示版本号
        val versionName = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName
        view.findViewById<TextView>(R.id.tv_version).text = "版本 $versionName"
        
        // 点击邮箱复制
        view.findViewById<TextView>(R.id.tv_email).setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("email", "3033636395@qq.com")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "邮箱已复制", Toast.LENGTH_SHORT).show()
        }
        
        return view
    }
}
