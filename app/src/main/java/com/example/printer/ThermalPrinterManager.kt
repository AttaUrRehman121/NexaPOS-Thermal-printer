package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*

enum class PrinterConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    PRINTING,
    SUCCESS,
    ERROR
}

data class DiscoveredPrinter(
    val name: String,
    val address: String, // MAC address, IP:port, or USB ID
    val type: String, // BLUETOOTH, USB, NETWORK, VIRTUAL
    val isPaired: Boolean = false,
    val isProbablePrinter: Boolean = false,
    val signalStrength: Int? = null
)

class ThermalPrinterManager(private val context: Context) {
    private val TAG = "ThermalPrinterManager"
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val _connectionState = MutableStateFlow(PrinterConnectionState.DISCONNECTED)
    val connectionState: StateFlow<PrinterConnectionState> = _connectionState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _activePrinter = MutableStateFlow<DiscoveredPrinter?>(null)
    val activePrinter: StateFlow<DiscoveredPrinter?> = _activePrinter.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _printersList = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val printersList: StateFlow<List<DiscoveredPrinter>> = _printersList.asStateFlow()

    private val _lastVirtualPrintJob = MutableStateFlow<String?>(null)
    val lastVirtualPrintJob: StateFlow<String?> = _lastVirtualPrintJob.asStateFlow()

