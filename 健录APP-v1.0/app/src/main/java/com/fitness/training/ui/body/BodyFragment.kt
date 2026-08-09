package com.fitness.training.ui.body

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitness.training.R
import com.fitness.training.data.entity.BodyRecord
import com.fitness.training.utils.AnimationUtils.addClickScaleEffect
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BodyFragment : Fragment() {

    private lateinit var viewModel: BodyViewModel
    private lateinit var adapter: BodyRecordAdapter
    
    private lateinit var btnBack: ImageButton
    private lateinit var tvCurrentWeight: TextView
    private lateinit var tvCurrentBodyFat: TextView
    private lateinit var tvWeightChange: TextView
    private lateinit var tvBodyfatChange: TextView
    private lateinit var btnAddRecord: MaterialButton
    private lateinit var rvRecords: RecyclerView
    private lateinit var lineChart: LineChart
    
    private var allRecordsData: List<BodyRecord> = emptyList()  // 保存所有数据
    
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    private val chartDateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_body, container, false)
        viewModel = ViewModelProvider(this)[BodyViewModel::class.java]
        initViews(view)
        setupRecyclerView()
        setupObservers()
        setupListeners()
        return view
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btn_back)
        tvCurrentWeight = view.findViewById(R.id.tv_current_weight)
        tvCurrentBodyFat = view.findViewById(R.id.tv_current_body_fat)
        tvWeightChange = view.findViewById(R.id.tv_weight_change)
        tvBodyfatChange = view.findViewById(R.id.tv_bodyfat_change)
        btnAddRecord = view.findViewById(R.id.btn_add_record)
        rvRecords = view.findViewById(R.id.rv_records)
        lineChart = view.findViewById(R.id.line_chart)
        
        // 添加按钮点击缩放效果
        btnAddRecord.addClickScaleEffect()
        
        setupChart()
    }

    private fun setupRecyclerView() {
        adapter = BodyRecordAdapter(
            onEditClick = { showEditDialog(it) },
            onDeleteClick = { showDeleteDialog(it) }
        )
        rvRecords.layoutManager = LinearLayoutManager(requireContext())
        rvRecords.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.latestRecord.observe(viewLifecycleOwner) { record ->
            tvCurrentWeight.text = record?.weight?.let { String.format("%.1f", it) } ?: "-"
            tvCurrentBodyFat.text = record?.bodyFat?.let { String.format("%.1f", it) } ?: "-"
        }
        
        viewModel.allRecords.observe(viewLifecycleOwner) { records ->
            adapter.submitList(records)
            allRecordsData = records  // 保存所有数据
            updateChart(records)
        }
    }
    
    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)  // 允许缩放
            setPinchZoom(true)     // 允许双指缩放
            setDrawGridBackground(false)
            legend.textSize = 11f
            
            // X轴设置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textSize = 10f
                granularity = 1f
            }
            
            // 左Y轴（体重）
            axisLeft.apply {
                setDrawGridLines(true)
                textSize = 10f
                // 不设置固定值，由updateChart动态设置
            }
            
            // 右Y轴（体脂）
            axisRight.apply {
                setDrawGridLines(false)
                textSize = 10f
                // 体脂率固定0-100范围
                axisMinimum = 0f
                axisMaximum = 100f
                setLabelCount(6, false)
                granularity = 10f  // 最小间隔10
            }
        }
    }
    
    private fun updateChart(records: List<BodyRecord>) {
        if (records.isEmpty()) {
            lineChart.clear()
            return
        }
        
        // 显示全部记录，按时间倒序（最新的在右边）
        val allRecords = records.reversed()
        
        val weightEntries = mutableListOf<Entry>()
        val bodyFatEntries = mutableListOf<Entry>()
        val dates = mutableListOf<String>()
        
        allRecords.forEachIndexed { index, record ->
            record.weight?.let { weightEntries.add(Entry(index.toFloat(), it)) }
            record.bodyFat?.let { bodyFatEntries.add(Entry(index.toFloat(), it)) }
            dates.add(chartDateFormat.format(Date(record.date)))
        }
        
        val dataSets = mutableListOf<LineDataSet>()
        
        // 体重线
        if (weightEntries.isNotEmpty()) {
            val weightDataSet = LineDataSet(weightEntries, "体重(kg)").apply {
                color = Color.parseColor("#FF6B6B")
                setCircleColor(Color.parseColor("#FF6B6B"))
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT
            }
            dataSets.add(weightDataSet)
        }
        
        // 体脂线
        if (bodyFatEntries.isNotEmpty()) {
            val bodyFatDataSet = LineDataSet(bodyFatEntries, "体脂(%)").apply {
                color = Color.parseColor("#4ECDC4")
                setCircleColor(Color.parseColor("#4ECDC4"))
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                axisDependency = com.github.mikephil.charting.components.YAxis.AxisDependency.RIGHT
            }
            dataSets.add(bodyFatDataSet)
        }
        
        if (dataSets.isNotEmpty()) {
            val lineData = LineData(dataSets.toList())
            lineChart.data = lineData
            
            // 动态设置左Y轴（体重）范围
            if (weightEntries.isNotEmpty()) {
                val minWeight = weightEntries.minOf { it.y }
                val maxWeight = weightEntries.maxOf { it.y }
                val range = maxWeight - minWeight
                val padding = if (range > 0) range * 0.1f else 5f  // 10%边距或至少5kg
                
                lineChart.axisLeft.apply {
                    axisMinimum = (minWeight - padding).coerceAtLeast(0f)
                    axisMaximum = maxWeight + padding
                    
                    // 根据范围动态调整间隔
                    val adjustedRange = axisMaximum - axisMinimum
                    granularity = when {
                        adjustedRange <= 10 -> 1f   // 范围<=10kg，间隔1kg
                        adjustedRange <= 20 -> 2f   // 范围<=20kg，间隔2kg
                        adjustedRange <= 50 -> 5f   // 范围<=50kg，间隔5kg
                        else -> 10f                 // 范围>50kg，间隔10kg
                    }
                    setLabelCount(6, false)
                }
            }
            
            // 设置X轴日期标签
            lineChart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < dates.size) dates[index] else ""
                }
            }
            
            // 设置可见范围（一次显示10个数据点）
            lineChart.setVisibleXRangeMaximum(10f)
            
            // 移动到最右边（显示最新数据）
            lineChart.moveViewToX(allRecords.size.toFloat() - 1)
            
            // 设置图表手势监听
            lineChart.onChartGestureListener = object : com.github.mikephil.charting.listener.OnChartGestureListener {
                override fun onChartGestureStart(me: android.view.MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                override fun onChartGestureEnd(me: android.view.MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {
                    // 滑动结束后更新统计
                    updateStatistics()
                }
                override fun onChartLongPressed(me: android.view.MotionEvent?) {}
                override fun onChartDoubleTapped(me: android.view.MotionEvent?) {}
                override fun onChartSingleTapped(me: android.view.MotionEvent?) {}
                override fun onChartFling(me1: android.view.MotionEvent?, me2: android.view.MotionEvent?, velocityX: Float, velocityY: Float) {}
                override fun onChartScale(me: android.view.MotionEvent?, scaleX: Float, scaleY: Float) {
                    // 缩放时实时更新
                    updateStatistics()
                }
                override fun onChartTranslate(me: android.view.MotionEvent?, dX: Float, dY: Float) {
                    // 滑动时实时更新
                    updateStatistics()
                }
            }
            
            lineChart.invalidate()
            
            // 初始化统计信息
            updateStatistics()
        }
    }
    
    private fun updateStatistics() {
        if (allRecordsData.isEmpty()) {
            tvWeightChange.text = "--"
            tvBodyfatChange.text = "--"
            return
        }
        
        // 获取当前可见区域的范围
        val lowestVisibleX = lineChart.lowestVisibleX
        val highestVisibleX = lineChart.highestVisibleX
        
        val startIndex = lowestVisibleX.toInt().coerceIn(0, allRecordsData.size - 1)
        val endIndex = highestVisibleX.toInt().coerceIn(0, allRecordsData.size - 1)
        
        // 获取可见区域首尾的数据（注意数据是reversed的）
        val reversedData = allRecordsData.reversed()
        val startRecord = reversedData.getOrNull(startIndex)
        val endRecord = reversedData.getOrNull(endIndex)
        
        if (startRecord != null && endRecord != null && startIndex != endIndex) {
            // 计算体重变化
            val startWeight = startRecord.weight
            val endWeight = endRecord.weight
            if (startWeight != null && endWeight != null) {
                val weightChange = endWeight - startWeight
                val weightPercent = (weightChange / startWeight * 100)
                val sign = if (weightChange >= 0) "+" else ""
                tvWeightChange.text = String.format("%s%.1fkg (%.1f%%)", sign, weightChange, weightPercent)
                tvWeightChange.setTextColor(
                    resources.getColor(
                        if (weightChange >= 0) R.color.error else R.color.success,
                        null
                    )
                )
            } else {
                tvWeightChange.text = "--"
            }
            
            // 计算体脂变化
            val startBodyFat = startRecord.bodyFat
            val endBodyFat = endRecord.bodyFat
            if (startBodyFat != null && endBodyFat != null) {
                val bodyFatChange = endBodyFat - startBodyFat
                val bodyFatPercent = (bodyFatChange / startBodyFat * 100)
                val sign = if (bodyFatChange >= 0) "+" else ""
                tvBodyfatChange.text = String.format("%s%.1f%% (%.1f%%)", sign, bodyFatChange, bodyFatPercent)
                tvBodyfatChange.setTextColor(
                    resources.getColor(
                        if (bodyFatChange >= 0) R.color.error else R.color.success,
                        null
                    )
                )
            } else {
                tvBodyfatChange.text = "--"
            }
        } else {
            tvWeightChange.text = "--"
            tvBodyfatChange.text = "--"
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        btnAddRecord.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_body_record, null)
        
        val layoutDate = dialogView.findViewById<LinearLayout>(R.id.layout_date)
        val tvDate = dialogView.findViewById<TextView>(R.id.tv_date)
        val etWeight = dialogView.findViewById<TextInputEditText>(R.id.et_weight)
        val etBodyFat = dialogView.findViewById<TextInputEditText>(R.id.et_body_fat)
        val etNote = dialogView.findViewById<TextInputEditText>(R.id.et_note)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)
        
        var selectedDate = Calendar.getInstance()
        tvDate.text = "今天"
        
        layoutDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate = Calendar.getInstance().apply {
                        set(year, month, day, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val today = Calendar.getInstance()
                    tvDate.text = if (year == today.get(Calendar.YEAR) && 
                        month == today.get(Calendar.MONTH) && 
                        day == today.get(Calendar.DAY_OF_MONTH)) {
                        "今天"
                    } else {
                        dateFormat.format(selectedDate.time)
                    }
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val weight = etWeight.text.toString().toFloatOrNull()
            val bodyFat = etBodyFat.text.toString().toFloatOrNull()
            val note = etNote.text.toString()
            
            if (weight == null && bodyFat == null) {
                Toast.makeText(requireContext(), "请至少填写一项数据", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.addRecord(weight, bodyFat, note, selectedDate.timeInMillis) {
                Toast.makeText(requireContext(), "记录已添加", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }

    private fun showEditDialog(record: BodyRecord) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_body_record, null)
        
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_title)
        val layoutDate = dialogView.findViewById<LinearLayout>(R.id.layout_date)
        val tvDate = dialogView.findViewById<TextView>(R.id.tv_date)
        val etWeight = dialogView.findViewById<TextInputEditText>(R.id.et_weight)
        val etBodyFat = dialogView.findViewById<TextInputEditText>(R.id.et_body_fat)
        val etNote = dialogView.findViewById<TextInputEditText>(R.id.et_note)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)
        
        tvTitle.text = "编辑记录"
        
        var selectedDate = Calendar.getInstance().apply { timeInMillis = record.date }
        tvDate.text = dateFormat.format(Date(record.date))
        
        record.weight?.let { etWeight.setText(String.format("%.1f", it)) }
        record.bodyFat?.let { etBodyFat.setText(String.format("%.1f", it)) }
        etNote.setText(record.note)
        
        layoutDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate = Calendar.getInstance().apply {
                        set(year, month, day, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    tvDate.text = dateFormat.format(selectedDate.time)
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val weight = etWeight.text.toString().toFloatOrNull()
            val bodyFat = etBodyFat.text.toString().toFloatOrNull()
            val note = etNote.text.toString()
            
            if (weight == null && bodyFat == null) {
                Toast.makeText(requireContext(), "请至少填写一项数据", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val updated = record.copy(
                weight = weight,
                bodyFat = bodyFat,
                note = note,
                date = selectedDate.timeInMillis
            )
            viewModel.updateRecord(updated) {
                Toast.makeText(requireContext(), "记录已更新", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }

    private fun showDeleteDialog(record: BodyRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除记录")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteRecord(record) {
                    Toast.makeText(requireContext(), "记录已删除", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
