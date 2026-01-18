package me.deference.formsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import me.deference.formsample.customer.AddressMetadata
import me.deference.formsample.customer.CustomerFormModelMetadata

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App(CustomerFormModelMetadata(), AddressMetadata())
        }
    }
}