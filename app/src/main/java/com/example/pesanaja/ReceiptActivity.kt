package com.example.pesanaja

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.pesanaja.entities.CartItem
import com.example.pesanaja.entities.OrderResponse
import com.example.pesanaja.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReceiptActivity : AppCompatActivity() {

    private lateinit var btnAction: Button
    private lateinit var tvStatus: TextView // Tambahin ini biar bisa diakses global

    // Data Order
    private var orderData: OrderResponse? = null
    private var currentStatus: String = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        // 1. Inisialisasi View
        btnAction = findViewById(R.id.btnPayNow)
        tvStatus = findViewById(R.id.tvStatusOrder) // Inisialisasi TextView Status

        // 2. Ambil Data dari Intent
        orderData = intent.getSerializableExtra("order_response") as? OrderResponse
        val cartList = intent.getSerializableExtra("cart_list") as? ArrayList<CartItem> ?: arrayListOf()
        val meja = intent.getStringExtra("meja") ?: "0"

        val order = orderData?.orderData
        currentStatus = order?.status ?: "pending"

        // 3. Setup Teks Statis
        findViewById<TextView>(R.id.tvReceiptInfo).text = """
            Order ID: #${order?.id ?: "---"}
            Pelanggan: ${order?.customerName ?: "---"}
            Meja: $meja
        """.trimIndent()

        findViewById<TextView>(R.id.tvTimestamp).text = order?.createdAt ?: "---"

        // 4. Render List Item
        val container = findViewById<LinearLayout>(R.id.containerItems)
        cartList.forEach { item ->
            val tvItem = TextView(this)
            val infoLevel = if (item.extraCost > 0) " (+Level)" else ""
            val totalItem = (item.price + item.extraCost) * item.quantity
            tvItem.text = "${item.menuName}$infoLevel x${item.quantity} - Rp $totalItem"
            tvItem.textSize = 14f
            tvItem.setPadding(0, 4, 0, 4)
            container.addView(tvItem)
        }

        findViewById<TextView>(R.id.tvReceiptSubtotal).text = "Rp ${order?.subtotal?.toInt() ?: 0}"
        findViewById<TextView>(R.id.tvReceiptPajak).text = "Rp ${order?.taxAmount?.toInt() ?: 0}"
        findViewById<TextView>(R.id.tvReceiptGrandTotal).text = "Rp ${order?.finalTotal?.toInt() ?: 0}"

        // 5. PANGGIL FUNGSI UPDATE TAMPILAN
        updateTampilanStatus()
    }

    // --- FUNGSI BARU: UPDATE SEMUA (TEKS + WARNA + TOMBOL) ---
    private fun updateTampilanStatus() {
        // Ubah background & teks berdasarkan status
        when (currentStatus) {
            "pending" -> {
                tvStatus.text = "BELUM BAYAR"
                tvStatus.setTextColor(Color.parseColor("#C62828")) // Merah
                btnAction.visibility = View.VISIBLE
                btnAction.text = "Bayar Sekarang"
                btnAction.setOnClickListener {
                    if (orderData != null) {
                        showPaymentDialog(orderData!!)
                    } else {
                        Toast.makeText(this, "Data order hilang", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "processing" -> {
                tvStatus.text = "LUNAS / DIPROSES"
                tvStatus.setTextColor(Color.parseColor("#F57C00")) // Oranye
                // 🎯 SOLUSI: Sembunyikan tombol agar tidak bisa diklik lagi
                btnAction.text = "Selesai & Kembali ke Menu"
                btnAction.backgroundTintList = getColorStateList(android.R.color.darker_gray)
                btnAction.setOnClickListener {
                    val i = Intent(this, MainActivity::class.java) // Atau MenuActivity
                    i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(i)
                    finish()
                }
            }
            "completed" -> {
                tvStatus.text = "LUNAS / SELESAI"
                tvStatus.setTextColor(Color.parseColor("#2E7D32")) // Hijau
                btnAction.text = "Selesai & Kembali ke Menu"
                btnAction.backgroundTintList = getColorStateList(android.R.color.darker_gray)
                btnAction.setOnClickListener {
                    val i = Intent(this, MainActivity::class.java) // Atau MenuActivity
                    i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(i)
                    finish()
                }
            }
            "canceled" -> {
                tvStatus.text = "DIBATALKAN"
                tvStatus.setTextColor(Color.GRAY)
                btnAction.visibility = View.GONE
            }
        }
    }

    private fun showPaymentDialog(dataOrder: OrderResponse) {
        val dialogBuilder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.payment, null)

        val tvTotal = view.findViewById<TextView>(R.id.tvTotalBayarDialog)
        val etPin = view.findViewById<EditText>(R.id.etPinPayment)
        val btnBayar = view.findViewById<Button>(R.id.btnProsesBayar)
        val btnBatal = view.findViewById<TextView>(R.id.btnBatalBayar)

        val totalHarga = dataOrder.orderData?.finalTotal?.toInt() ?: 0
        tvTotal.text = "Rp $totalHarga"

        dialogBuilder.setView(view)
        val dialog = dialogBuilder.create()
        dialog.setCancelable(false)

        btnBayar.setOnClickListener {
            val pin = etPin.text.toString()
            if (pin == "123456") {
                btnBayar.text = "Memproses..."
                btnBayar.isEnabled = false
                verifikasiPembayaran(dataOrder.orderData?.id ?: 0, dialog)
            } else {
                etPin.error = "PIN Salah!"
            }
        }

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun verifikasiPembayaran(orderId: Int, dialog: AlertDialog) {
        // 🎯 Matikan tombol agar tidak bisa diklik lagi selama menunggu respon API
        btnAction.isEnabled = false

        ApiClient.instance.payOrder(orderId).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                dialog.dismiss()
                if (response.isSuccessful) {
                    // Update status lokal berdasarkan apa yang dikirim Laravel (processing)
                    currentStatus = response.body()?.orderData?.status ?: "processing"
                    updateTampilanStatus()
                    Toast.makeText(this@ReceiptActivity, "Pembayaran Berhasil!", Toast.LENGTH_SHORT).show()
                } else {
                    btnAction.isEnabled = true // Hidupkan lagi jika gagal
                    Toast.makeText(this@ReceiptActivity, "Gagal Verifikasi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                dialog.dismiss()
                btnAction.isEnabled = true // Hidupkan lagi jika error koneksi
                Toast.makeText(this@ReceiptActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}