    private val customPrinters = mutableListOf<DiscoveredPrinter>()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothManager?.adapter
    }

    private val usbManager: UsbManager? by lazy {
        context.getSystemService(Context.USB_SERVICE) as? UsbManager
    }

    private var isReceiverRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    if (device != null) {
                        handleDiscoveredDevice(device, rssi)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isScanning.value = true
                    _statusMessage.value = "Scanning for nearby Bluetooth devices..."
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    _statusMessage.value = "Discovery completed. Found ${_printersList.value.size} device(s)."
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshPrinters()
                }
            }
        }
    }

    init {
        // Pre-add Virtual Demo Thermal Printer for testing in emulator & without physical printer
        customPrinters.add(
            DiscoveredPrinter(
                name = "Demo Virtual Thermal Printer (58mm/80mm)",
                address = "virtual://demo-pos-printer",
                type = "VIRTUAL",
                isPaired = true,
                isProbablePrinter = true
            )
        )
        refreshPrinters()
    }

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _statusMessage.value = "Bluetooth is not supported on this device"
            return
        }

        if (!adapter.isEnabled) {
            _statusMessage.value = "Please turn on Bluetooth to discover printers"
            return
        }

        if (!hasBluetoothPermissions()) {
            _statusMessage.value = "Bluetooth permissions required to discover nearby printers"
            return
        }

        try {
            if (!isReceiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                    addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                }
                context.registerReceiver(discoveryReceiver, filter)
                isReceiverRegistered = true
            }

            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            val started = adapter.startDiscovery()
            _isScanning.value = started
            if (started) {
                _statusMessage.value = "Searching for nearby Bluetooth thermal printers..."
            } else {
                _statusMessage.value = "Could not start Bluetooth discovery"
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting discovery", e)
            _statusMessage.value = "Permission denied for Bluetooth scan"
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery", e)
            _statusMessage.value = "Discovery error: ${e.localizedMessage}"
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }
            _isScanning.value = false
        } catch (ignored: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun handleDiscoveredDevice(device: BluetoothDevice, rssi: Int) {
        try {
            val name = device.name ?: "Unnamed Device"
            val address = device.address ?: return
            val isBonded = device.bondState == BluetoothDevice.BOND_BONDED

            val isProbable = isProbableThermalPrinter(name, device.bluetoothClass)

            val newPrinter = DiscoveredPrinter(
                name = name,
                address = address,
                type = "BLUETOOTH",
                isPaired = isBonded,
                isProbablePrinter = isProbable,
                signalStrength = if (rssi != Short.MIN_VALUE.toInt()) rssi else null
            )

            val current = _printersList.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.address == address }
            if (existingIndex >= 0) {
                current[existingIndex] = newPrinter
            } else {
                // Insert probable printers higher up
                if (isProbable) {
                    current.add(0, newPrinter)
                } else {
                    current.add(newPrinter)
                }
            }
            _printersList.value = current
        } catch (e: Exception) {
            Log.e(TAG, "Error handling discovered device", e)
        }
    }

    private fun isProbableThermalPrinter(name: String, btClass: BluetoothClass?): Boolean {
        val upper = name.uppercase()
        val printerKeywords = listOf(
            "POS", "PRINTER", "THERMAL", "MPT", "MTP", "RPP", "XP-", "XP",
            "GOOJPRT", "RONGTA", "EPSON", "SUNMI", "ZJ-", "PT-", "INNERPRINTER",
            "RECEIPT", "BILL", "MINI", "58MM", "80MM", "ESC"
        )
        if (printerKeywords.any { upper.contains(it) }) return true

        if (btClass != null) {
            val major = btClass.majorDeviceClass
            if (major == BluetoothClass.Device.Major.IMAGING) return true
            if (btClass.hasService(BluetoothClass.Service.RENDER)) return true
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun getPairedBluetoothPrinters(): List<DiscoveredPrinter> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            adapter.bondedDevices?.map { device ->
                val name = device.name ?: "Unknown Paired Device"
                DiscoveredPrinter(
                    name = name,
                    address = device.address,
                    type = "BLUETOOTH",
                    isPaired = true,
                    isProbablePrinter = isProbableThermalPrinter(name, device.bluetoothClass)
                )
            } ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching paired Bluetooth printers", e)
            emptyList()
        }
    }

    fun getUsbPrinters(): List<DiscoveredPrinter> {
        val manager = usbManager ?: return emptyList()
        val deviceList = manager.deviceList
        val printers = mutableListOf<DiscoveredPrinter>()

        for ((_, device) in deviceList) {
            var isPrinter = device.deviceClass == UsbConstants.USB_CLASS_PRINTER
            if (!isPrinter) {
                for (i in 0 until device.interfaceCount) {
                    if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                        isPrinter = true
                        break
                    }
                }
            }

            val deviceName = try {
                device.productName ?: device.deviceName
            } catch (e: Exception) {
                device.deviceName
            }

            printers.add(
                DiscoveredPrinter(
                    name = "$deviceName (USB ID: ${device.deviceId})",
                    address = device.deviceId.toString(),
                    type = "USB",
                    isPaired = true,
                    isProbablePrinter = isPrinter
                )
            )
        }
        return printers
    }

    fun addManualPrinter(name: String, address: String, type: String) {
        val printer = DiscoveredPrinter(
            name = name.ifBlank { "Custom $type Printer" },
            address = address.trim(),
            type = type.uppercase(),
            isPaired = true,
            isProbablePrinter = true
        )
        customPrinters.removeAll { it.address == address.trim() }
        customPrinters.add(printer)
        refreshPrinters()
    }

    fun removeManualPrinter(address: String) {
        customPrinters.removeAll { it.address == address }
        refreshPrinters()
    }

    fun refreshPrinters() {
        val paired = getPairedBluetoothPrinters()
        val usb = getUsbPrinters()

        // Combine paired, USB, and custom printers, preserving already discovered devices
        val combined = mutableListOf<DiscoveredPrinter>()
        combined.addAll(paired)
        combined.addAll(usb)
        combined.addAll(customPrinters)

        val existingScanned = _printersList.value.filter { current ->
            combined.none { it.address == current.address }
        }
        combined.addAll(existingScanned)

        _printersList.value = combined
    }

    @SuppressLint("MissingPermission")
    suspend fun pairDevice(printer: DiscoveredPrinter): Boolean = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter ?: return@withContext false
        if (printer.type != "BLUETOOTH") return@withContext true

        return@withContext try {
            val device = adapter.getRemoteDevice(printer.address)
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                true
            } else {
                _statusMessage.value = "Pairing with ${printer.name}..."
                device.createBond()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pairing error", e)
            _statusMessage.value = "Pairing failed: ${e.localizedMessage}"
            false
        }
    }

    suspend fun printBytes(
        printer: DiscoveredPrinter,
        bytes: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        _connectionState.value = PrinterConnectionState.CONNECTING
        _statusMessage.value = "Connecting to ${printer.name}..."

        return@withContext when (printer.type) {
            "VIRTUAL" -> printViaVirtual(printer, bytes)
            "BLUETOOTH" -> printViaBluetooth(printer, bytes)
            "USB" -> printViaUsb(printer, bytes)
            "NETWORK" -> printViaNetwork(printer, bytes)
            else -> {
                _connectionState.value = PrinterConnectionState.ERROR
                _statusMessage.value = "Unsupported printer interface: ${printer.type}"
                false
            }
        }
    }

    private suspend fun printViaVirtual(
        printer: DiscoveredPrinter,
        bytes: ByteArray
    ): Boolean = withContext(Dispatchers.Default) {
        _connectionState.value = PrinterConnectionState.PRINTING
        _statusMessage.value = "Simulating thermal ESC/POS print job..."

        try {
            // Convert printable bytes to readable preview text
            val text = buildString {
                append("=== VIRTUAL THERMAL PRINT SLIP (${bytes.size} bytes) ===\n")
                val clean = String(bytes, Charsets.ISO_8859_1)
                    .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
                append(clean)
                append("\n=== [PAPER CUT SUCCESSFUL] ===")
            }

            _lastVirtualPrintJob.value = text
            kotlinx.coroutines.delay(400) // realistic print delay

            _connectionState.value = PrinterConnectionState.SUCCESS
            _statusMessage.value = "Demo receipt generated & sent to virtual thermal print tape!"
            _activePrinter.value = printer
            true
        } catch (e: Exception) {
            _connectionState.value = PrinterConnectionState.ERROR
            _statusMessage.value = "Virtual print failed: ${e.localizedMessage}"
            false
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun printViaBluetooth(
        printer: DiscoveredPrinter,
        bytes: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = PrinterConnectionState.ERROR
            _statusMessage.value = "Bluetooth is disabled on this device"
            return@withContext false
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device = adapter.getRemoteDevice(printer.address)
            _statusMessage.value = "Connecting to ${printer.name} (${printer.address})..."

            // CRITICAL: cancel discovery before connecting to prevent RFCOMM connection timeouts
            try { adapter.cancelDiscovery() } catch (ignored: Exception) {}

            // Attempt 1: Standard SPP UUID socket
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
            } catch (sppError: Exception) {
                Log.w(TAG, "Standard SPP connection failed, attempting fallback RFCOMM port 1", sppError)
                try { socket?.close() } catch (ignored: Exception) {}

                // Attempt 2: Reflection fallback for Chinese/generic POS printers
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                socket = method.invoke(device, 1) as BluetoothSocket
                socket.connect()
            }

            _connectionState.value = PrinterConnectionState.PRINTING
            _statusMessage.value = "Sending ESC/POS data (${bytes.size} bytes)..."

            outputStream = socket.outputStream
            outputStream.write(bytes)
            outputStream.flush()

            // Sleep to let printer buffer drain before socket closes
            Thread.sleep(400)

            _connectionState.value = PrinterConnectionState.SUCCESS
            _statusMessage.value = "Receipt printed successfully on ${printer.name}"
            _activePrinter.value = printer
            true
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth printing failed", e)
            _connectionState.value = PrinterConnectionState.ERROR
            val msg = e.localizedMessage ?: "Connection timed out"
            _statusMessage.value = "Printer connection failed: $msg. Ensure printer is powered on and within range."
            false
        } finally {
            try { outputStream?.close() } catch (ignored: Exception) {}
            try { socket?.close() } catch (ignored: Exception) {}
        }
    }

    private suspend fun printViaUsb(
        printer: DiscoveredPrinter,
        bytes: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        val manager = usbManager
        if (manager == null) {
            _connectionState.value = PrinterConnectionState.ERROR
            _statusMessage.value = "USB subsystem unavailable on this device"
            return@withContext false
        }

        val deviceId = printer.address.toIntOrNull()
        val device: UsbDevice? = manager.deviceList.values.find { it.deviceId == deviceId }

        if (device == null) {
            _connectionState.value = PrinterConnectionState.ERROR
            _statusMessage.value = "USB printer not found. Ensure USB OTG cable is firmly connected."
            return@withContext false
        }

        try {
            val connection = manager.openDevice(device)
            if (connection == null) {
                _connectionState.value = PrinterConnectionState.ERROR
                _statusMessage.value = "USB permission required to communicate with printer"
                return@withContext false
            }

            var printerInterface = device.getInterface(0)
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                    printerInterface = iface
                    break
                }
            }

            connection.claimInterface(printerInterface, true)

            var outEndpoint = printerInterface.getEndpoint(0)
            for (i in 0 until printerInterface.endpointCount) {
                val ep = printerInterface.getEndpoint(i)
                if (ep.direction == UsbConstants.USB_DIR_OUT) {
                    outEndpoint = ep
                    break
                }
            }

            _connectionState.value = PrinterConnectionState.PRINTING
            _statusMessage.value = "Transferring USB ESC/POS data..."

            val transferred = connection.bulkTransfer(outEndpoint, bytes, bytes.size, 5000)
            connection.releaseInterface(printerInterface)
            connection.close()

            if (transferred >= 0) {
                _connectionState.value = PrinterConnectionState.SUCCESS
                _statusMessage.value = "USB receipt print complete ($transferred bytes)"
                _activePrinter.value = printer
                true
            } else {
                _connectionState.value = PrinterConnectionState.ERROR
                _statusMessage.value = "USB bulk transfer timed out"
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "USB print error", e)
            _connectionState.value = PrinterConnectionState.ERROR
            _statusMessage.value = "USB print failed: ${e.localizedMessage}"
            false
        }
    }

    private suspend fun printViaNetwork(
        printer: DiscoveredPrinter,
        bytes: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        val parts = printer.address.split(":")
        val host = parts[0].trim()
        val port = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 9100 else 9100

        var socket: Socket? = null
        var out: OutputStream? = null
        try {
            _statusMessage.value = "Connecting to network printer $host:$port..."
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), 4000)

            _connectionState.value = PrinterConnectionState.PRINTING
            out = socket.getOutputStream()
            out.write(bytes)
            out.flush()

            Thread.sleep(300)
            _connectionState.value = PrinterConnectionState.SUCCESS
            _statusMessage.value = "Network receipt printed on $host:$port"
            _activePrinter.value = printer
            true
        } catch (e: Exception) {
            Log.e(TAG, "Network print error", e)
            _connectionState.value = PrinterConnectionState.ERROR
            _statusMessage.value = "Network print failed ($host:$port): ${e.localizedMessage}"
            false
        } finally {
            try { out?.close() } catch (ignored: Exception) {}
            try { socket?.close() } catch (ignored: Exception) {}
        }
    }

    fun resetState() {
        _connectionState.value = PrinterConnectionState.DISCONNECTED
        _statusMessage.value = "Idle"
    }

    fun cleanup() {
        try {
            stopDiscovery()
            if (isReceiverRegistered) {
                context.unregisterReceiver(discoveryReceiver)
                isReceiverRegistered = false
            }
        } catch (ignored: Exception) {}
    }
}
