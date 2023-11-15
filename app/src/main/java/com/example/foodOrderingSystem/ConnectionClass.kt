package com.example.foodOrderingSystem

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

open class ConnectionClass {

    init {
        // Initialize BluetoothManager
        ConnectionBluetoothManager.initializeBluetoothAdapter()
    }
}

class ConnectionBluetoothManager private constructor() {
    companion object {
        private val instance = ConnectionBluetoothManager()
        private var printerName: String? = null
        private var bluetoothManager: BluetoothManager? = null
        private var bluetoothAdapter: BluetoothAdapter? = null
        private var bluetoothSocket: BluetoothSocket? = null
        private var bluetoothDevice: BluetoothDevice? = null
        private var outputStream: OutputStream? = null
        private var inputStream: InputStream? = null
        private var workerThread: Thread? = null

        fun getInstance(): ConnectionBluetoothManager {
            return instance
        }

        fun initializeBluetoothAdapter() {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        }

        fun getBluetoothManager(): BluetoothManager? {
            return bluetoothManager
        }

        fun getBluetoothAdapter(): BluetoothAdapter? {
            return bluetoothAdapter
        }

        fun getBluetoothSocket(): BluetoothSocket? {
            return bluetoothSocket
        }

        fun getBluetoothDevice(): BluetoothDevice? {
            return bluetoothDevice
        }

        fun getOutputSteam(): OutputStream? {
            return outputStream
        }

        fun getInputSteam(): InputStream? {
            return inputStream
        }

        fun getWorkerThread(): Thread? {
            return workerThread
        }

        fun getPrinterName(): String? {
            return printerName
        }

        fun setPrinterName(name: String) {
            this.printerName = name
        }

        fun setBluetoothAdapter(adapter: BluetoothAdapter?) {
            this.bluetoothAdapter = adapter
        }

        fun setBluetoothSocket(socket: BluetoothSocket?) {
            this.bluetoothSocket = socket
        }

        fun setBluetoothDevice(device: BluetoothDevice?) {
            this.bluetoothDevice = device
        }

        fun setOutputStream(outputStream: OutputStream?) {
            this.outputStream = outputStream
        }

        fun setInputStream(inputStream: InputStream?) {
            this.inputStream = inputStream
        }

        fun setWorkerThread(workerThread: Thread?) {
            this.workerThread = workerThread
        }

        // Other Bluetooth-related methods and properties

        fun disconnectBluetooth() {
            try {
                if (bluetoothSocket != null && bluetoothSocket?.isConnected == true) {
                    bluetoothSocket?.close()
                    outputStream?.close()
                    inputStream?.close()
                    workerThread?.interrupt()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun isBluetoothConnected(): Boolean {
            return bluetoothSocket?.isConnected == true
        }

        fun printQRDataForCustomer(data: ByteArray) {
            try {
                if (outputStream != null) {
                    outputStream?.write(data)
                    val feedPaperCut = byteArrayOf(0x10, 0x56, 66, 0x00)
                    outputStream!!.write(feedPaperCut)
                    outputStream!!.flush()
                    // Add any additional print logic as needed
                    Log.d("QR Code Data", data.contentToString())
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Bluetooth-related properties and methods
}