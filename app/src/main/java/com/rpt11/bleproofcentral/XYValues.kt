package com.rpt11.bleproofcentral

class XYValues(
    var x: Double? = null,
    var y: Double? = null
   ){


    fun getX(): Double {
        return x!!
    }

    fun setX(x: Double) {
        this.x = x
    }

    fun getY(): Double {
        return y!!
    }

    fun setY(y: Double) {
        this.y = y
    }
}