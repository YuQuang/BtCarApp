package com.aimma.gitexample

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import com.aimma.gitexample.databinding.DialogLoadingBinding

class LoadingDialog(activity: Activity) {
    private var alertDialog: AlertDialog? = null
    var dialogLoadingBinding: DialogLoadingBinding

    init {
        val builder =  AlertDialog.Builder(activity)
        dialogLoadingBinding = DialogLoadingBinding.inflate(activity.layoutInflater)
        val v = dialogLoadingBinding.root
        builder.setView(v)
        alertDialog = builder.create()
        alertDialog?.setCanceledOnTouchOutside(false)
    }

    fun startLoadingDialog(){
        if (alertDialog?.isShowing == false){
            alertDialog?.show()
        }
    }

    fun dismiss(){
        alertDialog?.dismiss()
    }

    fun setOnCancelListener(listener: DialogInterface.OnCancelListener?){
        this.alertDialog?.setOnCancelListener(listener)
    }
}