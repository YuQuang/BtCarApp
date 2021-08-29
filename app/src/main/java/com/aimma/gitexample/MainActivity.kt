package com.aimma.gitexample

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aimma.gitexample.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(), MyAdapter.CellClickListener {
    private val mData: ArrayList<BtInfo> = ArrayList()
    private lateinit var activityMainbinding: ActivityMainBinding
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val adapter = MyAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityMainbinding = ActivityMainBinding.inflate(layoutInflater)

        val view = activityMainbinding.root
        setContentView(view)
        // 準備資料，塞50個項目到ArrayList裡
        for (i in 0..4) {
            val connection = BtInfo("Test", "AC:13:GE:61:AC")
            mData.add(connection)
        }
        val connection = BtInfo("Test1", "AC:13:GE:61:AA")
        mData.add(connection)

        // 設置RecyclerView為列表型態
        activityMainbinding.mRecyView.layoutManager = LinearLayoutManager(this)
        // 設置格線
        activityMainbinding.mRecyView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // 將資料交給adapter
        adapter.updateList(mData)
        adapter.setCellClickListener(this)
        // 設置adapter給recycler_view
        activityMainbinding.mRecyView.adapter = adapter
    }

    @SuppressLint("MissingPermission")
    override fun onStart(){
        super.onStart()
        /********************
         * 檢查設備是否支援藍芽功能
         ********************/
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "設備不支援藍芽功能", Toast.LENGTH_SHORT).show()
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val myActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ activityResult ->
                if(activityResult.resultCode == Activity.RESULT_CANCELED){
                    Toast.makeText( this, "U need to turn on BT", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText( this, "Now is turning on", Toast.LENGTH_SHORT).show()
                }
            }
            myActivityLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        /********************
         * 確定藍芽權限有沒有取得
         * 其中 ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION 皆為危險權限必須加以檢查
         *********************/
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ){ it ->
                it.entries.forEach {
                    // 檢查權限部分是否同意若否則 it.value 為 false
                    when (it.key) {
                        android.Manifest.permission.ACCESS_COARSE_LOCATION -> {
                            Log.d("PERMISSION", "ACCESS_COARSE_LOCATION ${it.value}")
                        }
                        android.Manifest.permission.ACCESS_FINE_LOCATION -> {
                            Log.d("PERMISSION", "ACCESS_FINE_LOCATION ${it.value}")
                        }
                    }
                }
            }
        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        )


        /**************
         * 使此設備可被其他裝置搜尋到
         ***************/
//        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
//            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30)
//        }
//        startActivity(discoverableIntent)

        /*******************
         * 掃描到新裝置該如何處置
         *******************/
        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        bluetoothAdapter?.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
        unregisterReceiver(mReceiver)
        bluetoothAdapter?.cancelDiscovery()
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    @SuppressLint("MissingPermission")
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    var deviceName = device?.name
                    var deviceHardwareAddress = device?.address

                    deviceName = deviceName ?: "None"
                    deviceHardwareAddress = deviceHardwareAddress ?: "None"

                    if(mData.none { d -> d.deviceAddress == deviceHardwareAddress }){
                        val connection = BtInfo(deviceName, deviceHardwareAddress)
                        adapter.addData(connection)
                        adapter.notifyItemInserted(mData.size - 1)
                    }
                }
            }
        }
    }
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive (context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF){
                        Toast.makeText( applicationContext, "請打開藍芽", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    var MY_UUID = UUID.fromString("192770fa-4a48-4acd-8b37-f9716c2c7899")
    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice?) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device?.createRfcommSocketToServiceRecord(MY_UUID)
        }
        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                Toast.makeText(applicationContext, "Connected", Toast.LENGTH_SHORT).show()
            }
        }
        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("GG", "Could not close the client socket")
            }
        }
    }

    override fun onCellClickListener(data: BtInfo){
        Log.d("GG", data.toString())
    }
}