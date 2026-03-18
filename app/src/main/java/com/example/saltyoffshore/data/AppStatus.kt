package com.example.saltyoffshore.data

sealed class AppStatus {
    data object Idle : AppStatus()
    data object Loading : AppStatus()
    data object ComingSoon : AppStatus()
    data class Error(val message: String) : AppStatus()
}
