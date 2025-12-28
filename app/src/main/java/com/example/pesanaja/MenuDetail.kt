package com.example.pesanaja

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.example.pesanaja.entities.MenuModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.NumberFormat
import java.util.Locale

// Callback diupdate: Mengirim Qty, LevelId, ExtraCost, dan Notes kembali ke Activity
class MenuDetail(
    private val menu: MenuModel,
    private val currentQty: Int,
    private val onSave: (Int, Int?, Int, String) -> Unit
) : BottomSheetDialogFragment() {

    private var qty = 1
    private var selectedLevelId: Int? = null
    private var selectedExtraCost: Int = 0

    // Data Level (Hardcoded sementara, idealnya dari API)
    private val levelNames = listOf("Level 0 (Netral)", "Level 1", "Level 2", "Level 3", "Level 4", "Level 5 (+100)", "Level 6 (+200)", "Level 9 (+500)", "Immortality (+500)", "Heavenly Demon (+1000)")
    private val levelIds = listOf(1, 2, 3, 4, 5, 6, 7, 10, 14, 15)
    private val extraCosts = listOf(0, 0, 0, 0, 0, 100, 200, 500, 500, 1000)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Pastikan nama layout XML sesuai dengan yang kita buat tadi (bottom_sheet_menu)
        return inflater.inflate(R.layout.bottom_sheet_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inisialisasi Semua View (Termasuk yang dikembalikan)
        val tvQty = view.findViewById<TextView>(R.id.tvSheetQty)
        val btnPlus = view.findViewById<ImageButton>(R.id.btnSheetPlus)
        val btnMinus = view.findViewById<ImageButton>(R.id.btnSheetMinus)
        val btnSave = view.findViewById<Button>(R.id.btnSheetSave)
        val spinnerLevel = view.findViewById<Spinner>(R.id.spinnerLevel)
        val tvLevelLabel = view.findViewById<TextView>(R.id.tvLevelLabel)
        val tvPrice = view.findViewById<TextView>(R.id.tvSheetPrice)

        // Set nilai awal qty dari adapter (jika sudah ada di keranjang)
        qty = if (currentQty > 0) currentQty else 1
        tvQty.text = qty.toString()

        // 2. Logic Tombol Plus & Minus
        btnPlus.setOnClickListener {
            qty++
            tvQty.text = qty.toString()
        }

        btnMinus.setOnClickListener {
            if (qty > 1) {
                qty--
                tvQty.text = qty.toString()
            }
        }

        // 3. Logic Spinner Level (Tetap dipertahankan)
        if (menu.hasLevel == 1 && !menu.levels.isNullOrEmpty()) {
            val listNames = menu.levels.map { "${it.name} (+Rp ${it.extraCost})" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerLevel.adapter = adapter

            spinnerLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    val level = menu.levels[pos]
                    selectedLevelId = level.id
                    selectedExtraCost = level.extraCost
                    updatePriceDisplay(tvPrice)
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
            tvLevelLabel.visibility = View.VISIBLE
            spinnerLevel.visibility = View.VISIBLE
        }

        // 4. Tombol Simpan (Sekarang mengirim qty asli hasil klik plus-minus)
        btnSave.setOnClickListener {
            if (menu.hasLevel == 1 && selectedLevelId == null) {
                Toast.makeText(context, "Pilih level dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mengirim 'qty' yang sudah dimanipulasi tombol plus/minus
            onSave(qty, selectedLevelId, selectedExtraCost, "")
            dismiss()
        }
    }

    // Fungsi update harga biar rapi
    private fun updatePriceDisplay(tvPrice: TextView) {
        val totalSatuPorsi = menu.price + selectedExtraCost
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)

        if (selectedExtraCost > 0) {
            tvPrice.text = "${numberFormat.format(totalSatuPorsi)}"
            // Opsional: tvPrice.text = "${numberFormat.format(totalSatuPorsi)} (+Level)"
        } else {
            tvPrice.text = numberFormat.format(totalSatuPorsi)
        }
    }
}