package com.aimma.gitexample

import android.content.Intent
import android.os.*
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aimma.gitexample.databinding.ActivityWebsiteBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.property_list_item.*
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.*


class Website:
    Fragment(R.layout.activity_website),
    PropertyAdapter.CellClickListener
{

    /**
     * API 網址設定
     */
    private var session: String = ""

    private var searchThread: Thread? = null
    private lateinit var activityWebsiteBinding: ActivityWebsiteBinding
    private var propertyDataArray: ArrayList<PropertyInfo> = ArrayList()
    private var propertyAdapter = PropertyAdapter(this)

    override fun onDestroy() {
        super.onDestroy()
        Log.i("GG", "Fragment website destroy")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        session = this.arguments?.getString("session") ?: ""
        if(session == "") {
            Toast.makeText(this.context!!, "尚未登入", Toast.LENGTH_SHORT).show()
            val intent = Intent()
            intent.setClass(this.context!!, LoginPage::class.java)
            startActivity(intent)
        }
        /**
         * 網站請求資料
         */
        propertyAdapter.setSession(session)
        propertyAdapter.setCellClickListener(this)
        Thread{
            // 取得資料
            val rawJson = webAPIConnect(Namespace.webAPIUrl + "getData/", session)
            // 若 rawData 為空則跳過
            if(rawJson == "") return@Thread
            // 解析 RawData
            val propertyJson = JSONObject(rawJson)
            val propertyArray = propertyJson.getJSONArray("data")
            Log.i("GG", propertyArray.getJSONObject(0).toString())
            // 將讀取到的資料塞入 Adapter
            for (i in 0 until propertyArray.length()) {
                val connection = PropertyInfo(
                    propertyName = propertyArray.getJSONObject(i).getString("product_name"),
                    propertyImage = propertyArray.getJSONObject(i).getString("image"),
                    propertyNumber = propertyArray.getJSONObject(i).getString("number"),
                    propertyProductNumber = propertyArray.getJSONObject(i).getString("product_number"),
                    propertyTip = propertyArray.getJSONObject(i).getString("tip"),
                    propertyGetDate = propertyArray.getJSONObject(i).getString("get_date"),
                    propertyAgeLimit = propertyArray.getJSONObject(i).getString("age_limit"),
                    propertyQuantity = propertyArray.getJSONObject(i).getString("quantity"),
                    propertySingleValue = propertyArray.getJSONObject(i).getString("single_value"),
                    propertyPosition = propertyArray.getJSONObject(i).getString("position"),
                    propertyLabelPosition = propertyArray.getJSONObject(i).getString("label_position"),
                    propertyUnit = propertyArray.getJSONObject(i).getString("unit"),
                    propertyStatus = propertyArray.getJSONObject(i).getString("status"),
                    propertyIsCheck = propertyArray.getJSONObject(i).getString("is_check"),
                )

                propertyDataArray.add(connection)
                val msg = Message()
                msg.data.putString("action", "itemChange")
                msg.data.putInt("id", i)
                handler.sendMessage(msg)
            }
        }.start()
    }

    /**
     * 處理 RecycleView的更新
     */
    private val handler = object : Handler(Looper.getMainLooper())
    {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.data.getString("action")){
                "clear" -> {
                    propertyAdapter.notifyItemRangeRemoved(0, msg.data.getInt("size"))
                }
                "itemChange" -> {
                    propertyAdapter.notifyItemChanged(msg.data.getInt("id"))
                }
            }
        }
    }

    /**
     * 綁定 RecycleView 的列表以及 LayoutInflate
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        activityWebsiteBinding = ActivityWebsiteBinding.inflate(inflater, container, false)

        // 設置RecyclerView為列表型態
        activityWebsiteBinding.propertyRecycleView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        // 將資料交給adapter
        propertyAdapter.updateList(propertyDataArray)
        // 設置adapter給recycler_view
        activityWebsiteBinding.propertyRecycleView.adapter = propertyAdapter

        activityWebsiteBinding.editTextTextPersonName2.onFocusChangeListener =
            View.OnFocusChangeListener {
                    p0, p1 ->
                Log.i("GG", p1.toString())
            }

        activityWebsiteBinding.editTextTextPersonName2.setOnKeyListener(searchEditListener)

        return activityWebsiteBinding.root
    }

    private val searchEditListener = object: View.OnKeyListener{
        override fun onKey(p0: View?, p1: Int, p2: KeyEvent?): Boolean {
            if((p2?.action == KeyEvent.ACTION_DOWN) && (p1 == KeyEvent.KEYCODE_ENTER)){
                if(searchThread?.isAlive == true && searchThread != null){
                    searchThread?.interrupt()
                }
                searchThread = Thread {
                    // 取得資料
                    val rawJson = webAPIConnect(
                        Namespace.webAPIUrl + "getData/?search=" + (p0 as EditText).text,
                        session
                    )
                    // 若 rawData 為空則跳過
                    if (rawJson == "") return@Thread
                    Log.i("GG", rawJson)
                    // 解析 RawData
                    val propertyJson = JSONObject(rawJson)
                    val propertyArray = propertyJson.getJSONArray("data")
                    val msg = Message()
                    // 清空 Array 並提醒 Adapter 做改變
                    val size = propertyDataArray.size
                    propertyDataArray.clear()
                    msg.data.putString("action", "clear")
                    msg.data.putInt("size", size)
                    handler.sendMessage(msg)
                    // 將讀取到的資料塞入 Adapter
                    for (i in 0 until propertyArray.length()) {
                        val connection = PropertyInfo(
                            propertyName = propertyArray.getJSONObject(i)
                                .getString("product_name"),
                            propertyImage = propertyArray.getJSONObject(i).getString("image"),
                            propertyNumber = propertyArray.getJSONObject(i).getString("number"),
                            propertyProductNumber = propertyArray.getJSONObject(i)
                                .getString("product_number"),
                            propertyTip = propertyArray.getJSONObject(i).getString("tip"),
                            propertyGetDate = propertyArray.getJSONObject(i)
                                .getString("get_date"),
                            propertyAgeLimit = propertyArray.getJSONObject(i)
                                .getString("age_limit"),
                            propertyQuantity = propertyArray.getJSONObject(i)
                                .getString("quantity"),
                            propertySingleValue = propertyArray.getJSONObject(i)
                                .getString("single_value"),
                            propertyPosition = propertyArray.getJSONObject(i)
                                .getString("position"),
                            propertyLabelPosition = propertyArray.getJSONObject(i)
                                .getString("label_position"),
                            propertyUnit = propertyArray.getJSONObject(i).getString("unit"),
                            propertyStatus = propertyArray.getJSONObject(i).getString("status"),
                            propertyIsCheck = propertyArray.getJSONObject(i)
                                .getString("is_check"),
                        )

                        propertyDataArray.add(connection)
                        val msg = Message()
                        msg.data.putInt("id", i)
                        handler.sendMessage(msg)
                    }
                }
                searchThread?.start()
                return true
            }
            return false
        }
    }

    /**
     * WebAPI 連線用
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
        if(connect.responseCode == 403){
            Log.e("GG", "API Error code 403")
            return ""
        }else if(connect.responseCode != 200) return ""

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

    /**
     * RecycleView 的 ClickListener
     */
    override fun onCellClickListener(data: PropertyInfo, position: Int)
    {
        Log.i("GG", data.toString())

    }
}