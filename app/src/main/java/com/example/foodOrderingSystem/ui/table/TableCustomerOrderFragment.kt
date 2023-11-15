package com.example.foodOrderingSystem.ui.table

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.foodOrderingSystem.ConnectionBluetoothManager
import com.example.foodOrderingSystem.ConnectionClass
import com.example.foodOrderingSystem.PrintPic
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.databinding.FragmentTableCustomerOrderBinding
import com.example.foodOrderingSystem.models.TableViewModel
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.EnumMap
import java.util.Locale
import java.util.UUID

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
    private val tableViewModel: TableViewModel by activityViewModels()

    @Volatile
    var stopWorker = false
    private var value = ""
    private var connectionClass: ConnectionClass = ConnectionClass()
    private val outputStreamLock = Any()

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter

            if (bluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                btActivityResultLauncher.launch(enableBtIntent)
            } else {
                btScan()
            }
        }

    }

    private val btActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            btScan()
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


        binding.apply{
            topAppBar.setNavigationOnClickListener { backToPrevious() }

            val navigationView: NavigationView = binding.tableNavView
            val headerView: View = navigationView.getHeaderView(0)
            val tableNumberTextView: TextView = headerView.findViewById(R.id.table_header_number)
            tableNumberTextView.text = tableViewModel.tableNumber.value.toString()

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

    private fun print(
        qrCode: Bitmap,
        startTime: String,
        expirationTime: String,
        formattedTableNumber: String
    ) {
        val handler = Handler(Looper.getMainLooper())

        if (checkBluetoothConnectionStatus()) {
            // Bluetooth is connected, introduce a delay before printing
            handler.postDelayed({
                // Print the QR code
                sendPrintData(qrCode, startTime, expirationTime, formattedTableNumber)
            }, 1000) // Introduce a 1-second delay (adjust as needed)
        } else {
            // Bluetooth is not connected, handle
            Toast.makeText(requireContext(), "Bluetooth is not connected", Toast.LENGTH_SHORT).show()
            checkPermission()
        }
    }

    private fun sendPrintData(
        qrCode: Bitmap,
        startTime: String,
        expirationTime: String,
        formattedTableNumber: String
    ) {
        // Convert the Bitmap to a byte array
//        val qrCodeData = convertBitmapToByteArray(qrCode)

        printPic.init(qrCode)
        val qrCodeData = printPic.printDraw()

        val maxLength = 32 // Adjust based on your printer's line width
        val emptyLinesBelowTitle = 2 // Adjust based on your preference

        // Generate empty lines below the title
        val emptyLines = "\r\n".repeat(emptyLinesBelowTitle)

        val paddingText1 = maxOf(0, (maxLength - startTime.length) / 2)
        val centerStartText = " ".repeat(paddingText1) + startTime

        val paddingText2 = maxOf(0, (maxLength - expirationTime.length) / 2)
        val centerExpireText = " ".repeat(paddingText2) + expirationTime

        // Create a ByteArray for the text data
        val tableData = "${emptyLines}${formattedTableNumber}\n\n".toByteArray(Charset.forName("UTF-8"))
        val timeData = "\n\n${centerStartText}${emptyLines}${centerExpireText}${emptyLines}\n\n\n".toByteArray(Charset.forName("UTF-8"))

        Log.d("QR Code Data", qrCodeData.contentToString())
        Log.d("Text Data", timeData.contentToString())

        // Combine the QR code data and text data
//        val printData = ByteArray( qrCodeData.size + timeData.size)
//        System.arraycopy(qrCodeData, 0, printData, 0, qrCodeData.size)
//        System.arraycopy(timeData, 0, printData, qrCodeData.size, timeData.size)


        // Check if your Bluetooth connection is still valid
        if (checkBluetoothConnectionStatus()) {
            // Send the print data to the printer
            ConnectionBluetoothManager.printQRDataForCustomer(qrCodeData, timeData, tableData)
        } else {
            // Handle the case where the Bluetooth connection is lost
            Toast.makeText(requireContext(), "Bluetooth connection lost", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun openDialogQrCode() {
        val inflater = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_generate_qr_code, null)

        val startTextView: TextView = inflater.findViewById(R.id.start_view)
        startTextView.isVisible = false
        val expireTextView: TextView = inflater.findViewById(R.id.expire_view)
        expireTextView.isVisible = false
        val qrImage: ImageView = inflater.findViewById(R.id.qr_image)
        val hourEditText: EditText = inflater.findViewById(R.id.qr_code_expire_hour_edit_text)
        val minuteEditText: EditText = inflater.findViewById(R.id.qr_code_expire_minute_edit_text)
        val qrGenerateButton: Button = inflater.findViewById(R.id.generate_qr_code_button)

        qrGenerateButton.setOnClickListener {
            val baseUrl = "http://192.168.0.3:8080"
            val tableNumber = tableViewModel.tableNumber.value.toString()
            var hourString = hourEditText.text.toString()
            var minuteString = minuteEditText.text.toString()

            if (hourString.isNotEmpty() && minuteString.isNotEmpty()) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = System.currentTimeMillis()
                val sdf = SimpleDateFormat("dd/MM/YYYY HH:mm:ss", Locale.getDefault())
                val startTime = sdf.format(calendar.time)

                val expirationTime = calculateExpirationTime(hourString.toInt(), minuteString.toInt())

                // Generate the QR code with the modified URL
                val qrCode = generateQRCode("$baseUrl?tableNumber=$tableNumber")

                // Display the QR code in an ImageView
                qrImage.setImageBitmap(qrCode)


                val formattedTableNumber = "Table: ${tableViewModel.tableNumber.value.toString()}"
                val formattedStartTime = "Start Date: $startTime"
                val formattedExpireTime = "Expire Date: $expirationTime"
                startTextView.text = formattedStartTime
                startTextView.isVisible = true
                expireTextView.text = formattedExpireTime
                expireTextView.isVisible = true

                val hourTextView: TextInputLayout  = inflater.findViewById(R.id.qr_code_expire_hour_field)
                val minuteTextView: TextInputLayout = inflater.findViewById(R.id.qr_code_expire_minute_field)
                hourTextView.isVisible = false
                minuteTextView.isVisible = false

                // Print the QR code, start date, and expire date
                print(qrCode!!, formattedStartTime, formattedExpireTime, formattedTableNumber)
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

    private fun checkPermission() {
        bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

//        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun btScan() {

        bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        ConnectionBluetoothManager.setBluetoothAdapter(bluetoothAdapter)

        Log.d("check bluetooth adapter", bluetoothAdapter!!.isEnabled.toString())
        Log.d("check socket is connected", socket?.isConnected.toString())
        Log.d("check socket is null or exist", socket.toString())

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.scan_bt, null)

        val btlst = dialogView.findViewById<ListView>(R.id.bt_list)
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter?.bondedDevices as Set<BluetoothDevice>
        val ADAhere: SimpleAdapter
        var data: MutableList<Map<String?, Any?>?>? = null
        data = ArrayList()

        if (pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                val datanum: MutableMap<String?, Any?> = HashMap()
                datanum["A"] = device.name
                datanum["B"] = device.address
                data.add(datanum)
            }
            val fromWhere = arrayOf("A")
            val viewsWhere = intArrayOf(R.id.item_name)
            ADAhere = SimpleAdapter(requireContext(), data, R.layout.list_bluetooth_item, fromWhere, viewsWhere)
            btlst.adapter = ADAhere
            ADAhere.notifyDataSetChanged()

            btlst.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, position, l ->
                val string = ADAhere.getItem(position) as HashMap<String, String>
                val prnName = string["A"]
                ConnectionBluetoothManager.setPrinterName(prnName.toString())

                try {
                    if (bluetoothAdapter != null) {
                        if (!bluetoothAdapter!!.isEnabled) {
                            val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            btActivityResultLauncher.launch(enableBluetooth)
                        }
                    } else {
                        Log.d("bluetoothAdapter", "Null")
                    }

                    Log.d("check socket status", socket.toString() +", "+ socket?.isConnected.toString())
                    if (socket != null && socket!!.isConnected) {
                        // Reuse the existing connected socket
                        outputStream = socket!!.outputStream
                        inputStream = socket!!.inputStream
                        beginListenForData()
                        return@OnItemClickListener
                    }

                    val pairedDevices = bluetoothAdapter?.bondedDevices

                    Log.d("get pair device", pairedDevices.toString())
                    if (pairedDevices != null) {
                        if (pairedDevices.isNotEmpty()) {
                            for (device in pairedDevices) {
                                if (device.name == ConnectionBluetoothManager.getPrinterName()) {
                                    bluetoothDevice = device
                                    ConnectionBluetoothManager.setPrinterName(device.name)
                                    ConnectionBluetoothManager.setBluetoothDevice(bluetoothDevice)
                                    Log.d("get device name", bluetoothDevice.toString())
                                    val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                                    val m = bluetoothDevice!!.javaClass.getMethod(
                                        "createRfcommSocket", *arrayOf<Class<*>?>(
                                            Int::class.javaPrimitiveType
                                        )
                                    )
                                    disconnectBluetoothSocket()

                                    // Show progress dialog before connecting
                                    //showProgress()

                                    Log.d("before run beginListenData", "Creating socket")
                                    socket = m.invoke(bluetoothDevice, 1) as BluetoothSocket
                                    Log.d("before run beginListenData", "Socket created, canceling discovery")
                                    bluetoothAdapter?.cancelDiscovery()
                                    Log.d("before run beginListenData", "Discovery canceled, connecting socket")
                                    socket!!.connect()
                                    Log.d("before run beginListenData", "Socket connected")
                                    ConnectionBluetoothManager.setBluetoothSocket(socket)
                                    outputStream = socket!!.outputStream
                                    inputStream = socket!!.inputStream
                                    ConnectionBluetoothManager.setOutputStream(outputStream)
                                    ConnectionBluetoothManager.setInputStream(inputStream)
                                    beginListenForData()

//                                    // Introduce a delay before connecting
//                                    Handler(Looper.getMainLooper()).postDelayed({
//
//
//                                        // Close progress dialog after connecting
//                                        closeProgress()
//                                    }, 2000) // 2000 milliseconds delay
                                    break
                                }
                            }
                        } else {
                            val value = "No Devices Found"
                            Toast.makeText(requireContext(), value, Toast.LENGTH_SHORT).show()
                            return@OnItemClickListener
                        }
                    }
                } catch (ex: java.lang.Exception) {
                    val value = "Bluetooth Printer Is Not Connected"
                    Toast.makeText(requireContext(), value, Toast.LENGTH_SHORT).show()
                    socket = null
                    ConnectionBluetoothManager.setBluetoothSocket(socket)
                } finally {
                    dialog.dismiss()
                }
            }
        } else {
            val value = "No Devices Found"
            Toast.makeText(requireContext(), value, Toast.LENGTH_SHORT).show()
        }

        dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .show()
    }

    // Disconnect Bluetooth socket
    private fun disconnectBluetoothSocket() {
        try {
            if (socket != null && socket!!.isConnected) {
                socket!!.close()
                outputStream?.close()
                inputStream?.close()
                // Note: outputStream is automatically closed when the socket is closed
            }
        } catch (e: IOException) {
            e.printStackTrace()
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

    fun showProgress() {
        mProgressDialog = Dialog(requireContext())

        mProgressDialog.setContentView(R.layout.dialog_progress)

        mProgressDialog.setCancelable(false)
        mProgressDialog.setCanceledOnTouchOutside(false)

        mProgressDialog.show()
    }

    fun closeProgress() {
        mProgressDialog.dismiss()
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
}