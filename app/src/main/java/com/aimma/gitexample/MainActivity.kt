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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.aimma.gitexample.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.net.URL
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.*
import javax.net.ssl.*

class MainActivity : AppCompatActivity()
    , MyAdapter.CellClickListener
    , NavigationView.OnNavigationItemSelectedListener
{
    /**
     * API 網址設定
     */
    private val webAPIUrl = "https://192.168.1.134/"

    /**
     * 頁面跳轉 Launcher 設定
     */
    private var loginLauncher: ActivityResultLauncher<Intent>
        = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if (it.resultCode == RESULT_CANCELED) return@registerForActivityResult
            val session = it.data?.getStringExtra("session") ?: ""
            Log.i("GG", session)
            Thread{
                Log.i("GG", webAPIConnect(webAPIUrl+"getUserInfo/", session))
            }.start()
        }
    private val myActivityLauncher
        = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { activityResult ->
        if(activityResult.resultCode == Activity.RESULT_CANCELED){
            Toast.makeText( this, "U need to turn on BT", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText( this, "Now is turning on", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 各個變數設置
     */
    private val mData: ArrayList<BtInfo> = ArrayList()
    private lateinit var activityMainBinding: ActivityMainBinding
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val adapter = MyAdapter()
    private val mReceiver = object : BroadcastReceiver(){
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        /**
         * ActionBar 設定
         * */
        setSupportActionBar(activityMainBinding.toolbar)
        supportActionBar?.title = "  實驗室智慧管家"
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setLogo(R.drawable.ai_32x32)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        /**
         * Drawer 以及 NavigationView 設定
         * */
        val toggle = ActionBarDrawerToggle(this, activityMainBinding.root, activityMainBinding.toolbar, R.string.app_name, R.string.app_name)
        toggle.isDrawerIndicatorEnabled = true
        toggle.syncState()
        activityMainBinding.root.addDrawerListener(toggle)
        activityMainBinding.navView.setNavigationItemSelectedListener(this)

        /**
         * RecycleView設定
         * */
//        activityMainBinding.mRecyView.layoutManager = LinearLayoutManager(this)
//        activityMainBinding.mRecyView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
//        adapter.updateList(mData)
//        adapter.setCellClickListener(this)
//        activityMainBinding.mRecyView.adapter = adapter
//        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter



//        /**
//         * 將以連線過的設備添加到陣列裡
//         */
//        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
//        pairedDevices?.forEach { device ->
//            val connection = BtInfo(device.name, device.address, -999)
//            mData.add(connection)
//        }


        /**
        * 事件綁定
        */
//        activityMainBinding.scanBtn.setOnClickListener {
//            Toast.makeText(this, "Start discovering", Toast.LENGTH_SHORT).show()
//            bluetoothAdapter?.let {
//                it.bluetoothLeScanner.startScan(object: ScanCallback(){
//                    override fun onScanResult(callbackType: Int, result: ScanResult) {
//                        super.onScanResult(callbackType, result)
//                        val deviceName = result.device.name ?: "None"
//                        val deviceHardwareAddress = result.device.address ?: "None"
//
//                        if(mData.none { btInfo -> btInfo.deviceAddress == deviceHardwareAddress }){
//                            val connection = BtInfo(deviceName, deviceHardwareAddress, result.rssi)
//                            adapter.addData(connection)
//                            adapter.notifyItemInserted(mData.size - 1)
//                        }else {
//                            val btInfo: BtInfo =
//                                mData.filter { btInfo -> btInfo.deviceAddress == deviceHardwareAddress }[0]
//                            val index: Int = mData.indexOf(btInfo)
//                            mData[index].deviceRRSI = result.rssi
//                            mData[index].deviceName = result.device.name ?: "None"
//                            adapter.updateList(mData)
//                            adapter.notifyItemChanged(index)
//                        }
//                    }
//                })
//            }
//        }
//        activityMainBinding.wifiConnectBtn.setOnClickListener(WifiConnectBtnListener())
        activityMainBinding.loginBtn.setOnClickListener{
            activityMainBinding.root.closeDrawers()
            val intent = Intent()
            intent.setClass(this, LoginPage::class.java)
            loginLauncher.launch(intent)
        }


        /**
         * 檢查設備是否支援藍芽功能
         */
//        if (bluetoothAdapter == null) {
//            Toast.makeText(this, "設備不支援藍芽功能", Toast.LENGTH_SHORT).show()
//        }

        /**
         * 掃描到新裝置該如何處置
         */
//        registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actionbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // 處理權限部分以及綁定 Receiver 事件ㄔ
    override fun onStart(){
        super.onStart()

//        if (bluetoothAdapter?.isEnabled != true) {
//            myActivityLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
//        }
//
//        /**
//         * 確定藍芽權限有沒有取得
//         * 其中 ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION 皆為危險權限必須加以檢查
//         */
//        val requestPermissionLauncher =
//            registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions()){ it ->
//                it.entries.forEach {
//                    // 檢查權限部分是否同意若否則 it.value 為 false
//                    when (it.key) {
//                        android.Manifest.permission.ACCESS_COARSE_LOCATION -> {
//                            Log.d("PERMISSION", "ACCESS_COARSE_LOCATION ${it.value}")
//                        }
//                        android.Manifest.permission.ACCESS_FINE_LOCATION -> {
//                            Log.d("PERMISSION", "ACCESS_FINE_LOCATION ${it.value}")
//                        }
//                    }
//                }
//            }
//        requestPermissionLauncher.launch(
//            arrayOf(
//                android.Manifest.permission.ACCESS_COARSE_LOCATION,
//                android.Manifest.permission.ACCESS_FINE_LOCATION
//            )
//        )
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

//    inner class WifiConnectBtnListener: View.OnClickListener{
//        // WiFi連線按鈕被按下時
//        override fun onClick(p0: View?) {
//            if (validIP(activityMainBinding.ipAddrEditText.text.toString())) {
//                try {
//                    val intent = Intent()
//                    intent.setClass(this@MainActivity, WiFiControlPage::class.java)
//                    intent.putExtra("deviceIP", activityMainBinding.ipAddrEditText.text.toString())
//                    startActivity(intent)
//                }catch(e: java.lang.IllegalArgumentException){
//                        Log.e("GG", "IP invalid")
//                }
//            }else Toast.makeText(this@MainActivity, "Invalid ip", Toast.LENGTH_SHORT).show()
//        }
//
//        // 檢查IP是否正確
//        private fun validIP(ip: String): Boolean {
//            if(ip.count { it == '.' } == 3){
//                for (number in ip.split('.')){
//                    try {
//                        val i = number.toInt()
//                        if (i !in 0..255 || number != i.toString()) return false
//                    }catch (e: Exception){
//                        return false
//                    }
//                }
//                return true
//            }else return false
//        }
//    }

    /**
     * Listener 部分
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.about -> {
                Log.d("Nav", "about")
            }
            R.id.website -> {
                Log.d("Nav", "website")
            }
            R.id.setting -> {
                Log.d("Nav", "setting")
            }
        }
        activityMainBinding.root.closeDrawers()
        return true
    }

    /**
     * 網頁串接連線部分
     */
    private fun webAPIConnect(APIUrl: String ,session: String): String{
        // 設定連線網址以及連線設定
        val url = URL(APIUrl)
        val connect: HttpsURLConnection = url.openConnection() as HttpsURLConnection

        // 憑證設定並導入憑證
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val caInput: InputStream = BufferedInputStream(resources.openRawResource(R.raw.code))
        val ca = caInput.use { cf.generateCertificate(it) }
        // Create a KeyStore containing our trusted CAs
        val keyStoreType = KeyStore.getDefaultType()
        val keyStore = KeyStore.getInstance(keyStoreType).apply {
            load(null, null)
            setCertificateEntry("ca", ca as Certificate)
        }
        // Create a TrustManager that trusts the CAs inputStream our KeyStore
        val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
        val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
            init(keyStore)
        }
        // Create an SSLContext that uses our TrustManager
        val context: SSLContext = SSLContext.getInstance("TLS").apply {
            init(null, tmf.trustManagers, null)
        }

        // 連線部分設定
        connect.hostnameVerifier = AllowAllHostNameVerifier()
        connect.sslSocketFactory = context.socketFactory
        connect.doOutput = true
        connect.doInput = true
        connect.connectTimeout = 5000
        connect.readTimeout = 5000
        connect.setRequestProperty("Cookie", "sessionid=$session")
        connect.connect()

        // 若連線不成功則返回
        if(connect.responseCode != 200) return ""

        val inputStream = connect.inputStream
        val reader = InputStreamReader(inputStream)
        val bufReader = BufferedReader(reader)
        var result = String()
        var data = bufReader.readLine()

        while(data != ""){
            result += data
            data = bufReader.readLine() ?: ""
        }
        return result
    }

    inner class AllowAllHostNameVerifier : HostnameVerifier {
        override fun verify(p0: String?, p1: SSLSession?): Boolean {
            return true;
        }
    }
}