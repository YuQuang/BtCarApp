package com.aimma.gitexample

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aimma.gitexample.databinding.PropertyListItemBinding
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.URL
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.*
import java.lang.Exception
import java.security.SecureRandom
import java.security.cert.X509Certificate


class PropertyAdapter(private val parent: Website): RecyclerView.Adapter<PropertyAdapter.ViewHolder>() {
    private var session: String = ""
    private lateinit var dataList: ArrayList<PropertyInfo>
    private lateinit var cellClickListener: CellClickListener

    interface CellClickListener{
        fun onCellClickListener(data: PropertyInfo, position: Int)
    }

    fun setCellClickListener(cellClickListener: CellClickListener){
        this.cellClickListener = cellClickListener
    }

    inner class ViewHolder(propertyListItemBinding: PropertyListItemBinding): RecyclerView.ViewHolder(propertyListItemBinding.root){
        private val propertyName = propertyListItemBinding.propertyName
        private val propertyNumber = propertyListItemBinding.propertyNumber
        private val propertyProductNumber = propertyListItemBinding.propertyProductNumber
        private val cardview = propertyListItemBinding.root
        val propertyImage = propertyListItemBinding.propertyImage
        fun bind(data: PropertyInfo){
            propertyName.text = data.getPropertyName()
            propertyNumber.text = data.getPropertyNumber()
            propertyProductNumber.text = data.getPropertyProductNumber()
            if (data.getPropertyStatus() == "o") {
                cardview.setCardBackgroundColor(parent.resources.getColor(R.color.red30))
            }else{
                cardview.setCardBackgroundColor(parent.resources.getColor(R.color.white40))
            }
            cardview.isClickable = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyAdapter.ViewHolder {
        val propertyListItemBinding = PropertyListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(propertyListItemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])

        holder.itemView.setOnClickListener {
            cellClickListener.onCellClickListener(dataList[position], position)
        }

        GlideApp.with(this.parent.context!!)
            .load(dataList[position].getPropertyImage())
            .apply(RequestOptions().transform(FitCenter(), RoundedCorners(40)))
            .into(holder.propertyImage)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun updateList(dataList: java.util.ArrayList<PropertyInfo>){
        this.dataList = dataList
    }

    fun setSession(session: String){
        this.session = session
    }

    private fun webBitmapAPI(APIUrl: String ,session: String): Bitmap?{
        // 設定連線網址以及連線設定
        val url = URL(APIUrl)
        val connect: HttpsURLConnection = url.openConnection() as HttpsURLConnection

        // 憑證設定並導入憑證
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val caInput: InputStream = BufferedInputStream(parent.resources.openRawResource(R.raw.code))
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
        connect.useCaches = false
        connect.connectTimeout = 5000
        connect.readTimeout = 5000
        connect.setRequestProperty("Cookie", "sessionid=$session")
        connect.connect()


        // 若連線不成功則返回
        if(connect.responseCode == 403){
            Log.e("GG", "API Error code 403")
            return null
        }else if(connect.responseCode != 200) return null

        val inputStream = connect.inputStream

        return BitmapFactory.decodeStream(inputStream)
    }
    inner class AllowAllHostNameVerifier : HostnameVerifier {
        override fun verify(p0: String?, p1: SSLSession?): Boolean {
            return true
        }
    }

    /**
     * 忽略https的证书校验
     * 避免Glide加载https图片报错：
     * javax.net.ssl.SSLHandshakeException: java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.
     */
    fun handleSSLHandshake() {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate?> {
                    return arrayOfNulls<X509Certificate>(0)
                }
                override fun checkClientTrusted(
                    p0: Array<out X509Certificate>?,
                    authType: String?
                ) {}
                override fun checkServerTrusted(
                    p0: Array<out X509Certificate>?,
                    authType: String?
                ) {}
            })
            val sc = SSLContext.getInstance("TLS")
            // trustAllCerts信任所有的证书
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { hostname, session -> true }
        } catch (ignored: Exception) {
        }
    }
}