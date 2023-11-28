package com.example.foodOrderingSystem.ui.table

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodOrderingSystem.utils.ConnectionBluetoothManager
import com.example.foodOrderingSystem.PrintPic
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.adapters.OrderItemListAdapter
import com.example.foodOrderingSystem.databinding.FragmentTableCustomerOrderBinding
import com.example.foodOrderingSystem.firestore.Firestore
import com.example.foodOrderingSystem.models.OrderItem
import com.example.foodOrderingSystem.models.OrderItemViewModel
import com.example.foodOrderingSystem.models.Report
import com.example.foodOrderingSystem.models.TableViewModel
import com.example.foodOrderingSystem.utils.Constants
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.EnumMap
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

class TableCustomerOrderFragment: Fragment() {

    private var _binding: FragmentTableCustomerOrderBinding? = null
    private val binding get() = _binding!!
    private var bluetoothManager = ConnectionBluetoothManager.getBluetoothManager()
    private var bluetoothAdapter = ConnectionBluetoothManager.getBluetoothAdapter()
    private var socket = ConnectionBluetoothManager.getBluetoothSocket()
    private var bluetoothDevice = ConnectionBluetoothManager.getBluetoothDevice()
    private var outputStream = ConnectionBluetoothManager.getOutputSteam()
    private var inputStream = ConnectionBluetoothManager.getInputSteam()
    private var workerThread = ConnectionBluetoothManager.getWorkerThread()
    private lateinit var readBuffer: ByteArray
    private var readBufferPosition = 0
    private lateinit var dialog: Dialog
    private lateinit var mProgressDialog: Dialog
    val printPic = PrintPic.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderItemList: MutableLiveData<MutableList<OrderItem>>
    private val orderItemViewModel: OrderItemViewModel by activityViewModels()
    private val tableViewModel: TableViewModel by activityViewModels()

    @Volatile
    var stopWorker = false
    private var value = ""
    private val outputStreamLock = Any()

    private val btActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
//            btScan()
        }

    }

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

        // Initialize Bluetooth when the fragment is created
        ConnectionBluetoothManager.getInstance()

        orderItemList = orderItemViewModel.orderItemList

        recyclerView = binding.orderItemRecycleView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe changes in the tableList LiveData
        orderItemList.observe(viewLifecycleOwner) {
            // Update your RecyclerView adapter when the LiveData changes
            recyclerView.adapter = OrderItemListAdapter(this, requireContext(), orderItemList)
        }

        val tableId = tableViewModel.tableId.value.toString()

        // Get data from firebase
        Firestore().getCustomerOrder(this, recyclerView, orderItemViewModel, tableId)

        binding.apply{
            topAppBar.setNavigationOnClickListener { backToPrevious() }

            val navigationView: NavigationView = binding.tableNavView
            val headerView: View = navigationView.getHeaderView(0)
            val tableNumberTextView: TextView = headerView.findViewById(R.id.table_header_number)
            tableNumberTextView.text = buildString {
                append("Table ")
                append(tableViewModel.tableNumber.value.toString())
            }

            burgerMenu.setOnClickListener {
                drawerLayout.open()
            }
            tableNavView.setNavigationItemSelectedListener { navDrawerNavigation(it) }

            paymentButton.setOnClickListener { openDialogPayment() }

            orderItemViewModel.subTotalPrice.observe(viewLifecycleOwner) { subTotalPrice ->
                // Convert to Double and update UI with subTotalPrice
                subTotalPriceTextView.text = String.format("%.2f", subTotalPrice!!.toDoubleOrNull() ?: 0.0)
            }

            orderItemViewModel.serviceCharge.observe(viewLifecycleOwner) { serviceCharge ->
                // Convert to Double and update UI with serviceCharge
                serviceChargePriceTextView.text = String.format("%.2f", serviceCharge!!.toDoubleOrNull() ?: 0.0)
            }

            orderItemViewModel.roundup.observe(viewLifecycleOwner) { roundup ->
                // Convert to Double and update UI with roundup
                roundUpPriceTextView.text = String.format("%.2f", roundup!!.toDoubleOrNull() ?: 0.0)
            }

            orderItemViewModel.finalTotal.observe(viewLifecycleOwner) { finalTotal ->
                // Convert to Double and update UI with finalTotal
                finalTotalPriceTextView.text = String.format("%.2f", finalTotal!!.toDoubleOrNull() ?: 0.0)
            }

        }
    }

    private fun navDrawerNavigation(menuItem: MenuItem): Boolean {

        return when (menuItem.itemId) {
            R.id.generate_qr_code -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                openDialogQrCode();
                true
            }

            R.id.print_receipt -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                checkPrintReceipt()
                true
            }

            R.id.cancel_order -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                openCancelDialog()
                true
            }

            else -> false
        }
    }

    private fun openDialogPayment() {
        val inflater = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_payment_selection, null)

        val ePayment: TextView = inflater.findViewById(R.id.e_wallet_textview)
        val cashPayment: TextView = inflater.findViewById(R.id.cash_textview)
        val divider: MaterialDivider = inflater.findViewById(R.id.divider)
        val changeTextView: TextView = inflater.findViewById(R.id.display_change_textView)
        val cashInputLayout: TextInputLayout = inflater.findViewById(R.id.cash_enter_textField)
        val cashEditText: EditText = inflater.findViewById(R.id.cash_enter_editText)
        val cashButton: Button = inflater.findViewById(R.id.cash_button)

        val id = UUID.randomUUID().toString()
        val tableId = tableViewModel.tableId.value.toString()
        val tableNumber = tableViewModel.tableNumber.value.toString()

        val itemList = orderItemViewModel.orderItemList.value
        val serviceCharge = orderItemViewModel.serviceCharge.value.toString()
        val totalQuantity = orderItemViewModel.totalQuantity.value.toString()
        val subTotalPrice = orderItemViewModel.subTotalPrice.value.toString()
        val finalTotal = orderItemViewModel.finalTotal.value.toString()

        // Generating current timestamp
