package com.aimma.gitexample

data class BtInfo(var deviceName: String = "None", var deviceAddress: String = "None") {

    fun setBtDeviceName(name: String){
        deviceName = name
    }
    fun setBtDeviceAddress(address: String){
        deviceAddress = address
    }

    fun getBtDeviceName(): String{
        return deviceName
    }
    fun getBtDeviceAddress(): String{
        return deviceAddress
    }
}