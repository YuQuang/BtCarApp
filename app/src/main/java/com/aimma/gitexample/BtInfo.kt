package com.aimma.gitexample

data class BtInfo(var deviceName: String = "None", var deviceAddress: String = "None", var deviceRRSI: Int) {

    fun getBtDeviceName(): String{
        return deviceName
    }
    fun getBtDeviceAddress(): String{
        return deviceAddress
    }
    fun getBtDeviceRRSI(): Int{
        return deviceRRSI
    }
}