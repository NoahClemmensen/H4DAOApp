package com.h4.dao

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.h4.dao.services.ApiService
import com.h4.dao.ui.theme.DAOTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var apiService: ApiService = ApiService("http://172.27.232.4:3000/")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            DAOTheme {
                ScaffoldSetup()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScaffoldSetup() {
        var presses by remember { mutableIntStateOf(0) }

        Scaffold(
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(text = "Top app bar")
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "Bottom app bar",
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    val intent = Intent(this, ScanningActivity::class.java)
                    startActivity(intent)
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "To scan")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text(text =
                    """
                    This is an example of a scaffold. It uses the Scaffold composable's parameters to create a screen with a simple top app bar, bottom app bar, and floating action button.

                    It also contains some basic inner content, such as this text.

                    You have pressed the floating action button $presses times.
                    """.trimIndent()
                    )

                    Button(onClick = {
                        lifecycleScope.launch {
                            try {
                                apiService.makeWebhookGetCall()

                                val data = Package(
                                    id = 1,
                                    shopId = 1,
                                    senderId = 1,
                                    createdDate = 1738915166,
                                    deliveryTime = 1741330766,
                                    deliveryStatus = "pending"
                                )
                                apiService.makeWebhookPostCall(data)


                                apiService.makeApiCall()
                            } catch (e: Exception) {
                                println("Error: ${e.message}")
                            }
                        }
                        presses++
                    }) {
                        Text("Press me")
                    }

                    Button(onClick = {
                        lifecycleScope.launch {
                            try {
                                val pendingPackages = apiService.getPendingPackages()
                                Log.d("MainActivity", "Pending packages: $pendingPackages")

                            } catch (e: Exception) {
                                println("Error: ${e.message}")
                            }
                        }
                        presses++
                    }) {
                        Text("Get pending packages")
                    }
                }
            }
        }
    }

    @Preview(showBackground = true, name = "scaffold")
    @Composable
    fun ScaffoldPreview() {
        DAOTheme {
            ScaffoldSetup()
        }
    }
}