package com.fitness.training.ui.training

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fitness.training.R

class TrainingContainerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_training_container, container, false)
        
        // 直接显示训练页面
        childFragmentManager.beginTransaction()
            .replace(R.id.container, TrainingFragment())
            .commit()
        
        return view
    }
}
