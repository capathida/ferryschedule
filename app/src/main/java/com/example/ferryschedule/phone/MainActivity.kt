package com.example.ferryschedule.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.ferryschedule.phone.ui.DeparturesPhoneScreen
import com.example.ferryschedule.phone.ui.theme.FerryTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DeparturesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FerryTheme {
                DeparturesPhoneScreen(viewModel = viewModel)
            }
        }
    }
}
