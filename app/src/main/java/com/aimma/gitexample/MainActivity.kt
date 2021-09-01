package com.aimma.gitexample

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.aimma.gitexample.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity(), MyAdapter.CellClickListener {
    private val mData: ArrayList<BtInfo> = ArrayList()
    private lateinit var activityMainBinding: ActivityMainBinding
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val adapter = MyAdapter()
    private val mReceiver = object : BroadcastReceiver() // 偵測藍芽狀態改變
    {
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


    // 布局以及將 Adapter 綁定到 RecycleView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        val view = activityMainBinding.root
        setContentView(view)

        // 設置RecyclerView為列表型態
        activityMainBinding.mRecyView.layoutManager = LinearLayoutManager(this)
        // 設置格線
        activityMainBinding.mRecyView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        // 將資料交給adapter
        adapter.updateList(mData)
        adapter.setCellClickListener(this)
        // 設置adapter給recycler_view
        activityMainBinding.mRecyView.adapter = adapter

        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        /**
         * 將以連線過的設備添加到陣列裡
         */
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val connection = BtInfo(device.name, device.address, -999)
            mData.add(connection)
        }


        /**
        * 事件綁定
        */
        activityMainBinding.scanBtn.setOnClickListener {
            Toast.makeText(this, "Start discovering", Toast.LENGTH_SHORT).show()
            bluetoothAdapter?.let {
                it.bluetoothLeScanner.startScan(object: ScanCallback(){
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        super.onScanResult(callbackType, result)
                        val deviceName = result.device.name ?: "None"
                        val deviceHardwareAddress = result.device.address ?: "None"

                        if(mData.none { btInfo -> btInfo.deviceAddress == deviceHardwareAddress }){
                            val connection = BtInfo(deviceName, deviceHardwareAddress, result.rssi)
                            adapter.addData(connection)
                            adapter.notifyItemInserted(mData.size - 1)
                        }else {
                            val btInfo: BtInfo =
                                mData.filter { btInfo -> btInfo.deviceAddress == deviceHardwareAddress }[0]
                            val index: Int = mData.indexOf(btInfo)
                            mData[index].deviceRRSI = result.rssi
                            mData[index].deviceName = result.device.name ?: "None"
                            adapter.updateList(mData)
                            adapter.notifyItemChanged(index)
                        }
                    }
                })
            }
        }

        /**
         * 檢查設備是否支援藍芽功能
         */
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "設備不支援藍芽功能", Toast.LENGTH_SHORT).show()
        }

        /**
         * 掃描到新裝置該如何處置
         */
        registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }
    // 處理權限部分以及綁定 Receiver 事件
    override fun onStart(){
        super.onStart()

        if (bluetoothAdapter?.isEnabled != true) {
            val myActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ activityResult ->
                if(activityResult.resultCode == Activity.RESULT_CANCELED){
                    Toast.makeText( this, "U need to turn on BT", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText( this, "Now is turning on", Toast.LENGTH_SHORT).show()
                }
            }
            myActivityLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        /**
         * 確定藍芽權限有沒有取得
         * 其中 ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION 皆為危險權限必須加以檢查
         */
        val requestPermissionLauncher =
            registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions()){ it ->
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
    }
    override fun onPause() {
        super.onPause()
        Log.d("GG", "Pause")
    }
    override fun onStop() {
        super.onStop()
        Log.d("GG", "Stop")
    }
    // 取消註冊 receiver 、藍芽發現新裝置
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
        bluetoothAdapter?.cancelDiscovery()
    }

    /**
     * 實作RecycleView點擊事件
     * 在此會依照點擊的 MAC 進行連線
     */
    override fun onCellClickListener(data: BtInfo, position: Int){
        try{
            val intent = Intent()
            intent.setClass(this, ControlPage::class.java)
            intent.putExtra("deviceMac", data.deviceAddress)
            startActivity(intent)
        }catch(e: java.lang.IllegalArgumentException){
            Log.e("GG", "MacAddress invalid")
        }
    }
}