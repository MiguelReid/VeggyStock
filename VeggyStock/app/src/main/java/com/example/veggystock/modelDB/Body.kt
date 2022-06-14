package com.example.veggystock.modelDB

class Body {

    /**
     * La estructura que realtime database
     * va a seguir
     */

    constructor()
    constructor(
        nm: String,
        pvd: String,
        prc: Float,
        adrs: String,
        rtng: Float,
        fav: Boolean,
        vgn: Boolean
    ) {
        name = nm
        provider = pvd
        price = prc
        address = adrs
        rating = rtng
        favourite = fav
        vegan = vgn
    }

    var name: String = ""
    var provider: String = ""
    var price = 0.0f
    var address = ""
    var rating = 0.0f
    var favourite = false
    var vegan = false
}