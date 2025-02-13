package com.h4.dao

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.h4.dao.services.ApiService
import com.h4.dao.ui.theme.DAOTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var apiService: ApiService = ApiService()

    private var deliveries: MutableStateFlow<List<Delivery>> = MutableStateFlow(
        listOf()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadDeliveriesFromApi()

        enableEdgeToEdge()
        setContent {
            DAOTheme {
                ScaffoldSetup()
            }
        }
    }

    private fun loadDeliveriesFromApi() {
        lifecycleScope.launch {
            try {
                val newPackages = apiService.getDeliveries()
                if (newPackages != null) {
                    deliveries.value = newPackages
                } else {
                    deliveries.value = listOf()
                }

                Log.d("UwU", "Fetched packages: $newPackages")
            } catch (e: Exception) {
                Log.e("UwU", "Failed to fetch packages", e)
                deliveries.value = listOf()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScaffoldSetup() {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(text = "DAO Registration app")
                    }
                )
            },
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
                    Row {
                        val packages by deliveries.collectAsState()
                        DeliveryList(packages)
                    }
                }
            }
        }
    }

    @Composable
    fun DeliveryList(deliveries: List<Delivery>) {
        Column {
            Text("Deliveries", fontSize = 24.sp)
            HorizontalDivider()
            if (deliveries.isEmpty()) {
                Text("No deliveries found")
            }
            LazyColumn() {
                items(deliveries) { delivery ->
                    DeliveryListItem(delivery)
                    HorizontalDivider()
                }
            }
        }
    }

    @Composable
    fun DeliveryListItem(delivery: Delivery) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = delivery.barcode.toString(), fontWeight = FontWeight.SemiBold)
            Text(text = delivery.shopName)
            Text(text = delivery.shopAddress, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis, maxLines = 1)
            VerticalDivider()
            TextButton(onClick = {
                // Go to scanning activity
                val intent = Intent(this@MainActivity, ScanningActivity::class.java)
                intent.putExtra("shopName", delivery.shopName)
                startActivity(intent)
            }) {
                Text("Deliver")
            }
        }
    }

    @Preview(showBackground = true, name = "DeliveryList")
    @Composable
    fun DeliveryListPreview() {
        DAOTheme {
            DeliveryList(
                listOf(
                    Delivery(
                        barcode = 123456,
                        createdDate = "2023-01-01",
                        deliveryTime = "2023-01-02 10:00",
                        deliveryStatus = "Delivered",
                        senderName = "Shop A",
                        shopName = "Sender A",
                        senderAddress = "123 Sender St",
                        shopAddress = "456 Shop St",
                        senderZipcode = "12345",
                        shopZipcode = "67890",
                        senderPhone = "123-456-7890",
                        shopPhone = "098-765-4321",
                        senderEmail = "sender@example.com",
                        shopEmail = "shop@example.com"
                    ),
                    Delivery(
                        barcode = 789012,
                        createdDate = "2023-02-01",
                        deliveryTime = "2023-02-02 11:00",
                        deliveryStatus = "In Transit",
                        senderName = "Shop B",
                        shopName = "Sender B",
                        senderAddress = "789 Sender St",
                        shopAddress = "012 Shop St",
                        senderZipcode = "54321",
                        shopZipcode = "09876",
                        senderPhone = "321-654-0987",
                        shopPhone = "876-543-2109",
                        senderEmail = "senderb@example.com",
                        shopEmail = "shopb@example.com"
                    ),
                    Delivery(
                        barcode = 345678,
                        createdDate = "2023-03-01",
                        deliveryTime = "2023-03-02 12:00",
                        deliveryStatus = "Pending",
                        senderName = "Shop C",
                        shopName = "Sender C",
                        senderAddress = "345 Sender St",
                        shopAddress = "678 Shop St",
                        senderZipcode = "67890",
                        shopZipcode = "12345",
                        senderPhone = "456-789-0123",
                        shopPhone = "210-987-6543",
                        senderEmail = "senderc@example.com",
                        shopEmail = "shopc@example.com"
                    )
                )
            )
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