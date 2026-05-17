package com.fitness.training.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fitness.training.R

object ExerciseImageLoader {
    
    fun loadExerciseImage(imageView: ImageView, imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.ic_fitness)
        } else {
            Glide.with(imageView.context)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_fitness)
                .error(R.drawable.ic_fitness)
                .centerCrop()
                .into(imageView)
        }
    }
}
