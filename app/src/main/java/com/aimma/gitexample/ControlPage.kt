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


class ControlPage : AppCompatActivity() {
    private lateinit var activityControlPageLandscapeBinding: ActivityControlPageLandscapeBinding
    private var loadingDialog: LoadingDialog? = null
    private var mSocket: BluetoothSocket? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var connectThread: Thread? = null
    private var pictureThread: Thread? = null
    private var pictureTrackingThread: Thread? = null
    private var socket: Socket? = null
    private var trackingServerSocket: Socket? = null
    private var text: String = "none"
    private var myUUID: UUID = UUID.fromString("192770fa-4a48-4acd-8b37-f9716c2c7899")
    private val mGattCallback: BluetoothGattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.readRemoteRssi()
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED){}
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS){ }
            else {
                Log.d("GG", "onServicesDiscovered received: $status");
            }
        }
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            Handler(Looper.getMainLooper()).postDelayed({
                activityControlPageLandscapeBinding.statusText.text = "rssi:$rssi \n status: $status"
                mBluetoothGatt?.readRemoteRssi()
            }, 500)
        }
    }


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
     * WIFI連線線程
     */
    @RequiresApi(Build.VERSION_CODES.O)
    inner class PictureThread(private var handler: MyHandler): Thread(){
        private val host: String = "10.10.11.79"
        private val port: Int = 65432

        override fun run() {
            try{
                Log.d("Net", "Starting Connect to Raspi")
                socket = Socket(host, port)
                val input = socket?.getInputStream()
                val reader = BufferedReader(InputStreamReader(input))

                text = reader.readLine()
                Log.d("Net", "Get Data")
                pictureTrackingThread = PictureTrackingThread()
                pictureTrackingThread?.start()
                while (true) {
                    val msg = Message()
                    msg.data.putString("data", text)
                    handler.sendMessage(msg)
                    text = reader.readLine()
                }
            }catch (e: Exception){

            }
        }
    }

    /**
     * 連線至物件追蹤伺服器
     */
    inner class PictureTrackingThread(): Thread(){
        private val trackingServerHost: String = "10.10.11.182"
        private val trackingServerPort: Int = 8000

        override fun run() {
            super.run()
            Log.d("Net", "Starting Connect to TrackingServer")
            trackingServerSocket = Socket(trackingServerHost, trackingServerPort)
            val reader = BufferedReader(InputStreamReader(trackingServerSocket?.getInputStream()))

            var msg = text
            Log.d("Net", "Text Len = " + msg.length.toString())
            trackingServerSocket?.getOutputStream()?.write((msg.length.toString() + "\n").toByteArray())
            trackingServerSocket?.getOutputStream()?.write(msg.toByteArray())
            Log.d("Net", "Send to TrackingServer")
            var ok = reader.readLine()
            Log.d("Net", "Received from TrackingServer")
            while(ok != null){
                msg = text
                trackingServerSocket?.getOutputStream()?.write((msg.length.toString() + "\n").toByteArray())
                trackingServerSocket?.getOutputStream()?.write(msg.toByteArray())
                ok = reader.readLine()
            }
        }
    }

    /**
     * 藍芽連線線程，等待連線成功後便會呼叫 afterSocketConnected 並將連線中的 Socket 傳入
     */
    private inner class ConnectThread(var device : BluetoothDevice) : Thread() {
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
     * Handler 負責顯示圖片
     */
    @SuppressLint("HandlerLeak")
    @RequiresApi(Build.VERSION_CODES.O)
    inner class MyHandler(activity: ControlPage) : Handler(Looper.getMainLooper()) {
        private var activity: WeakReference<ControlPage>? = null
        private val v = activityControlPageLandscapeBinding.picView
        private val decoder = Base64.getDecoder()
        private val p = Paint()


        init {
            this.activity = WeakReference(activity)
            p.strokeWidth = 2f
            p.color = Color.RED
            p.style = Paint.Style.STROKE
        }

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val text = msg.data.getString("data")
            val code = decoder.decode(text)
            val pic = BitmapFactory.decodeByteArray(code, 0, code.count())
            val mutableBitmap = pic.copy(Bitmap.Config.ARGB_8888, true)
            val rect = Canvas(mutableBitmap)
            rect.drawRect(10f, 10f, 50f, 50f, p)
            v.setImageBitmap(mutableBitmap)
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

