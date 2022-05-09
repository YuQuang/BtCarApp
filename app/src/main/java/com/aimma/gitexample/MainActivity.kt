package com.aimma.gitexample

import android.bluetooth.*
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import com.aimma.gitexample.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
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
    private var session: String? = null

    /**
     * 藍芽適配器變數
     * 以及檢查藍芽狀態的廣播接收
     */
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var myUUID: UUID = UUID.fromString("192770fa-4a48-4acd-8b37-f9716c2c7899")  // 藍芽用 UUID
    private var mSocket: BluetoothSocket? = null                                              // 藍芽 Socket
    private var mBluetoothGatt: BluetoothGatt? = null                                         // 藍芽 Gatt
    private val mGattCallback: BluetoothGattCallback = object: BluetoothGattCallback() {

        var keepRead = true

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) gatt.readRemoteRssi()
            else if(newState == BluetoothProfile.STATE_DISCONNECTED) keepRead = false
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if(status != BluetoothGatt.GATT_SUCCESS) Log.d("GG", "onServicesDiscovered received: $status")
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            Handler(Looper.getMainLooper()).postDelayed({
                if(rssi > -3){
                    val output = OutputStreamWriter(mSocket?.outputStream)
                    output.write("WakeUp\n")
                    output.flush()
                    output.close()
                }
                Log.d("GG", rssi.toString())
                if(keepRead) mBluetoothGatt?.readRemoteRssi()
            }, 500)
        }
    }

    /**
     * 頁面跳轉 Launcher 設定
     */
    private var loginLauncher: ActivityResultLauncher<Intent>
        = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if (it.resultCode == RESULT_CANCELED) return@registerForActivityResult
            val session = it.data?.getStringExtra("session") ?: ""
            this.session = session
            Thread{
                val userInfo = JSONObject(webAPIConnect(Namespace.webAPIUrl+"getUserInfo/", session))
                runOnUiThread(Runnable {
                    val userName = userInfo.getString("userName")
                    val groupArr = userInfo.getJSONArray("userGroup")
                    var userGroup: String = String()

                    Snackbar.make(activityMainBinding.root, "歡迎$userGroup! $userName.", Snackbar.LENGTH_SHORT).show()

                    for(i in 1..groupArr.length())
                        userGroup+=groupArr.getString(i-1)
                    activityMainBinding.UserNameInfo.text = userName
                    activityMainBinding.UserGroupInfo.text = userGroup
                })
            }.start()
        }

    /**
     * 各個變數設置
     */
    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        Log.i("GG", "Main Act $session")

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
        * 事件綁定
        */
        activityMainBinding.loginBtn.setOnClickListener{
            activityMainBinding.root.closeDrawers()
            val intent = Intent()
            intent.setClass(this, LoginPage::class.java)
            loginLauncher.launch(intent)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actionbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

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

    /**
     * 側邊選單選取的監聽
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.bluetooth -> {
                val transaction = supportFragmentManager.beginTransaction()
                val btFragment = BtConnectPage()
                transaction.replace(activityMainBinding.fragmentContainerView.id, btFragment)
                transaction.commitNowAllowingStateLoss()
            }
            R.id.website -> {
                val transaction = supportFragmentManager.beginTransaction()
                val websiteFragment = Website()
                val bundle = Bundle()
                bundle.putString("session", session)
                websiteFragment.arguments = bundle
                transaction.replace(activityMainBinding.fragmentContainerView.id, websiteFragment)
                transaction.commitNowAllowingStateLoss()
                Log.d("Nav", "website")
            }
            R.id.connect -> {
                val transaction = supportFragmentManager.beginTransaction()
                val connectFragment = ConnectPage()
                transaction.replace(activityMainBinding.fragmentContainerView.id, connectFragment)
                transaction.commitNowAllowingStateLoss()
            }
        }
        activityMainBinding.root.closeDrawers()
        return true
    }

    /**
     * 藍芽連線部分
     */
    fun btConnect(mac: String){
        try{
            // 取得藍芽適配器
            bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            val bluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val device = bluetoothManager.adapter.getRemoteDevice(mac)
            mSocket = device.createRfcommSocketToServiceRecord(myUUID)
            mSocket?.connect()
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
            runOnUiThread{
                activityMainBinding.BtDeviceMac.text = mBluetoothGatt?.device?.address
            }
            val transaction = supportFragmentManager.beginTransaction()
            val btFragment = BtConnectPage()
            transaction.replace(activityMainBinding.fragmentContainerView.id, btFragment)
            transaction.commitNowAllowingStateLoss()
        }catch(e: IllegalArgumentException){
            Log.e("GG", "In Main Activity, $e")
        }catch(e: Exception){
            Log.e("GG", e.toString())
        }
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
            return true
        }
    }
}