//        val currentTimestamp: Timestamp = Timestamp(System.currentTimeMillis())
        val currentTimestamp = Timestamp(System.currentTimeMillis())

        // Using the toInstant() method to get an Instant and then extracting seconds
        val unixTimestampSeconds = currentTimestamp.toInstant().epochSecond
        println("Current Timestamp: $unixTimestampSeconds")

        // Generating current date
        val currentDate: LocalDateTime = LocalDateTime.now()
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss")
        val formattedDate: String = currentDate.format(formatter)
        println("Current Date: $formattedDate")

        val timeStamp = com.google.firebase.Timestamp.now()

        var dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(inflater)
            .show()

        ePayment.setOnClickListener {
            Firestore().updateCustomerPaymentMethod(this, tableId, Constants.EPAYMENT)

            val report = Report (
                id,
                tableId,
                tableNumber,
                unixTimestampSeconds.toString(),
                formattedDate,
                totalQuantity,
                finalTotal,
                serviceCharge,
                subTotalPrice,
                itemList,
                "COMPLETED",
                timeStamp,
                ""
            )
            Firestore().addReport(this, report)

            binding.paymentButton.visibility = View.GONE
            dialog.dismiss()
        }

        cashPayment.setOnClickListener {
            ePayment.visibility = View.INVISIBLE
            cashPayment.visibility = View.INVISIBLE
            divider.visibility = View.INVISIBLE
            cashInputLayout.visibility = View.VISIBLE
            cashButton.visibility = View.VISIBLE

            cashButton.setOnClickListener {
                val cashString = cashEditText.text.toString()

                if (cashString.isNotEmpty()) {
                    val cash = cashString.toDouble()
                    val total = orderItemViewModel.finalTotal.value?.toDouble()

                    if (cash >= total!!) {
                        val change = cash - total
                        changeTextView.text = "Change: RM" + String.format("%.2f", change)
                        changeTextView.visibility = View.VISIBLE
                        cashButton.visibility = View.GONE

                        binding.paymentButton.visibility = View.GONE

                        Firestore().updateCustomerPaymentMethod(this, tableId, Constants.CASHPAYMENT)

                        val report = Report (
                            id,
                            tableId,
                            tableNumber,
                            unixTimestampSeconds.toString(),
                            formattedDate,
                            totalQuantity,
                            finalTotal,
                            serviceCharge,
                            subTotalPrice,
                            itemList,
                            "COMPLETED",
                            timeStamp,
                            ""
                        )
                        Firestore().addReport(this, report)

                    } else {
                        Toast.makeText(requireContext(), "The cash was not enough", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Please enter the value", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    private fun openCancelDialog() {
        val inflater = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_cancel_reason, null)

        val cancelEditText: EditText = inflater.findViewById(R.id.cancel_editText)

        MaterialAlertDialogBuilder(requireContext())
            .setView(inflater)
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("OK") { dialog, _ ->
                val cancelReason = cancelEditText.text.toString()

                if (cancelReason.isNotEmpty()) {
                    val tableId = tableViewModel.tableId.value.toString()
                    dialog.dismiss()
                    Firestore().updateTableCancelReason(this, tableId, cancelReason)

                    val id = UUID.randomUUID().toString()
                    val tableNumber = tableViewModel.tableNumber.value.toString()
                    val currentTimestamp = Timestamp(System.currentTimeMillis())

                    // Using the toInstant() method to get an Instant and then extracting seconds
                    val unixTimestampSeconds = currentTimestamp.toInstant().epochSecond
                    println("Current Timestamp: $unixTimestampSeconds")

                    // Generating current date
                    val currentDate: LocalDateTime = LocalDateTime.now()
                    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss")
                    val formattedDate: String = currentDate.format(formatter)
                    println("Current Date: $formattedDate")

                    val timeStamp = com.google.firebase.Timestamp.now()

                    val report = Report (
                        id,
                        tableId,
                        tableNumber,
                        unixTimestampSeconds.toString(),
                        formattedDate,
                        "0",
                        "0",
                        "0",
                        "0",
                        null,
                        "CANCELED",
                        timeStamp,
                        cancelReason
                    )
                    Firestore().addReport(this, report)
                } else {
                    Toast.makeText(requireContext(), "Please enter the reason", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun openDialogQrCode() {
        val inflater = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_generate_qr_code, null)

        val startTextView: TextView = inflater.findViewById(R.id.start_view)
        startTextView.isVisible = false
        val qrImage: ImageView = inflater.findViewById(R.id.qr_image)
        val qrGenerateButton: Button = inflater.findViewById(R.id.generate_qr_code_button)

        qrGenerateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            val sdf = SimpleDateFormat("dd/MM/YYYY HH:mm:ss", Locale.getDefault())
            val startTime = sdf.format(calendar.time)

            val baseUrl = "foodorderingsystem-a59e8.firebaseapp.com"
            val tableNumber = tableViewModel.tableNumber.value.toString()
            val uniqueToken = UUID.randomUUID().toString()


            // Generate the QR code with the modified URL
            val qrCode = generateQRCode("$baseUrl?tableNumber=$tableNumber&token=$uniqueToken")

            // Display the QR code in an ImageView
            qrImage.setImageBitmap(qrCode)

            val formattedTableNumber = "Table: ${tableViewModel.tableNumber.value.toString()}"
            val formattedStartTime = "Start Date: $startTime"
            startTextView.text = formattedStartTime
            startTextView.isVisible = true

            // Print the QR code, start date, and expire date
            print(qrCode!!, formattedStartTime, formattedTableNumber, uniqueToken, startTime)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(inflater)
            .show()
    }

    private fun checkPrintReceipt() {
        if (checkBluetoothConnectionStatus()) {
            // Bluetooth is connected, introduce a delay before printing
            printReceipt()
        } else {
            // Bluetooth is not connected, handle
            Toast.makeText(requireContext(), "Bluetooth is not connected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun printReceipt() {
        try {
            val tableFragment = this // Assuming you're calling this from a Fragment
            var tableNumber: String? = null
            var id: String? = null
            var tempOrderedItems: List<OrderItem>? = emptyList()
            var orderReceipt: String? = null
            val tableId = tableViewModel.tableId.value.toString()

            // Call the function to retrieve customer ordering data
            Firestore().getCustomerOrderForReceipt(tableFragment, tableId) { orderItems ->
                // Check if the orderItems is not null
                if (orderItems != null) {
                    for (orderItem in orderItems) {
                        id = orderItem.id
                        tableNumber = orderItem.tableNumber
                        tempOrderedItems = orderItem.customerOrder?.toList()
                        Log.d("print receipt", tempOrderedItems.toString())

                    }

                    if (tempOrderedItems != null && tableNumber != null) {
                        orderReceipt = generateReceipt(tempOrderedItems, tableNumber)
                        Log.d("get order receipt", orderReceipt.toString())
                    } else {
                        Log.d("printOrder", "customerOrdering or tableNumber is null")
                    }

                    if (orderReceipt != null) {
                        // Inside your Fragment/Activity
                        lifecycleScope.launch(Dispatchers.IO) {
                            intentPrint(orderReceipt!!)
                        }
                    }

                } else {
                    Log.d("printOrder", "Order items are null")
                }
            }
        } catch (ex: java.lang.Exception) {
            value += "$ex\nExcep IntentPrint \n"
            Toast.makeText(requireContext(), value, Toast.LENGTH_LONG).show()
            Log.d("error printing", ex.toString())
        }
    }

    private fun generateReceipt(orderItem: List<OrderItem>?, tableNumber: String?): String {
        val maxLength = 32 // Adjust based on your printer's line width
        val separatorLine = "-".repeat(maxLength)

        val header = "Table: $tableNumber"
        val emptyLine = "\r\n"
        // Item name and quantity header
        val itemDetails = buildString {
            append(separatorLine)
            append(emptyLine)

            // Item name, quantity, and amount header
            append("Item Name${" ".repeat(5)}Quantity${" ".repeat(4)}Amount$emptyLine")

            var subTotal = 0.0
            var totalQuantity = 0

            for (item in orderItem.orEmpty()) {
                // Item name on the left, quantity in the middle, amount on the right
                val itemName = item.itemName
                val quantity = item.quantity.toString()
                val amount = item.totalPrice!!
                subTotal += amount
                totalQuantity += item.quantity!!

                // Item name, quantity, and amount in a single line
                val formattedLine = "%-${maxLength - 18}s %8s %8s".format(itemName, quantity, String.format("%.2f", amount))
                append("$formattedLine$emptyLine")

                // Remark and take away information
                if (item.remarks?.isNotEmpty() == true) {
                    append("Remark: ${item.remarks}$emptyLine")
                }
                if (item.takeaway == true) {
                    append("Take Away$emptyLine")
                }

                append(emptyLine)
            }

            // Separator line between items and total calculation
            append(emptyLine)

            // Separator line between items
            append(separatorLine)

            // Display total quantity and sub-total
            val totalQuantityLine = "Total Quantity: %-${maxLength - 19}s %s$emptyLine"
            append(totalQuantityLine.format(" ", totalQuantity))

            val subTotalLine = "Sub-Total: %-${maxLength - 18}s %.2f$emptyLine"
            append(subTotalLine.format(" ", subTotal))

            // Calculate service charge, round up, and final total
            val serviceChargePercentage = 0.10 // 10%
            val serviceCharge = subTotal * serviceChargePercentage
            val beforeRoundup = subTotal + serviceCharge
            val roundup = roundup(subTotal, serviceChargePercentage) - beforeRoundup
            val finalTotal = beforeRoundup + roundup

            // Display service charge, round up, and final total
            val serviceChargeLine = "Service Charge 10%%: %${maxLength - 21}.2f$emptyLine"
            val roundupLine = "Round Up: %${maxLength - 11}.2f$emptyLine"
            val finalTotalLine = "Final Total: %${maxLength - 14}.2f$emptyLine"

            append(serviceChargeLine.format(serviceCharge))
            append(roundupLine.format(roundup))
            append(finalTotalLine.format(finalTotal))

            append(emptyLine)
            append(emptyLine)
            append(separatorLine)
        }

        return "$header$emptyLine$itemDetails"
    }

    private fun roundup(subTotal: Double, serviceChargePercentage: Double): Double {
        val serviceCharge = subTotal * serviceChargePercentage
        val beforeRoundup = subTotal + serviceCharge

        val decimalValue = (beforeRoundup * 100).toInt() % 10

        val roundedValue = if (decimalValue > 5) {
            ceil(beforeRoundup)
        } else {
            beforeRoundup
        }

        return roundedValue
    }

    private fun print(
        qrCode: Bitmap,
        formattedStartTime: String,
        formattedTableNumber: String,
        uniqueToken: String,
        startTime: String
    ) {
        val handler = Handler(Looper.getMainLooper())

        if (checkBluetoothConnectionStatus()) {
            // Bluetooth is connected, introduce a delay before printing
            handler.postDelayed({

                val tableId = tableViewModel.tableId.value.toString()
                Firestore().updateTableOrder(this, tableId, startTime, uniqueToken)
                // Print the QR code

            }, 1000) // Introduce a 1-second delay (adjust as needed)
            sendPrintData(qrCode, formattedStartTime, formattedTableNumber)
        } else {
            // Bluetooth is not connected, handle
            Toast.makeText(requireContext(), "Bluetooth is not connected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendPrintData(
        qrCode: Bitmap,
        startTime: String,
        formattedTableNumber: String
    ) {
        printPic.init(qrCode)
        val qrCodeData = printPic.printDraw()

        val maxLength = 32 // Adjust based on your printer's line width
        val emptyLinesBelowTitle = 2 // Adjust based on your preference

        // Generate empty lines below the title
        val emptyLines = "\r\n".repeat(emptyLinesBelowTitle)

        val paddingText1 = maxOf(0, (maxLength - startTime.length) / 2)
        val centerStartText = " ".repeat(paddingText1) + startTime

        // Create a ByteArray for the text data
        val tableData = "${emptyLines}${formattedTableNumber}\n\n".toByteArray(Charset.forName("UTF-8"))
        val timeData = "\n\n${centerStartText}${emptyLines}${emptyLines}\n\n\n".toByteArray(Charset.forName("UTF-8"))

        Log.d("QR Code Data", qrCodeData.contentToString())
        Log.d("Text Data", timeData.contentToString())

        // Check if your Bluetooth connection is still valid
        if (checkBluetoothConnectionStatus()) {
            // Send the print data to the printer
            ConnectionBluetoothManager.printQRDataForCustomer(qrCodeData, timeData, tableData)
        } else {
            // Handle the case where the Bluetooth connection is lost
            Toast.makeText(requireContext(), "Bluetooth connection lost", Toast.LENGTH_SHORT).show()
        }
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

    private fun intentPrint(textValue: String) {
        var prName = ""
        prName = ConnectionBluetoothManager.getPrinterName().toString()
        if (prName.isNotEmpty()) {
            val buffer = textValue.toByteArray()
            val printHeader = byteArrayOf(0xAA.toByte(), 0x55, 2, 0)
            printHeader[3] = buffer.size.toByte()
            beginListenForData()
            if (printHeader.size > 128) {
                value += "\nValue is more than 128 size\n"
                Toast.makeText(requireContext(), value, Toast.LENGTH_LONG).show()
            } else {
                try {
                    if (socket != null && socket!!.isConnected) {
                        Log.d("Print", "Entering intentPrint")
                        synchronized(outputStreamLock) {
                            Log.d("Print", "Inside synchronized block")
                            try {
                                val sp = byteArrayOf(0x1B, 0x40)
                                outputStream!!.write(sp)
                                outputStream!!.write(textValue.toByteArray())
                                val feedPaperCut = byteArrayOf(0x10, 0x56, 66, 0x00)
                                outputStream!!.write(feedPaperCut)
                                outputStream!!.flush()
                            } catch (e: IOException) {
                                // Handle exceptions
                                e.printStackTrace()
                            }
                        }
                        Log.d("Print", "Exiting intentPrint")
                    } else {
                        // Handle case where the socket is not connected
                        Log.e("Bluetooth", "Socket is not connected")
                    }
                } catch (ex: java.lang.Exception) {
                    Log.e("Bluetooth Error Printing", "Error during printing", ex)
                    Toast.makeText(requireContext(), "Please select printer again", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Log.d("printer name in class", ConnectionBluetoothManager.getPrinterName().toString())
        }
    }

    // Function to check Bluetooth connection status
    private fun checkBluetoothConnectionStatus(): Boolean {

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            // Bluetooth is not enabled
            Log.d("Bluetooth", "Bluetooth is not enabled")
            return false
        } else {
            // Bluetooth is enabled, check if a device is connected
            if (ConnectionBluetoothManager.getBluetoothSocket() != null && ConnectionBluetoothManager.getBluetoothSocket()!!.isConnected) {
                // Bluetooth connection is active
                Log.d("Bluetooth", "Bluetooth is connected")
                return true
            } else {
                // Bluetooth connection is not active
                Log.d("Bluetooth", "Bluetooth is not connected")
                return false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToBluetoothDevice() {
        bluetoothAdapter = ConnectionBluetoothManager.getBluetoothAdapter()
        bluetoothDevice = ConnectionBluetoothManager.getBluetoothDevice()

        try {
            if (bluetoothAdapter == null) {
                Log.d("bluetoothAdapter", "Null")
                return
            }

            if (!bluetoothAdapter!!.isEnabled) {
                val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                btActivityResultLauncher.launch(enableBluetooth)
                return
            }

            if (bluetoothDevice == null) {
                Log.d("bluetoothDevice", "Null")
                return
            }

            val m = bluetoothDevice!!.javaClass.getMethod(
                "createRfcommSocket", *arrayOf<Class<*>?>(
                    Int::class.javaPrimitiveType
                )
            )
            socket = m.invoke(bluetoothDevice, 1) as BluetoothSocket
            bluetoothAdapter?.cancelDiscovery()
            socket!!.connect()
            ConnectionBluetoothManager.setBluetoothSocket(socket)
            outputStream = socket!!.outputStream
            inputStream = socket!!.inputStream
            ConnectionBluetoothManager.setOutputStream(outputStream)
            ConnectionBluetoothManager.setInputStream(inputStream)
            beginListenForData()

        } catch (ex: java.lang.Exception) {
            val value = "Bluetooth Printer Is Not Connected"
            Toast.makeText(requireContext(), value, Toast.LENGTH_SHORT).show()
//            socket = null
            ConnectionBluetoothManager.setBluetoothSocket(null)
            ex.printStackTrace() // Log the exception for debugging
        }
    }

    private fun beginListenForData() {
        Log.d("inside beginListenData function","before running try catch")
        try {
            val handler = Handler()
            val delimiter: Byte = 10
            stopWorker = false
            readBufferPosition = 0
            val DELAY_TIME = 100 // milliseconds

            readBuffer = ByteArray(1024)
            workerThread = Thread{
                while (!Thread.currentThread().isInterrupted && !stopWorker) {
                    try {
                        //Log.d("beginListenForData", "Before reading inputStream")
                        val bytesAvailable = inputStream!!.available()
                        if (bytesAvailable > 0) {
                            Log.d("run", "if bigger than 0")
                            val packetBytes = ByteArray(bytesAvailable)
                            inputStream!!.read(packetBytes)
                            for (i in 0 until bytesAvailable) {
                                val b = packetBytes[i]
                                if (b == delimiter) {
                                    val encodedBytes = ByteArray(readBufferPosition)
                                    System.arraycopy(
                                        readBuffer, 0,
                                        encodedBytes, 0,
                                        encodedBytes.size
                                    )

                                    val data = String(encodedBytes, Charset.forName("US-ASCII"))
                                    readBufferPosition = 0

                                    // tell the user data were sent to bluetooth printer device
                                    handler.post{ Log.d("run thread inside the beginListenData", data)}

                                    Handler(Looper.getMainLooper()).postDelayed({
                                        data
                                        Log.d("check handler", "running after delay 1 second")
                                    }, 1000) // 1000 milliseconds delay

                                    Log.d("run thread inside the beginListenData", data)
                                } else {
                                    readBuffer[readBufferPosition++] = b
                                }
                            }
                        } //else {
                        // No data available, sleep for a short duration
//                            Thread.sleep(DELAY_TIME.toLong())
//                        }
                    } catch (ex: IOException) {
                        stopWorker = true
                        Log.d("get error from begin listen data", ex.toString())
                        ex.printStackTrace() // Log the exception
                    }
                }
            }
            workerThread!!.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun backToPrevious() {
        Utils().backToPrevious(this, R.id.navigation_table)
    }

    override fun onResume() {
        super.onResume()

        // Check Bluetooth connection status
        if (!ConnectionBluetoothManager.isBluetoothConnected()) {
            // Reconnect if necessary
            if (socket == null || !socket!!.isConnected) {
                connectToBluetoothDevice()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Disconnect Bluetooth when leaving the fragment
        ConnectionBluetoothManager.disconnectBluetooth()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        orderItemViewModel.resetValue()
    }
}