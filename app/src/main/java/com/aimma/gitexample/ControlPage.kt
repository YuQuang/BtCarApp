package com.aimma.gitexample

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.graphics.*
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.aimma.gitexample.databinding.ActivityControlPageLandscapeBinding
import java.io.*
import java.lang.Exception
import java.lang.ref.WeakReference
import java.net.Socket
import java.util.*
import android.os.Looper
import org.json.JSONArray
import org.json.JSONTokener

class ControlPage : AppCompatActivity() {
    private lateinit var activityControlPageLandscapeBinding: ActivityControlPageLandscapeBinding
    private var loadingDialog: LoadingDialog? = null                                          // 嘗試連線時顯示的 LoadingDialog
    private var mSocket: BluetoothSocket? = null                                              // 藍芽 Socket
    private var mBluetoothGatt: BluetoothGatt? = null                                         // 藍芽 Gatt
    private var connectThread: Thread? = null                                                 // 一開始嘗試連線的 Thread
    private var pictureThread: Thread? = null                                                 // 接收樹梅派圖片串流的 Thread
    private var pictureTrackingThread: Thread? = null                                         // 圖片辨識串流的 Thread
    private var socket: Socket? = null                                                        // 與樹梅派連線的 Socket
    private var trackingServerSocket: Socket? = null                                          // 連線至追蹤伺服器的 Socket
    private var base64CodedPic: String = "none"                                               // 樹梅派傳來編碼過的圖片 (JPEG)
    private var result: JSONArray? = null                                                     // 物件追蹤判斷結果
    private var myUUID: UUID = UUID.fromString("192770fa-4a48-4acd-8b37-f9716c2c7899")  // 藍芽用 UUID

