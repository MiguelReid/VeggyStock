package com.example.veggystock.modelDB

class Body {

    /*
    The structure that realtime
    database is going to follow
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