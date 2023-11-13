package com.example.foodOrderingSystem.ui.table

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.databinding.FragmentTableCustomerOrderBinding
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.EnumMap
import java.util.Locale

class TableCustomerOrderFragment: Fragment() {

    private var _binding: FragmentTableCustomerOrderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTableCustomerOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply{
            topAppBar.setNavigationOnClickListener { backToPrevious() }
            burgerMenu.setOnClickListener {
                drawerLayout.open()
            }
            tableNavView.setNavigationItemSelectedListener { navDrawerNavigation(it) }
        }
    }

    private fun navDrawerNavigation(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.add_food_item -> {
    //                binding.drawerLayout.closeDrawer(GravityCompat.START)
                true
            }

            R.id.generate_qr_code -> {
//                binding.drawerLayout.closeDrawer(GravityCompat.START)
                openDialogQrCode();
                true
            }

            R.id.print_receipt -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                true
            }

            else -> false
        }
    }

    private fun openDialogQrCode() {
        val inflater = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_generate_qr_code, null)

        val textView: TextView = inflater.findViewById(R.id.expire_view)
        textView.isVisible = false
        val qrImage: ImageView = inflater.findViewById(R.id.qr_image)
        val hourEditText: EditText = inflater.findViewById(R.id.qr_code_expire_hour_edit_text)
        val minuteEditText: EditText = inflater.findViewById(R.id.qr_code_expire_minute_edit_text)
        val qrGenerateButton: Button = inflater.findViewById(R.id.generate_qr_code_button)

        qrGenerateButton.setOnClickListener {
            val baseUrl = "http://10.100.65.59:8080"
            val tableNumber = 7 // Replace with the actual table number
            var hourString = hourEditText.text.toString()
            var minuteString = minuteEditText.text.toString()

            if (hourString.isNotEmpty() && minuteString.isNotEmpty()) {
                val expirationTime = calculateExpirationTime(hourString.toInt(), minuteString.toInt())

                // Generate the QR code with the modified URL
                val qrCode = generateQRCode("$baseUrl?tableNumber=$tableNumber")

                // Display the QR code in an ImageView
                qrImage.setImageBitmap(qrCode)


                textView.text = "Expire Date: " + expirationTime
                textView.isVisible = true

                val hourTextView: TextInputLayout  = inflater.findViewById(R.id.qr_code_expire_hour_field)
                val minuteTextView: TextInputLayout = inflater.findViewById(R.id.qr_code_expire_minute_field)
                hourTextView.isVisible = false
                minuteTextView.isVisible = false
            } else {
                Toast.makeText(requireContext(), "Please enter the time", Toast.LENGTH_SHORT).show()
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(inflater)
            .show()
    }

    private fun generateQRCode(data: String): Bitmap? {
        val width = 300
        val height = 300
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)

        hints[EncodeHintType.MARGIN] = 0
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L

        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }

        return bitmap
    }

    private fun calculateExpirationTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()

        // Set the expiration time
        calendar.add(Calendar.HOUR_OF_DAY, hour)
        calendar.add(Calendar.MINUTE, minute)

        // Format the expiration time
        val sdf = SimpleDateFormat("dd/MM/YYYY HH:mm:ss", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun backToPrevious() {
        Utils().backToPrevious(this, R.id.navigation_table)
    }
}