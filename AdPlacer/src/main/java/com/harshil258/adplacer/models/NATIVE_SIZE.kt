package com.harshil258.adplacer.models

enum class NATIVE_SIZE {
    SMALL, MEDIUM, LARGE
}


enum class SCREENS{
    HOME,START,EXTRA_START,
}

enum class ADTYPE{
    BANNER,NATIVE
}


enum class TYPE_OF_RESPONSE(var value: String) {
    GOOGLE("APPDETAILS"),API("")
}