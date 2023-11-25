package com.example.foodOrderingSystem.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.databinding.FragmentPrinterBinding
import com.example.foodOrderingSystem.utils.ConnectionBluetoothManager
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID


class PrinterFragment : Fragment() {

    private var _binding: FragmentPrinterBinding? = null
    private val binding get() = _binding!!
    private lateinit var dialog: Dialog
    private lateinit var mProgressDialog: Dialog
    private var printerName = ConnectionBluetoothManager.getPrinterName()
    private var btPermission = false
    private var bluetoothManager = ConnectionBluetoothManager.getBluetoothManager()
    private var bluetoothAdapter = ConnectionBluetoothManager.getBluetoothAdapter()
    private var socket = ConnectionBluetoothManager.getBluetoothSocket()
    private var bluetoothDevice = ConnectionBluetoothManager.getBluetoothDevice()
    private var outputStream = ConnectionBluetoothManager.getOutputSteam()
    private var inputStream = ConnectionBluetoothManager.getInputSteam()
    private var workerThread = ConnectionBluetoothManager.getWorkerThread()
    private lateinit var readBuffer: ByteArray
    private var readBufferPosition = 0

    @Volatile
    var stopWorker = false
    private var value = ""
    private val outputStreamLock = Any()

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter
            btPermission = true

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
        if (result.resultCode == RESULT_OK) {
            btScan()
        }

    }

    private fun scanBt() {
        checkPermission()
    }

    private fun print() {
        val handler = Handler(Looper.getMainLooper())

        if (checkBluetoothConnectionStatus()) {
            // Bluetooth is connected, introduce a delay before printing
            printTest()
        } else {
            // Bluetooth is not connected, handle
            Toast.makeText(requireContext(), "Bluetooth is not connected", Toast.LENGTH_SHORT).show()
            //checkPermission()
        }
//        if (btPermission) {
//            printTest()
//        } else {
//            checkPermission()
//        }
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
            ADAhere = SimpleAdapter(requireContext(), data,
                R.layout.list_bluetooth_item, fromWhere, viewsWhere)
            btlst.adapter = ADAhere
            ADAhere.notifyDataSetChanged()

            btlst.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, position, l ->
                val string = ADAhere.getItem(position) as HashMap<String, String>
                val prnName = string["A"]
                binding.printerEditText.setText(prnName)
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
                        //beginListenForData()
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
// Define a boolean flag to control the thread
                                    var isConnectionThreadRunning = true
                                    showProgress()

                                    Thread {
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            if (isConnectionThreadRunning) {
                                                // Introduce a delay before connecting
                                                try {
                                                    socket = m.invoke(bluetoothDevice, 1) as BluetoothSocket
                                                    bluetoothAdapter?.cancelDiscovery()
                                                    socket!!.connect()
                                                    Log.d("socket", "connected")
                                                    ConnectionBluetoothManager.setBluetoothSocket(socket)
                                                    outputStream = socket!!.outputStream
                                                    inputStream = socket!!.inputStream
                                                    ConnectionBluetoothManager.setOutputStream(outputStream)
                                                    ConnectionBluetoothManager.setInputStream(inputStream)
                                                    beginListenForData()

                                                    // Your UI update code
                                                    Toast.makeText(requireContext(), "Successfully connected", Toast.LENGTH_SHORT).show()
                                                } catch (e: IOException) {
                                                    e.printStackTrace()
                                                    // Handle connection error
                                                } finally {
                                                    closeProgress()
                                                }
                                            }
                                        }, 3000) // 3000 milliseconds delay
                                    }.start()

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
    private fun connectToBluetoothDevice(selectedDevice: String?) {
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPrinterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Bluetooth when the fragment is created
        ConnectionBluetoothManager.getInstance()

        binding.apply {
            topAppBar.setNavigationOnClickListener { backToPrevious() }
            selectPrinterButton.setOnClickListener { scanBt() }
            printButton.setOnClickListener { print() }
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

    private fun printTest() {
        try {
            val restaurantName = "Food Ordering System"
            val dummyAddress = "1234 Example Street,\nCity, Country"

            val maxLength = 32 // Adjust based on your printer's line width
            val emptyLinesBelowTitle = 2 // Adjust based on your preference

            // Center-align the restaurant name
            val padding = maxOf(0, (maxLength - restaurantName.length) / 2)
            val centeredRestaurantName = " ".repeat(padding) + restaurantName

            // Create a separator line
            val separatorLine = "-".repeat(maxLength)

            // Center-align the dummy address
            // Center-align each line of the dummy address
            val centeredDummyAddress = dummyAddress.split("\n").joinToString("\n") { line ->
                val addressPadding = maxOf(0, (maxLength - line.length) / 2)
                " ".repeat(addressPadding) + line
            }

            // Generate empty lines below the title
            val emptyLines = "\r\n".repeat(emptyLinesBelowTitle)

            // Combine the elements for printing
            val printText = "$centeredRestaurantName$emptyLines\n$centeredDummyAddress$emptyLines\n$separatorLine"


//            // Inside your Fragment/Activity
            lifecycleScope.launch(Dispatchers.IO) {
                intentPrint(printText)
            }
            //intentPrint(BILL)
//            val testMessage = "Hello, this is a test print."
//            intentPrint(testMessage)

        } catch (ex: java.lang.Exception) {
            value += "$ex\nExcep IntentPrint \n"
            Toast.makeText(requireContext(), value, Toast.LENGTH_LONG).show()
            Log.d("error printing", ex.toString())
        }
    }


    fun intentPrint(textValue: String) {
        var prName = ""
        prName = ConnectionBluetoothManager.getPrinterName().toString()
        if (prName.isNotEmpty()) {
            val buffer = textValue.toByteArray()
            val printHeader = byteArrayOf(0xAA.toByte(), 0x55, 2, 0)
            printHeader[3] = buffer.size.toByte()
            //initPrinter()
            beginListenForData()
            Log.d("print header", printHeader[3].toString())
           if (printHeader.size > 128) {
               value += "\nValue is more than 128 size\n"
               Toast.makeText(requireContext(), value, Toast.LENGTH_LONG).show()
           } else {
               Log.d("check socket in intentPrint", socket?.inputStream.toString() + ", " + socket?.outputStream.toString())
               Log.d("check socket in intentPrint", socket!!.isConnected.toString() + ", " +socket.toString())
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

    private fun backToPrevious() {
        Utils().backToPrevious(this, R.id.navigation_settings)
    }

    override fun onResume() {
        super.onResume()

        // Check Bluetooth connection status
        if (!ConnectionBluetoothManager.isBluetoothConnected()) {
            // Reconnect if necessary
            val selectedDevice = ConnectionBluetoothManager.getPrinterName()
            binding.printerEditText.setText(selectedDevice)
            Log.d("get reconnect device name", selectedDevice.toString())
            Log.d("check bluetooth adapter onResume", ConnectionBluetoothManager.getBluetoothAdapter()?.isEnabled.toString())
            Log.d("check adapter is exist onResume", ConnectionBluetoothManager.getBluetoothAdapter().toString())
            Log.d("check socket onResume", ConnectionBluetoothManager.getBluetoothSocket()?.inputStream.toString())

            if (socket == null || !socket!!.isConnected) {
                connectToBluetoothDevice(selectedDevice)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Disconnect Bluetooth when leaving the fragment
        ConnectionBluetoothManager.disconnectBluetooth()
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

}