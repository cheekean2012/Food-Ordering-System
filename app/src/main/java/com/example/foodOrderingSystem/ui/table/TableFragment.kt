package com.example.foodOrderingSystem.ui.table

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodOrderingSystem.utils.ConnectionBluetoothManager
import com.example.foodOrderingSystem.R
import com.example.foodOrderingSystem.adapters.TableListAdapter
import com.example.foodOrderingSystem.databinding.FragmentTableBinding
import com.example.foodOrderingSystem.firestore.Firestore
import com.example.foodOrderingSystem.models.OrderItem
import com.example.foodOrderingSystem.models.TableOrder
import com.example.foodOrderingSystem.models.TableViewModel
import com.example.foodOrderingSystem.models.Tables
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID

class TableFragment : Fragment() {

    private var _binding: FragmentTableBinding? = null
    private val tableViewModel: TableViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var tableList: LiveData<MutableList<Tables>>
    private val handler = Handler(Looper.getMainLooper())
    private var bluetoothManager = ConnectionBluetoothManager.getBluetoothManager()
    private var bluetoothAdapter = ConnectionBluetoothManager.getBluetoothAdapter()
    private var socket = ConnectionBluetoothManager.getBluetoothSocket()
    private var bluetoothDevice = ConnectionBluetoothManager.getBluetoothDevice()
    private var outputStream = ConnectionBluetoothManager.getOutputSteam()
    private var inputStream = ConnectionBluetoothManager.getInputSteam()
    private var workerThread = ConnectionBluetoothManager.getWorkerThread()
    private val binding get() = _binding!!
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

