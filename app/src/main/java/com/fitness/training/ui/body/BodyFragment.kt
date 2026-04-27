package com.fitness.training.ui.body

import android.app.AlertDialog
import android.app.DatePickerDialog
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
    private lateinit var btnAddRecord: MaterialButton
    private lateinit var rvRecords: RecyclerView
    
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

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
        btnAddRecord = view.findViewById(R.id.btn_add_record)
        rvRecords = view.findViewById(R.id.rv_records)
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