    /**
     * 管理藍芽連線狀態
     */
    private val mGattCallback: BluetoothGattCallback = object: BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) gatt.readRemoteRssi()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if(status != BluetoothGatt.GATT_SUCCESS) Log.d("GG", "onServicesDiscovered received: $status")
        }

        @SuppressLint("SetTextI18n")
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            Handler(Looper.getMainLooper()).postDelayed({
                activityControlPageLandscapeBinding.statusText.text = "rssi:$rssi \n status: $status"
                mBluetoothGatt?.readRemoteRssi()
            }, 500)
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityControlPageLandscapeBinding = ActivityControlPageLandscapeBinding.inflate(layoutInflater)
        activityControlPageLandscapeBinding.root.apply {
            setContentView(this)
        }

        /**
         * 返回上一頁，並斷開蓮線
         */
        activityControlPageLandscapeBinding.lastPageBtn.setOnClickListener {
            finish()
        }
        activityControlPageLandscapeBinding.forwardBtn.setOnTouchListener(MyTouchListener("forward"))
        activityControlPageLandscapeBinding.backBtn.setOnTouchListener(MyTouchListener("back"))
        activityControlPageLandscapeBinding.leftBtn.setOnTouchListener(MyTouchListener("left"))
        activityControlPageLandscapeBinding.rightBtn.setOnTouchListener(MyTouchListener("right"))

        /**
         * 設定對話框類型
         * 依照設定創建對話框類
         */
        loadingDialog = LoadingDialog(this)
        loadingDialog?.dialogLoadingBinding?.button?.setOnClickListener { onBackPressed() }
        loadingDialog?.setOnCancelListener { onBackPressed() }
        loadingDialog?.startLoadingDialog()

        intent.getStringExtra("deviceMac")?.let {
            val bluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val device = bluetoothManager.adapter.getRemoteDevice(it)
            mSocket = device.createRfcommSocketToServiceRecord(myUUID)
            connectThread = ConnectThread(bluetoothManager.adapter.getRemoteDevice(it))
            connectThread?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket?.close()
        mBluetoothGatt?.close()
        socket?.close()
    }

    // 若按返回則取消連線，並返回上一頁
    override fun onBackPressed() {
        super.onBackPressed()
        if(connectThread?.isAlive == true){
            connectThread?.interrupt()
            loadingDialog?.dismiss()
            finish()
        }
        pictureThread?.interrupt()
    }

    // 不管連線是否成功都會呼叫此函數，並把連線的Socket傳入
    @RequiresApi(Build.VERSION_CODES.O)
    fun afterSocketConnected(mSocket: BluetoothSocket){
        this.mSocket = mSocket
        if(!mSocket.isConnected){
            mSocket.close()
            loadingDialog?.dismiss()
            finish()

            object: Handler(Looper.getMainLooper()){
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    Toast.makeText(applicationContext, "Failed connect to ${mSocket.remoteDevice.address}", Toast.LENGTH_SHORT).show()
                }
            }.sendMessage(Message())
        }else{
            loadingDialog?.dismiss()
            pictureThread = PictureThread(MyHandler(this))
            pictureThread?.start()

            object: Handler(Looper.getMainLooper()){
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    Toast.makeText(applicationContext, "Connected to ${mSocket.remoteDevice.address}", Toast.LENGTH_SHORT).show()
                }
            }.sendMessage(Message())

            Looper.prepare()
            Looper.loop()
        }
    }

    /**
     * 連線至樹梅派接收圖片
     */
    @RequiresApi(Build.VERSION_CODES.O)
    inner class PictureThread(private var handler: MyHandler): Thread(){
        // IP & Port參數設定
        private val host: String = "10.10.11.79"
        private val port: Int = 65432

        override fun run() {
            try{
                socket = Socket(host, port)                             // 嘗試連線至樹梅派
                val input = socket?.getInputStream()                    // 取得輸入流
                val reader = BufferedReader(InputStreamReader(input))   // 創建輸入緩衝讀取區

                base64CodedPic = reader.readLine()                      // 從緩衝區讀取一行 (此為一張編碼過的圖片)
                pictureTrackingThread = PictureTrackingThread()         // 開始追蹤線程
                pictureTrackingThread?.start()
                while (true) {                                          // 持續從樹梅派圖片
                    val msg = Message()
                    msg.data.putString("data", base64CodedPic)
                    handler.sendMessage(msg)
                    base64CodedPic = reader.readLine()
                }
            }catch (e: Exception){
                Log.d("Net", "Error while connecting to the RasPi")
            }
        }
    }

    /**
     * 連線至物件追蹤伺服器
     */
    inner class PictureTrackingThread: Thread(){
        // 追蹤伺服器的 IP & Port
        private val trackingServerHost: String = "10.10.11.57"
        private val trackingServerPort: Int = 8000

        override fun run() {
            super.run()
            trackingServerSocket = Socket(trackingServerHost, trackingServerPort)                           // 開始連線至伺服器
            val reader = BufferedReader(InputStreamReader(trackingServerSocket?.getInputStream()))          // 取得輸入流

            var msg = base64CodedPic                                                                        // 取得當前圖片
            trackingServerSocket?.getOutputStream()?.write((msg.length.toString() + "\n").toByteArray())    // 傳送圖片資料總長度
            trackingServerSocket?.getOutputStream()?.write(msg.toByteArray())                               // 傳送圖片

            var ok = reader.readLine()                                                                      // 接收伺服器回傳結果
            while(ok != null){                                                                              // 持續傳送圖片並接收辨識結果
                msg = base64CodedPic
                trackingServerSocket?.getOutputStream()?.write((msg.length.toString() + "\n").toByteArray())
                trackingServerSocket?.getOutputStream()?.write(msg.toByteArray())
                ok = reader.readLine()
                result = JSONTokener(ok).nextValue() as JSONArray
            }
        }
    }

    /**
     * 藍芽連線線程，等待連線成功後便會呼叫 afterSocketConnected 並將連線中的 Socket 傳入
     */
    inner class ConnectThread(private var device : BluetoothDevice) : Thread() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            mSocket?.use{ socket ->
                try{
                    mBluetoothGatt = device.connectGatt( this@ControlPage, false, mGattCallback)
                    socket.connect()
                    afterSocketConnected(socket)
                }catch (e: IOException){
                    if(this.isInterrupted) return
                    else afterSocketConnected(socket)
                }
            }
        }
    }

    /**
     * Handler 負責顯示圖片並畫出預測結果
     */
    @SuppressLint("HandlerLeak")
    @RequiresApi(Build.VERSION_CODES.O)
    inner class MyHandler(activity: ControlPage) : Handler(Looper.getMainLooper()) {
        private var activity: WeakReference<ControlPage>? = null        // 取得要操控的 Activity
        private val v = activityControlPageLandscapeBinding.picView     // 取得顯示圖片的 View
        private val decoder = Base64.getDecoder()                       // Base64 解碼器
        private val p = Paint()                                         // Canvas 筆刷

        init {
            this.activity = WeakReference(activity)

            //筆刷參數設定
            p.strokeWidth = 1f              // 筆刷寬度
            p.color = Color.RED             // 顏色
            p.style = Paint.Style.STROKE    // 風格設定為實心線條
        }

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val byteArrPic = decoder.decode( msg.data.getString("data") )                   // 解碼圖片成 ByteArray
            val pic = BitmapFactory.decodeByteArray( byteArrPic, 0, byteArrPic.count() )   // 透過 BitmapFactory 解碼成 JPEG
            val mutableBitmap = pic.copy(Bitmap.Config.ARGB_8888, true)                 // 複製可修改的 Bitmap 物件
            val rect = Canvas(mutableBitmap)                                                    // 利用 Bitmap 物件創建畫布

            val drawResult = result                             // 最新預測結果
            for (i in 0 until (drawResult?.length() ?: 0)) {    // 將所有結果依序繪出
                // 解析 JSON 物件
                drawResult?.getJSONObject(i)?.let { it ->
                    // 取得 長寬 以及 xy
                    val rec = it.getString("wh").removeSurrounding("[", "]").split(",").map { it.toFloat() }
                    // 因為判斷結果為 810x608 因此座標及長寬需要轉換至 680x480 上
                    rect.drawRect(rec[0]*(680f/810f), rec[1]*(480f/608f), (rec[0]+rec[2])*(680f/810f),  (rec[1]+rec[3])*(480f/608f), p)
                    // 畫出 ID 編號
                    rect.drawText(it.getString("id"), rec[0]*(680f/810f), rec[1]*(480f/608f), p)
                }
            }
            v.setImageBitmap(mutableBitmap) // 顯示結果
        }
    }

    /**
     * 繼承自 TouchListener 判斷按鈕目前是被按下還是放開
     */
    inner class MyTouchListener(private var data: String = ""): View.OnTouchListener{
        override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
            when(p1?.action){
                MotionEvent.ACTION_UP->{
                    if (mSocket?.isConnected != true){
                        finish()
                    }
                    mSocket?.outputStream?.write("stop".toByteArray())
                    mSocket?.outputStream?.flush()
                }
                MotionEvent.ACTION_DOWN->{
                    if (mSocket?.isConnected != true){
                        finish()
                    }
                    mSocket?.outputStream?.write(data.toByteArray())
                    mSocket?.outputStream?.flush()
                }
            }
            p0?.performClick()
            return p0?.onTouchEvent(p1) ?: false
        }
    }
}