            if (bluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                btActivityResultLauncher.launch(enableBtIntent)
            } else {
//                btScan()
            }
        }

    }

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
    ): View {
        _binding = FragmentTableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController()
        navView.setupWithNavController(navController)

        navView.setOnItemSelectedListener  { item ->
            when (item.itemId) {
                R.id.navigation_table -> {
                    if (!item.isChecked) {
                        navController.navigate(R.id.navigation_table)
                    }
                    true
                }
                R.id.navigation_settings -> {
                    if (!item.isChecked) {
                        navController.navigate(R.id.navigation_settings)
                    }
                    true
                }
                else -> false
            }
        }

        // Schedule periodic refresh every 5 seconds
        handler.postDelayed(refreshRunnable, 5000)

        tableList = tableViewModel.tableList

        recyclerView = binding.tableRecycleView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe changes in the tableList LiveData
        tableList.observe(viewLifecycleOwner) {
            // Update your RecyclerView adapter when the LiveData changes
            recyclerView.adapter = TableListAdapter(this, requireContext(), tableList)
        }

        Firestore().getTableOrder(this, recyclerView, tableViewModel)

        binding.floatingActionButton.setOnClickListener { addTable() }
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            print()
            handler.postDelayed(this, 5000)
        }
    }

    private fun print() {
        if (checkBluetoothConnectionStatus()) {
            // Bluetooth is connected, introduce a delay before printing
            printOrder()
        } else {
            // Bluetooth is not connected, handle
            Toast.makeText(requireContext(), "Bluetooth is not connected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun printOrder() {
        try {
            val tableFragment = this // Assuming you're calling this from a Fragment
            var customerOrdering: MutableList<OrderItem>? = null
            var tableNumber: String? = null
            var id: String? = null
            var tempOrderingItems: List<OrderItem>? = emptyList()
            var tempOrderedItems: List<OrderItem>? = emptyList()
            var orderReceipt: String? = null

            // Call the function to retrieve customer ordering data
            Firestore().getCustomerOrdering(tableFragment) { orderItems ->
                // Check if the orderItems is not null
                if (orderItems != null) {
                    for (orderItem in orderItems) {
                        id = orderItem.id
                        customerOrdering = orderItem.customerOrdering
                        tableNumber = orderItem.tableNumber
                        tempOrderingItems = orderItem.customerOrdering?.toList()
                        tempOrderedItems = orderItem.customerOrder?.toList()

                        Log.d("CustomerOrdering", "$customerOrdering, TableNumber: $tableNumber")

//                        // If you have a list of OrderItem inside customerOrdering, you can also iterate through them
//                        customerOrdering?.forEach { orderItem ->
//                            val itemName = orderItem.itemName
//                            val quantity = orderItem.quantity
//
//                            Log.d("OrderItem", "Item: $itemName, Quantity: $quantity")
//                        }
                    }

                    if (customerOrdering != null && tableNumber != null) {
                        orderReceipt = generateOrderReceipt(tempOrderingItems, tableNumber)
                        // Rest of the code...
                    } else {
                        Log.d("printOrder", "customerOrdering or tableNumber is null")
                    }

                    if (orderReceipt != null) {
                        // Inside your Fragment/Activity
                        lifecycleScope.launch(Dispatchers.IO) {
                            intentPrint(orderReceipt!!)
                        }
                    }

                    if (customerOrdering != null) {
                        val combinedItems = (tempOrderedItems ?: emptyList()) + (tempOrderingItems ?: emptyList())
                        Firestore().updateCustomerOrderingToCustomerOrder(this, combinedItems, id)
                        customerOrdering = null
                        tempOrderingItems = emptyList()
                        tempOrderedItems = emptyList()
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


    private fun generateOrderReceipt(orderItem: List<OrderItem>?, tableNumber: String?): String {
        val maxLength = 32 // Adjust based on your printer's line width
        val separatorLine = "-".repeat(maxLength)

        val header = "Table: $tableNumber"
        val emptyLine = "\r\n"
        val orderDetails = buildString {
            append(separatorLine)
            append(emptyLine)

            // Item name and quantity header
            append("Item Name${" ".repeat(maxLength - 17)}Quantity$emptyLine")

            for (item in orderItem!!) {
                // Item name on the left, quantity on the right
                val itemName = item.itemName
                val quantity = item.quantity.toString()
                append("$itemName${" ".repeat(maxLength - itemName?.length!! - quantity.length)}$quantity$emptyLine")

                // Remark and take away information
                if (item.remarks?.isNotEmpty() == true) {
                    append("Remark: ${item.remarks}$emptyLine")
                }
                if (item.takeaway == true) {
                    append("Take Away${emptyLine}")
                }

                append(emptyLine)
            }
            // Separator line between items
            append(separatorLine)
            append(emptyLine)
            append(emptyLine)
            append(emptyLine)
        }

        return "$header$emptyLine$orderDetails"
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

    private fun addTable() {
        // Get table dialog layout
        val inflater = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_table, null)

        val tableEditTex: EditText = inflater.findViewById(R.id.newTableEditText)

        MaterialAlertDialogBuilder(requireContext())
            .setView(inflater)
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->
                // Respond to negative button press
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->
                val tableNumber = tableEditTex.text.toString() // Get the user input from the EditText

                if (tableNumber.isNotEmpty()) {
                    val uniqueID = UUID.randomUUID().toString()

                    val item = Tables(
                        uniqueID,
                        tableNumber
                    )

                    var tableOrder = TableOrder(
                        uniqueID,
                        "",
                        tableNumber,
                        null,
                        null,
                        "PROCESS",
                        "",
                        "",
                        ""
                    )

                    // Add data into table view model
                    tableViewModel.addTable(item)
                    recyclerView.adapter = TableListAdapter(this, requireContext(), tableList)
                    Log.d("get table id", tableOrder.id.toString())
                    Firestore().addTableOrder(this, requireContext(), tableOrder, recyclerView, tableList)

                    // Notify the adapter that the data has changed
//                    recyclerView.adapter?.notifyItemInserted(tableList.size - 1)
                    dialog.dismiss()
                } else {
                    // Handle case where user didn't enter any text
                    Toast.makeText(requireContext(), "Please enter a table name", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

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

    override fun onResume() {
        super.onResume()

        // Check Bluetooth connection status
        if (!ConnectionBluetoothManager.isBluetoothConnected()) {
            // Reconnect if necessary
            val selectedDevice = ConnectionBluetoothManager.getPrinterName()

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        handler.removeCallbacks(refreshRunnable)
    }
}