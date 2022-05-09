package com.aimma.gitexample

import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.aimma.gitexample.databinding.ActivityConnectBinding
import com.google.android.material.snackbar.Snackbar
import java.util.*

class ConnectPage: Fragment(R.layout.activity_connect) {

    private lateinit var activityConnectBinding: ActivityConnectBinding
    /**
     * Launcher 初始化
     */
    // 用以請求藍芽打開
    private val myActivityLauncher
            = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ activityResult ->
        if(activityResult.resultCode == Activity.RESULT_CANCELED){
            Toast.makeText( context, "U need to turn on BT", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText( context, "Now is turning on", Toast.LENGTH_SHORT).show()
        }
    }
    // 請求位置權限
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
     * 藍芽適配器變數
     * 以及檢查藍芽狀態的廣播接收
     */
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val mReceiver = object : BroadcastReceiver(){
        override fun onReceive (context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF){
                        Snackbar.make( view!!, "請勿關閉藍芽", Snackbar.LENGTH_SHORT).show()
                        myActivityLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                }
            }
        }
    }


    /**
     * 檢查設備藍芽支援度
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /**
         * 檢查設備是否支援藍芽功能
         */
        bluetoothAdapter = (context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (bluetoothAdapter == null) {
            Snackbar.make(this.view!!, "設備不支援藍芽功能", Snackbar.LENGTH_SHORT).show()
        }

        // 註冊廣播接收器
        context!!.registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        /**
         * 監聽設定
         */
        activityConnectBinding.btConnectBtn.setOnClickListener(btConnectBtnListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activityConnectBinding = ActivityConnectBinding.inflate(inflater, container, false)
        activityConnectBinding.btConnectProgress.visibility = View.GONE

        return activityConnectBinding.root
    }

    /**
     * 檢查位置權限是否給予
     */
    override fun onStart() {
        super.onStart()
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
     * 連結按鈕被按下時
     */
    private val btConnectBtnListener = View.OnClickListener {
        activityConnectBinding.btConnectProgress.visibility = View.VISIBLE
        if(activityConnectBinding.btConnectProgress.visibility==View.VISIBLE) {
            Thread {
                (activity as MainActivity).btConnect(activityConnectBinding.btMacEdit.text.toString())
                (activity as MainActivity).runOnUiThread {
                    activityConnectBinding.btConnectProgress.visibility = View.GONE
                }
                Snackbar.make(activityConnectBinding.root, "Connect failed...", Snackbar.LENGTH_SHORT).show()
            }.start()
        }
    }

}