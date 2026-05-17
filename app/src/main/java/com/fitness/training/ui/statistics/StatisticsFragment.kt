package com.fitness.training.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fitness.training.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.*

class StatisticsFragment : Fragment() {

    private lateinit var viewModel: StatisticsViewModel
    private lateinit var tvWeekWorkouts: TextView
    private lateinit var tvWeekVolume: TextView
    private lateinit var tvWeekDuration: TextView
    private lateinit var layoutMuscleStats: LinearLayout
    private lateinit var layoutPrList: LinearLayout
    private lateinit var chartStrength: LineChart
    private lateinit var chartVolume: BarChart
    private lateinit var spinnerExercise: Spinner
    private lateinit var tvChartEmpty: TextView
    private lateinit var tvVolumeChartEmpty: TextView
    private lateinit var chipGroupVolumePeriod: ChipGroup
    private lateinit var chipDaily: Chip
    private lateinit var chipWeekly: Chip
    private lateinit var chipMonthly: Chip

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)
        
        viewModel = ViewModelProvider(this)[StatisticsViewModel::class.java]
        
        initViews(view)
        setupChart()
        setupVolumeChart()
        setupSpinner()
        setupVolumePeriodChips()
        observeData()
        
        return view
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
    
    private fun initViews(view: View) {
        tvWeekWorkouts = view.findViewById(R.id.tv_week_workouts)
        tvWeekVolume = view.findViewById(R.id.tv_week_volume)
        tvWeekDuration = view.findViewById(R.id.tv_week_duration)
        layoutMuscleStats = view.findViewById(R.id.layout_muscle_stats)
        layoutPrList = view.findViewById(R.id.layout_pr_list)
        chartStrength = view.findViewById(R.id.chart_strength)
        chartVolume = view.findViewById(R.id.chart_volume)
        spinnerExercise = view.findViewById(R.id.spinner_exercise)
        tvChartEmpty = view.findViewById(R.id.tv_chart_empty)
        tvVolumeChartEmpty = view.findViewById(R.id.tv_volume_chart_empty)
        chipGroupVolumePeriod = view.findViewById(R.id.chip_group_volume_period)
        chipDaily = view.findViewById(R.id.chip_daily)
        chipWeekly = view.findViewById(R.id.chip_weekly)
        chipMonthly = view.findViewById(R.id.chip_monthly)
    }
    
    private fun setupChart() {
        chartStrength.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            
            // X轴设置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = resources.getColor(R.color.text_secondary, null)
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    private val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        return dateFormat.format(Date(value.toLong()))
                    }
                }
            }
            
            // Y轴设置
            axisLeft.apply {
                textColor = resources.getColor(R.color.text_secondary, null)
                setDrawGridLines(true)
                gridColor = resources.getColor(R.color.light_gray, null)
            }
            axisRight.isEnabled = false
            
            // 图例
            legend.apply {
                textColor = resources.getColor(R.color.text_secondary, null)
                isEnabled = false
            }
        }
    }
    
    private fun setupVolumeChart() {
        chartVolume.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            
            // X轴设置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = resources.getColor(R.color.text_secondary, null)
                textSize = 10f
                setDrawGridLines(false)
                granularity = 1f
                setDrawAxisLine(false)
            }
            
            // Y轴设置
            axisLeft.apply {
                textColor = resources.getColor(R.color.text_secondary, null)
                textSize = 10f
                setDrawGridLines(true)
                gridColor = resources.getColor(R.color.light_gray, null)
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            
            // 图例
            legend.isEnabled = false
        }
    }
    
    private fun setupSpinner() {
        spinnerExercise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val exerciseName = spinnerExercise.selectedItem as? String
                if (!exerciseName.isNullOrEmpty() && exerciseName != "选择动作") {
                    viewModel.loadStrengthCurve(exerciseName)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupVolumePeriodChips() {
        chipGroupVolumePeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    R.id.chip_daily -> {
                        viewModel.dailyVolumeData.value?.let { updateVolumeChart(it) }
                    }
                    R.id.chip_weekly -> {
                        viewModel.weeklyVolumeData.value?.let { updateVolumeChart(it) }
                    }
                    R.id.chip_monthly -> {
                        viewModel.monthlyVolumeData.value?.let { updateVolumeChart(it) }
                    }
                }
            }
        }
    }
    
    private fun observeData() {
        viewModel.weekWorkouts.observe(viewLifecycleOwner) { count ->
            tvWeekWorkouts.text = count.toString()
        }
        
        viewModel.weekVolume.observe(viewLifecycleOwner) { volume ->
            tvWeekVolume.text = if (volume >= 1000) {
                String.format("%.1f", volume)
            } else {
                String.format("%.0f", volume)
            }
        }
        
        viewModel.weekDuration.observe(viewLifecycleOwner) { duration ->
            val minutes = duration / 60000
            tvWeekDuration.text = "$minutes"
        }
        
        viewModel.muscleStats.observe(viewLifecycleOwner) { stats ->
            updateMuscleStats(stats)
        }
        
        viewModel.personalRecords.observe(viewLifecycleOwner) { records ->
            updatePersonalRecords(records)
        }
        
        viewModel.exerciseList.observe(viewLifecycleOwner) { exercises ->
            updateExerciseSpinner(exercises)
        }
        
        viewModel.strengthCurveData.observe(viewLifecycleOwner) { data ->
            updateStrengthChart(data)
        }
        
        viewModel.monthlyVolumeData.observe(viewLifecycleOwner) { data ->
            // 只有当月选中时才更新
            if (chipMonthly.isChecked) {
                updateVolumeChart(data)
            }
        }
        
        viewModel.dailyVolumeData.observe(viewLifecycleOwner) { data ->
            // 只有当日选中时才更新
            if (chipDaily.isChecked) {
                updateVolumeChart(data)
            }
        }
        
        viewModel.weeklyVolumeData.observe(viewLifecycleOwner) { data ->
            // 只有当周选中时才更新
            if (chipWeekly.isChecked) {
                updateVolumeChart(data)
            }
        }
    }
    
    private fun updateExerciseSpinner(exercises: List<String>) {
        if (exercises.isEmpty()) {
            spinnerExercise.visibility = View.GONE
            chartStrength.visibility = View.GONE
            tvChartEmpty.visibility = View.VISIBLE
            tvChartEmpty.text = "完成训练后可查看力量变化趋势"
            return
        }
        
        spinnerExercise.visibility = View.VISIBLE
        val items = listOf("选择动作") + exercises
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, items)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerExercise.adapter = adapter
    }
    
    private fun updateStrengthChart(data: List<StrengthDataPoint>) {
        if (data.isEmpty() || data.size < 2) {
            chartStrength.visibility = View.GONE
            tvChartEmpty.visibility = View.VISIBLE
            tvChartEmpty.text = if (data.isEmpty()) "暂无数据" else "数据不足，至少需要2次训练记录"
            return
        }
        
        chartStrength.visibility = View.VISIBLE
        tvChartEmpty.visibility = View.GONE
        
        val entries = data.mapIndexed { index, point ->
            Entry(point.date.toFloat(), point.weight.toFloat())
        }
        
        val dataSet = LineDataSet(entries, "重量 (kg)").apply {
            color = resources.getColor(R.color.primary, null)
            setCircleColor(resources.getColor(R.color.primary, null))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextColor = resources.getColor(R.color.text_secondary, null)
            valueTextSize = 10f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = resources.getColor(R.color.primary, null)
            fillAlpha = 30
        }
        
        chartStrength.data = LineData(dataSet)
        chartStrength.invalidate()
    }
    
    private fun updateVolumeChart(data: List<VolumePoint>) {
        if (data.isEmpty()) {
            chartVolume.visibility = View.GONE
            tvVolumeChartEmpty.visibility = View.VISIBLE
            return
        }
        
        chartVolume.visibility = View.VISIBLE
        tvVolumeChartEmpty.visibility = View.GONE
        
        val entries = data.mapIndexed { index, point ->
            BarEntry(index.toFloat(), point.volume.toFloat())
        }
        
        val dataSet = BarDataSet(entries, "训练容量 (kg)").apply {
            color = resources.getColor(R.color.primary, null)
            valueTextColor = resources.getColor(R.color.text_secondary, null)
            valueTextSize = 9f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value >= 1000) {
                        String.format("%.1fk", value / 1000)
                    } else {
                        String.format("%.0f", value)
                    }
                }
            }
        }
        
        val barData = BarData(dataSet).apply {
            barWidth = 0.6f
        }
        
        chartVolume.xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.label })
        chartVolume.xAxis.labelCount = data.size
        chartVolume.data = barData
        chartVolume.invalidate()
    }
    
    private fun updateMuscleStats(stats: List<MuscleGroupStat>) {
        layoutMuscleStats.removeAllViews()
        
        if (stats.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "暂无数据"
                setTextColor(resources.getColor(R.color.text_secondary, null))
                textSize = 14f
            }
            layoutMuscleStats.addView(emptyText)
            return
        }
        
        val maxSets = stats.maxOfOrNull { it.setCount } ?: 1
        
        stats.forEach { stat ->
            val itemView = layoutInflater.inflate(R.layout.item_muscle_stat, layoutMuscleStats, false)
            
            itemView.findViewById<TextView>(R.id.tv_muscle_name).text = stat.muscleGroup
            itemView.findViewById<TextView>(R.id.tv_set_count).text = "${stat.setCount}组"
            
            // 进度条
            val progressBar = itemView.findViewById<View>(R.id.progress_bar)
            val params = progressBar.layoutParams
            val progress = (stat.setCount.toFloat() / maxSets * 100).toInt()
            progressBar.post {
                val parentWidth = (progressBar.parent as View).width
                params.width = (parentWidth * progress / 100)
                progressBar.layoutParams = params
            }
            
            layoutMuscleStats.addView(itemView)
        }
    }
    
    private fun updatePersonalRecords(records: List<PersonalRecord>) {
        layoutPrList.removeAllViews()
        
        if (records.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "暂无记录"
                setTextColor(resources.getColor(R.color.text_secondary, null))
                textSize = 14f
            }
            layoutPrList.addView(emptyText)
            return
        }
        
        val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
        
        records.forEach { record ->
            val itemView = layoutInflater.inflate(R.layout.item_personal_record, layoutPrList, false)
            
            itemView.findViewById<TextView>(R.id.tv_exercise_name).text = record.exerciseName
            itemView.findViewById<TextView>(R.id.tv_weight).text = "${record.weight}kg"
            itemView.findViewById<TextView>(R.id.tv_reps).visibility = View.GONE
            itemView.findViewById<TextView>(R.id.tv_date).text = dateFormat.format(Date(record.date))
            
            layoutPrList.addView(itemView)
        }
    }
}


