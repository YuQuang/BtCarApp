package com.aimma.gitexample

import android.content.Intent
import android.os.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.aimma.gitexample.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar
import java.io.*
import java.lang.StringBuilder
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.*

class LoginPage: AppCompatActivity()
{
    private lateinit var activityLoginBinding: ActivityLoginBinding
    private val loginPageHandler = LoginPageHandler(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityLoginBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(activityLoginBinding.root)

        activityLoginBinding.loginPageLoadingBar.visibility = View.INVISIBLE
        activityLoginBinding.loginPageLoginBtn.setOnClickListener(loginListener)
        activityLoginBinding.loginPageBackBtn.setOnClickListener(backListener)

    }

    /**
     * 上一頁按鈕
     */
    override fun onBackPressed() {
        super.onBackPressed()
        setResult(RESULT_CANCELED)
        finish()
    }

    /**
     * 登入按鈕的監聽
     */
    private val loginListener = View.OnClickListener {
        Thread {
            val account = activityLoginBinding.editTextTextPersonName.text.toString()
            val password = activityLoginBinding.editTextTextPassword.text.toString()
            try{
                // 顯示進度旋轉圖案
                val msg = Message()
                msg.data.putString("Action", "LoginPageVisible")
                msg.data.putString("Visible", "True")
                loginPageHandler.sendMessage(msg)

                // 設定連線網址以及連線設定
                val url = URL( Namespace.webAPIUrl + "/appLogin/")
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
                val dataOutputStream = DataOutputStream(connect.outputStream)
                val payload = StringBuilder()
                payload.append("username=").append(URLEncoder.encode(account, "UTF-8")).append("&")
                payload.append("password=").append(URLEncoder.encode(password, "UTF-8")).append("&")
                dataOutputStream.writeBytes(payload.toString())
                dataOutputStream.flush()
                dataOutputStream.close()
                connect.connect()

                // 若連線不成功則返回
                if(connect.responseCode != 200) return@Thread

                // 擷取 SessionID
                val responseSetCookie = connect.getHeaderField("Set-Cookie")
                var session = String()
                if(responseSetCookie != null) {
                    for (t in responseSetCookie.removeRange(0, 10)) {
                        if (t == ';') break
                        session += t
                    }
                    setResult(RESULT_OK, Intent().putExtra("session", session))
                    finish()
                }else{
                    Snackbar.make(this, activityLoginBinding.root
                        , "Login Failed", Snackbar.LENGTH_SHORT).show()
                }
            }catch(e: SocketTimeoutException){
                // 超時顯示
                Snackbar.make(this, activityLoginBinding.root
                    , "Connection Timeout...", Snackbar.LENGTH_SHORT).show()
            }finally {
                // 隱藏進度旋轉圖案
                val msg = Message()
                msg.data.putString("Action", "LoginPageVisible")
                msg.data.putString("Visible", "False")
                loginPageHandler.sendMessage(msg)
            }
        }.start()
    }

    /**
     * 返回按鈕
     */
    private val backListener = View.OnClickListener {
        setResult(RESULT_CANCELED)
        finish()
    }

    /**
     * 處理連線時的畫面顯示
     */
    inner class LoginPageHandler(activity: LoginPage) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.data.getString("Action")){
                "LoginPageVisible" ->{
                    if(msg.data.getString("Visible") == "True") {
                        activityLoginBinding.loginPageLoadingBar.visibility = View.VISIBLE
                        activityLoginBinding.loginPageLoginBtn.isEnabled = false
                    }else {
                        activityLoginBinding.loginPageLoadingBar.visibility = View.INVISIBLE
                        activityLoginBinding.loginPageLoginBtn.isEnabled = true
                    }
                }
            }
        }
    }

    /**
     * 憑證處理
     */
    inner class AllowAllHostNameVerifier : HostnameVerifier {
        override fun verify(p0: String?, p1: SSLSession?): Boolean {
            return true;
        }
    }
}