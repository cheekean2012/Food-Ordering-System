package com.example.foodOrderingSystem

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.foodOrderingSystem.databinding.FragmentPrinterBinding
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.foodOrderingSystem.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.UUID


class PrinterFragment : Fragment() {

    private var _binding: FragmentPrinterBinding? = null
    private val binding get() = _binding!!
    private lateinit var dialog: Dialog
    private var printerName = ""
    private var btPermission = false
    var bluetoothAdapter: BluetoothAdapter? = null
    private var socket: BluetoothSocket? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var workerThread: Thread? = null
    private lateinit var readBuffer: ByteArray
    private var readBufferPosition = 0

    @Volatile
    var stopWorker = false
    private var value = ""
    private var connectionClass:ConnectionClass = ConnectionClass()

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val bluetoothManager: BluetoothManager? = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
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

//        if (checkBluetoothConnectionStatus()) {
//            // Bluetooth is connected, introduce a delay before printing
//            handler.postDelayed({
//                printTest()
//            }, 1000) // 1000 milliseconds (1 second) delay
//        } else {
//            // Bluetooth is not connected, handle
//            Toast.makeText(requireContext(), "Bluetooth is not connected", Toast.LENGTH_SHORT).show()
//        }
        if (btPermission) {
            printTest()
        } else {
            checkPermission()
        }
    }

    // Function to check Bluetooth connection status
    private fun checkBluetoothConnectionStatus(): Boolean {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            // Bluetooth is not enabled
            Log.d("Bluetooth", "Bluetooth is not enabled")
            return false
        } else {
            // Bluetooth is enabled, check if a device is connected
            if (socket != null && socket!!.isConnected) {
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
        val bluetoothManager: BluetoothManager? = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

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
        val bluetoothManager: BluetoothManager? = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.scan_bt, null)

        dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .show()


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

            btlst.onItemClickListener = AdapterView.OnItemClickListener{ adapterView, view, position, l ->
                val string = ADAhere.getItem(position) as HashMap<String, String>
                val prnName = string["A"]
                binding.printerEditText.setText(prnName)
                connectionClass.printerName = prnName.toString()
                dialog.dismiss()
                val selectedDevice = pairedDevices.elementAt(position)
                Log.d("selectedDevice", selectedDevice.name.toString())
//                connectToBluetoothDevice(selectedDevice)
            }
        } else {
            val value = "No Devices Found"
            Toast.makeText(requireContext(), value, Toast.LENGTH_SHORT).show()
            return
        }

    }

    @SuppressLint("MissingPermission")
    private fun connectToBluetoothDevice(selectedDevice: BluetoothDevice) {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        try {
            if (bluetoothAdapter != null) {
                if (!bluetoothAdapter.isEnabled) {
                    val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    btActivityResultLauncher.launch(enableBluetooth)
                }
            } else {
                Log.d("bluetoothAdapter", "Null")
            }
            val pairedDevices = bluetoothAdapter?.bondedDevices
            Log.d("get pair device", pairedDevices.toString())
            if (pairedDevices != null) {
                if (pairedDevices.size > 0) {
                    if (pairedDevices != null) {
                        for (device in pairedDevices) {
                            if (device.name == selectedDevice.name.toString()) {
                                bluetoothDevice = device
                                ConnectionClass().printerName = device.name
                                Log.d("get device name", bluetoothDevice.toString())
                                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                                val m = bluetoothDevice!!.javaClass.getMethod(
                                    "createRfcommSocket", *arrayOf<Class<*>?> (
                                        Int::class.javaPrimitiveType
                                    )
                                )
                                socket = m.invoke(bluetoothDevice, 1) as BluetoothSocket
                                bluetoothAdapter?.cancelDiscovery()
                                socket!!.connect()
                                outputStream = socket!!.outputStream
                                inputStream = socket!!.inputStream
                                beginListenForData()
                                break
                            }
                        }
                    }
                } else {
                    val value = "No Devices Found"
                    Toast.makeText(requireContext(), value, Toast.LENGTH_SHORT).show()
                    return
                }
            }
            dialog.dismiss()
        } catch (ex: java.lang.Exception) {
            val value = "Bluetooth Printer Is Not Connected"
            Toast.makeText(requireContext(), value, Toast.LENGTH_SHORT).show()
            socket = null
        }

//        if (socket != null) {
//            socket!!.close()
//        }
//        if (inputStream != null) {
//            inputStream!!.close()
//        }
//        if (outputStream != null) {
//            outputStream!!.close()
//        }
//
//
//        try {
//            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
//            socket = m.invoke(device, 1) as BluetoothSocket
//            socket!!.connect()
//
//            // Now, you can use the 'socket' for communication
//            outputStream = socket!!.outputStream
//            inputStream = socket!!.inputStream
//
//            // Dismiss the dialog
//            dialog.dismiss()
//
//            // You may want to update UI or perform other actions here after successful connection
//        } catch (ex: Exception) {
//            // Handle connection failure
//            Toast.makeText(requireContext(), "Failed to connect to the Bluetooth device", Toast.LENGTH_SHORT).show()
//            Log.e("Bluetooth", "Error connecting to Bluetooth device", ex)
//        }
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

        binding.apply {
            topAppBar.setNavigationOnClickListener { backToPrevious() }
            selectPrinterButton.setOnClickListener { scanBt() }
            printButton.setOnClickListener { print() }
        }

    }

    private fun beginListenForData() {
        try {
            val handler = Handler()
            val delimiter: Byte = 10
            stopWorker = false
            readBufferPosition = 0
            readBuffer = ByteArray(1024)
            workerThread = Thread{
                while (!Thread.currentThread().isInterrupted && !stopWorker) {
                    try {
                        val bytesAvailable = inputStream!!.available()
                        if (bytesAvailable > 0) {
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
                                    handler.post{ Log.d("e", data)}
                                } else {
                                    readBuffer[readBufferPosition++] = b
                                }
                            }
                        }
                    } catch (ex: IOException) {
                        stopWorker = true
                    }
                }
            }
            workerThread!!.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun initPrinter() {
        printerName = connectionClass.printerName.toString()

        val bluetoothManager: BluetoothManager? = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
        try {
            if (bluetoothAdapter != null) {
                if (!bluetoothAdapter.isEnabled) {
                    val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    btActivityResultLauncher.launch(enableBluetooth)
                }
            }
            val pairedDevices = bluetoothAdapter?.bondedDevices
            if (pairedDevices != null) {
                if (pairedDevices.size > 0) {
                    if (pairedDevices != null) {
                        for (device in pairedDevices) {
                            if (device.name == printerName) {
                                bluetoothDevice = device
                                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                                val m = bluetoothDevice!!.javaClass.getMethod(
                                    "createRfcommSocket", *arrayOf<Class<*>?> (
                                        Int::class.javaPrimitiveType
                                    )
                                )
                                socket = m.invoke(bluetoothDevice, 1) as BluetoothSocket
                                bluetoothAdapter?.cancelDiscovery()
                                socket!!.connect()
                                outputStream = socket!!.outputStream
                                inputStream = socket!!.inputStream
                                beginListenForData()
                                break
                            }
                        }
                    }
                } else {
                    val value = "No Devices Found"
                    Toast.makeText(requireContext(), value, Toast.LENGTH_SHORT).show()
                    return
                }
            }
        } catch (ex: java.lang.Exception) {
            val value = "Bluetooth Printer Is Not Connected"
            Toast.makeText(requireContext(), value, Toast.LENGTH_SHORT).show()
            socket = null
        }
    }

    private fun printTest() {
        try {
//            var cmpName = "Print Test"
//            val textData = StringBuilder()
//
//            textData.append(
//                """$cmpName"""".trimIndent()
//            )
//
//            intentPrint(cmpName)

            val testMessage = "Hello, this is a test print."
            intentPrint(testMessage)

        } catch (ex: java.lang.Exception) {
            value += "$ex\nExcep IntentPrint \n"
            Toast.makeText(requireContext(), value, Toast.LENGTH_LONG).show()
        }
    }

    fun intentPrint(textValue: String) {
        var prName = ""
        prName = connectionClass.printerName.trim()
        if (prName.isNotEmpty()) {
            val buffer = textValue.toByteArray()
            val printHeader = byteArrayOf(0xAA.toByte(), 0x55, 2, 0)
            printHeader[3] = buffer.size.toByte()
            initPrinter()
           if (printHeader.size > 128) {
               value += "\nValue is more than 128 size\n"
               Toast.makeText(requireContext(), value, Toast.LENGTH_LONG).show()
           } else {
               try {
                   if (socket != null) {
                       try {
                           val sp = byteArrayOf(0x1B, 0x40)
                           outputStream!!.write(sp)
                           Thread.sleep(1000)
                       } catch (e: InterruptedException) {
                           e.printStackTrace()
                       }
                       outputStream!!.write(textValue.toByteArray())
                       val feedPaperCut = byteArrayOf(0x10, 0x56, 66, 0x00)
                       outputStream!!.write(feedPaperCut)
                       outputStream!!.flush()
                       outputStream!!.close()
                   }
               } catch (ex: java.lang.Exception) {
                   Log.e("Bluetooth", "Error during printing", ex)
                   Toast.makeText(requireContext(), ex.message.toString(), Toast.LENGTH_LONG).show()
               }
           }
        } else {
            Log.d("printer name in class", connectionClass.printerName)
        }
    }


    private fun backToPrevious() {
        Utils().backToPrevious(this, R.id.navigation_settings)
    }

}