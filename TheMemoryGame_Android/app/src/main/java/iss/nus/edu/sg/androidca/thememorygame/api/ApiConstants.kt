package iss.nus.edu.sg.androidca.thememorygame.api

object ApiConstants {
    const val HOST = "http://10.0.2.2:5178"
    const val BASE = "${HOST}/api"

    const val LOGIN = "$BASE/login/login"
    const val SAVE_TIME = "$BASE/home/save"
    const val TOP_FIVE = "$BASE/home/top5"
    const val FIND_RANK = "$BASE/home/rank"
    const val USERNAME = "$BASE/home/me"

    const val CAN_SEE_ADS = "$BASE/home/user/can-see-ads"
}