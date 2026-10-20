package com.example.rp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.rp.ui.RecetasApp
import com.example.rp.ui.theme.RPTheme

/**
 * Puerta de entrada de la aplicación (equivalente a `int main()` en C++
 * o `if __name__ == "__main__":` en Python).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RPTheme {
                RecetasApp()
            }
        }
    }
}