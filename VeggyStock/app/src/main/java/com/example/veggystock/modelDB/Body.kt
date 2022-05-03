package com.example.veggystock.modelDB

class Body {
    constructor()
    constructor(nm: String, pvd: String, prc: Float, adrs: String, rtng: Float, fav: Boolean) {
        name = nm
        provider = pvd
        price = prc
        address = adrs
        rating = rtng
        favorite = fav
    }

    var name: String = ""
    var provider: String = ""
    var price = 0.0f
    var address = ""
    var rating = 0.0f
    var favorite = false
}