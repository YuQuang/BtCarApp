package com.aimma.gitexample

import android.annotation.SuppressLint
import androidx.annotation.RequiresApi
import android.graphics.*
import android.os.*
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.aimma.gitexample.databinding.ActivityWifiControlPageBinding
import org.json.JSONArray
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.ref.WeakReference
import java.net.Socket
import java.util.*

class WiFiControlPage : AppCompatActivity() {
    private var loadingDialog: LoadingDialog? = null                                         // 嘗試連線時顯示的 LoadingDialog
    private lateinit var activityWifiControlPageBinding: ActivityWifiControlPageBinding
    private var wifiSocket: Socket? = null
    private var trackingServerSocket: Socket? = null
    private lateinit var wifiThread: WifiThread
    private var pictureTrackingThread: PictureTrackingThread? = null
    private var base64CodedPic: String = "none"                                               // 樹梅派傳來編碼過的圖片 (JPEG)
    private var result: JSONArray? = null                                                     // 物件追蹤判斷結果

    override fun onDestroy(){
        super.onDestroy()
        wifiThread.interrupt()
        wifiSocket?.close()
        pictureTrackingThread?.interrupt()
        trackingServerSocket?.close()
    }

    // 若按返回則取消連線，並返回上一頁
    override fun onBackPressed() {
        super.onBackPressed()
        if(wifiThread.isAlive){
            wifiThread.interrupt()
            loadingDialog?.dismiss()
            finish()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityWifiControlPageBinding =
            ActivityWifiControlPageBinding.inflate(layoutInflater)
        activityWifiControlPageBinding.root.apply {
            setContentView(this)
        }

        /**
         * 設定對話框類型
         * 依照設定創建對話框類
         */
        loadingDialog = LoadingDialog(this)
        loadingDialog?.dialogLoadingBinding?.button?.setOnClickListener { onBackPressed() }
        loadingDialog?.setOnCancelListener { onBackPressed() }
        loadingDialog?.startLoadingDialog()


        activityWifiControlPageBinding.backBtn.setOnTouchListener(BtnTouchListener("back"))
        activityWifiControlPageBinding.forwardBtn.setOnTouchListener(BtnTouchListener("forward"))
        activityWifiControlPageBinding.leftBtn.setOnTouchListener(BtnTouchListener("left"))
        activityWifiControlPageBinding.rightBtn.setOnTouchListener(BtnTouchListener("right"))

        wifiThread  = WifiThread(intent.getStringExtra("deviceIP").toString(), 8000, MyHandler(this))
        wifiThread.start()
    }

    /**
     * 與樹梅派連線
     */
    inner class WifiThread(private var ip: String, private var port: Int, private var handler: MyHandler): Thread() {
        override fun run(){
            try{
                wifiSocket = Socket(ip, port)                           // 嘗試連線至樹梅派
                loadingDialog?.dismiss()
                val input = wifiSocket?.getInputStream()                // 取得輸入流
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
     * 處理辨識結果與圖片顯示
     */
    @SuppressLint("HandlerLeak")
    @RequiresApi(Build.VERSION_CODES.O)
    inner class MyHandler(activity: WiFiControlPage) : Handler(Looper.getMainLooper()) {
        private var activity: WeakReference<WiFiControlPage>? = null    // 取得要操控的 Activity
        private val v = activityWifiControlPageBinding.picView          // 取得顯示圖片的 View
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
    inner class BtnTouchListener(private var command: String = ""): View.OnTouchListener{
        override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
            when(p1?.action){
                MotionEvent.ACTION_UP->{
//                    wifiSocket.outputStream?.write("stop".toByteArray())
//                    wifiSocket.outputStream?.flush()
                    Log.i("GG", "Stop")
                    p0?.performClick()
                }
                MotionEvent.ACTION_DOWN->{
//                    wifiSocket.outputStream?.write(command.toByteArray())
//                    wifiSocket.outputStream?.flush()
                    Log.i("GG", command)
                }
            }
            return p0?.onTouchEvent(p1) ?: false
        }
    }
}