package com.example.rp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.rp.ui.RecetasApp
import com.example.rp.ui.theme.RPTheme

/**
 * ============================================================================
 * ARCHIVO: MainActivity.kt
 * ============================================================================
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Es el punto de entrada principal de tu app Android (como `int main()` en C++
 * o `if __name__ == "__main__":` en Python).
 *
 * Cuando tocas el ícono de la app en tu celular, Android crea esta `MainActivity`
 * y ejecuta su función `onCreate`. Dentro de `setContent { ... }` llamamos a `RecetasApp()`,
 * que dibuja toda la interfaz gráfica definida en `ui/RecetasApp.kt`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Permite que la app dibuje de borde a borde de la pantalla moderna de Android
        enableEdgeToEdge()
        setContent {
            // Aplica los colores y tipografía del tema de tu proyecto y arranca la pantalla principal
            RPTheme {
                RecetasApp()
            }
        }
    }
}
