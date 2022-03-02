package com.aimma.gitexample

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.aimma.gitexample.databinding.ActivityBtconnectPageBinding
import java.lang.Exception
import java.util.ArrayList

class BtConnectPage:
    Fragment(R.layout.activity_btconnect_page),
    MyAdapter.CellClickListener
{
    /**
     * Binding 初始化
     */
    private lateinit var activityBtconnectPageBinding: ActivityBtconnectPageBinding

    /**
     * Launcher 初始化
     */
    private val myActivityLauncher
            = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ activityResult ->
        if(activityResult.resultCode == Activity.RESULT_CANCELED){
            Toast.makeText( context, "U need to turn on BT", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText( context, "Now is turning on", Toast.LENGTH_SHORT).show()
        }
    }
    private val requestPermissionLauncher
            = registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions()){ it ->
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

    /**
     * 各變數設置
     */
    private val mData: ArrayList<BtInfo> = ArrayList()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val adapter = MyAdapter()
    private val mReceiver = object : BroadcastReceiver(){
        override fun onReceive (context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF){
                        Toast.makeText( context, "請打開藍芽", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 在元件布局完後呼叫
     * - 綁定元件監聽
     * - 設定RecycleView
     * - 取得藍芽 Adapter
     * - 註冊藍芽掃描後的事件
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /**
         * 事件綁定
         */
        activityBtconnectPageBinding.scanBtn.setOnClickListener {
            Toast.makeText(context, "Start discovering", Toast.LENGTH_SHORT).show()
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
        activityBtconnectPageBinding.wifiConnectBtn.setOnClickListener(WifiConnectBtnListener)

        /**
         * RecycleView設定
         * */
        activityBtconnectPageBinding.mRecyView.adapter = adapter
        activityBtconnectPageBinding.mRecyView.layoutManager = LinearLayoutManager(context)
        activityBtconnectPageBinding.mRecyView.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        adapter.updateList(mData)
        adapter.setCellClickListener(this)

        /**
         * 取得藍芽適配器
         */
        bluetoothAdapter = (context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        /**
         * 將以連線過的設備添加到陣列裡
         */
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val connection = BtInfo(device.name, device.address, -999)
            mData.add(connection)
        }

        /**
         * 檢查設備是否支援藍芽功能
         */
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "設備不支援藍芽功能", Toast.LENGTH_SHORT).show()
        }

        context!!.registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    /**
     * 布局初始化以及 Binding
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        activityBtconnectPageBinding =
            ActivityBtconnectPageBinding.inflate(inflater, container, false)

        return activityBtconnectPageBinding.root
    }

    /**
     * - 確認裝置藍芽功能是否開啟
     * - 若否則要求用戶給予權限
     */
    override fun onStart(){
        super.onStart()

        if (bluetoothAdapter?.isEnabled != true) {
            myActivityLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        /**
         * 確定藍芽權限有沒有取得
         * 其中 ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION 皆為危險權限必須加以檢查
         */
        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    /**
     * - 關閉藍芽適配器的掃描功能
     */
    override fun onDestroy() {
        super.onDestroy()
        bluetoothAdapter?.cancelDiscovery()
    }

    /**
     * WiFi 的連線按鈕
     */
    private val WifiConnectBtnListener = View.OnClickListener{
        if (validIP(activityBtconnectPageBinding.ipAddrEditText.text.toString())) {
            try {
                val intent = Intent()
                intent.setClass(context!!, WiFiControlPage::class.java)
                intent.putExtra("deviceIP", activityBtconnectPageBinding.ipAddrEditText.text.toString())
                startActivity(intent)
            }catch(e: java.lang.IllegalArgumentException){
                Log.e("GG", "IP invalid")
            }
        }else Toast.makeText(context, "Invalid ip", Toast.LENGTH_SHORT).show()
    }

    /**
     * 檢查 IP 是否為正確格式
     */
    private fun validIP(ip: String): Boolean {
        if(ip.count { it == '.' } == 3){
            for (number in ip.split('.')){
                try {
                    val i = number.toInt()
                    if (i !in 0..255 || number != i.toString()) return false
                }catch (e: Exception){
                    return false
                }
            }
            return true
        }else return false

    }

    /**
     * 當 RecycleView 的內部 Cell 被點選時觸發
     */
    override fun onCellClickListener(data: BtInfo, position: Int){
        try{
            val intent = Intent()
            intent.setClass(context!!, ControlPage::class.java)
            intent.putExtra("deviceMac", data.deviceAddress)
            startActivity(intent)
        }catch(e: java.lang.IllegalArgumentException){
            Log.e("GG", "MacAddress invalid")
        }
    }
}