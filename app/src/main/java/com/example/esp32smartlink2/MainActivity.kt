import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.Activity
import android.content.Context
import android.widget.Toast
import android.bluetooth.BluetoothAdapter

import java.util.*

//main activity, dumbass.


// MainActivity
class MainActivity : AppCompatActivity() {

    private val TAG = "BLEExample"
    private val deviceName = "ESP32_Test_Device"
    private val serviceUuid = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
    private val characteristicUuid = UUID.fromString("87654321-4321-4321-4321-9876543210fe")

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private val PERMISSION_REQUEST_CODE = 1

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                initializeBluetooth()
            } else {
                Toast.makeText(toString("enable_the_perms_dummy"), Toast.LENGTH_SHORT).show() // Updated message
            }
        }
    }



     fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check permissions before proceeding
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE)
        } else {
            initializeBluetooth()
        }
    }



    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "Bluetooth is disabled or not supported!")
            return
        }

        scanForDevices()
    }

    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "Bluetooth is disabled or not supported!")
            return
        }

        scanForDevices()
    }

    private fun scanForDevices() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "Bluetooth LE Scanner not available.")
            return
        }

        Log.i(TAG, "Starting BLE scan...")

        scanner.startScan(object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (device.name == deviceName) {
                    Log.i(TAG, "Found device: ${device.name}")
                    scanner.stopScan(this)
                    connectToDevice(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error: $errorCode")
            }
        })
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Log.i(TAG, "Connecting to device: ${device.name}")
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i(TAG, "Connected to GATT server.")
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i(TAG, "Disconnected from GATT server.")
                        bluetoothGatt = null
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val characteristic = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
                    if (characteristic != null) {
                        sendData(characteristic, "Hello from Android!")
                    } else {
                        Log.e(TAG, "Characteristic not found!")
                    }
                } else {
                    Log.e(TAG, "Service discovery failed with status: $status")
                }
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val response = characteristic.getStringValue(0)
                    Log.i(TAG, "Received: $response")
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "Write successful: ${characteristic.getStringValue(0)}")
                } else {
                    Log.e(TAG, "Write failed with status: $status")
                }
            }
        })
    }

    private fun sendData(characteristic: BluetoothGattCharacteristic, data: String) {
        characteristic.value = data.toByteArray()
        val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
        if (success) {
            Log.i(TAG, "Data sent: $data")
        } else {
            Log.e(TAG, "Failed to send data.")
        }
    }
}