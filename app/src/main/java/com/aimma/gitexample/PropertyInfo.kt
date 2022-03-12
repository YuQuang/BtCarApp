package com.aimma.gitexample

data class PropertyInfo(
    private var propertyName: String = "",
    private var propertyImage: String = "",
    private var propertyNumber: String = "",
    private var propertyProductNumber: String = "",
    private var propertyTip: String = "",
    private var propertyGetDate: String = "",
    private var propertyAgeLimit: String = "",
    private var propertyQuantity: String = "",
    private var propertySingleValue: String = "",
    private var propertyPosition: String = "",
    private var propertyLabelPosition: String = "",
    private var propertyUnit: String = "",
    private var propertyStatus: String = "",
    private var propertyIsCheck: String = "",
) {
    /**
     * Setter
     */
    fun setPropertyName(propertyName: String){
        this.propertyName = propertyName
    }
    fun setPropertyImage(propertyImage: String){
        this.propertyImage = propertyImage
    }
    fun setPropertyNumber(propertyNumber: String){
        this.propertyNumber = propertyNumber
    }

    /**
     * Getter
     */
    fun getPropertyName(): String{
        return this.propertyName
    }
    fun getPropertyImage(): String {
        return this.propertyImage
    }
    fun getPropertyNumber(): String{
        return this.propertyNumber
    }
    fun getPropertyStatus(): String{
        return this.propertyStatus
    }
    fun getPropertyProductNumber(): String{
        return this.propertyProductNumber
    }

}