package com.h4.dao

import android.annotation.SuppressLint
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.content.IntentFilter
import android.os.Build
import com.google.mlkit.vision.barcode.common.Barcode
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.common.util.concurrent.ListenableFuture
import com.h4.dao.services.ApiService
import com.h4.dao.services.CameraService
import com.h4.dao.ui.theme.DAOTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ScanningActivity : ComponentActivity() {
    private var camService: CameraService = CameraService()
    private var apiService: ApiService = ApiService()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var showBottomSheet = mutableStateOf(false)
    private var openCreatePackageDialog = mutableStateOf(false)
    private var openConfirmationDialog = mutableStateOf(false)

    private var shop: String = ""

    private var pendingPackages: MutableStateFlow<List<PendingPackage>> = MutableStateFlow(
        listOf()
    )
    private var scannedPackages: MutableStateFlow<List<PendingPackage>> = MutableStateFlow(
        listOf()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var errorDialog by mutableStateOf(false)
        var errorTitle by mutableStateOf("")
        var errorText by mutableStateOf("")

        val shopName = intent.getStringExtra("shopName")

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }

        getlastLocation()

        if (shopName == null) {
            errorDialog = true
            errorTitle = "Error"
            errorText = "Shop name not provided"
        } else if () {
            shop = shopName
            loadPackagesFromApi(shop)
        }

        enableEdgeToEdge()
        setContent {
            DAOTheme {
                if (errorDialog) {
                    PopupDialog(
                        onDismissRequest = { errorDialog = false },
                        onConfirmation = {
                            finish()
                        },
                        title = errorTitle,
                        text = errorText,
                        showDismiss = false
                    )
                } else {
                    ScaffoldSetup()
                }
            }
        }
    }

    private fun getlastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("ScanningActivityLocation", "Location: ${location.latitude}, ${location.longitude}")
                    calculateDistance(location.latitude.toDouble(), location.longitude.toDouble())
                } else {
                    Log.d("ScanningActivityLocation", "Location is null")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ScanningActivityLocation", "Failed to get location", e)
            }
    }

    private fun calculateDistance(lat: Double, long: Double){
        val results = FloatArray(1)

        val distance = android.location.Location.distanceBetween(55.396, 10.388, lat.toDouble(), long.toDouble(), results)
        Log.d("ScanningActivityLocation", "Distance: ${results[0]}")
    }


    private fun loadPackagesFromApi(shopName: String) {
        lifecycleScope.launch {
            try {
                val packages = apiService.getPendingPackages(shopName)
                if (packages != null) {
                    this@ScanningActivity.pendingPackages.value = packages
                } else {
                    this@ScanningActivity.pendingPackages.value = listOf()
                }

                Log.d("UwU", "Fetched pending packages: $packages")
            } catch (e: Exception) {
                Log.e("UwU", "Failed to fetch pending packages", e)
                pendingPackages.value = listOf()
            }
        }
    }

    private fun onScanPackage(barcode: Int) {
        pendingPackages.value.forEach() {
            if (it.barcode == barcode) {
                pendingPackages.value = pendingPackages.value.filter { it.barcode != barcode }
                scannedPackages.value += it
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScaffoldSetup() {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(text = "Scan packages")
                    },
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            finish()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    actions = {
                        IconButton(onClick = {
                            openConfirmationDialog.value = true
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = "Mark scanned packages as delivered")
                        }
                        IconButton(onClick = {
                            showBottomSheet.value = true
                        }) {
                            Icon(
                                Icons.Outlined.Menu,
                                contentDescription = "List pending packages",
                            )
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { openCreatePackageDialog.value = true },
                        ) {
                            Icon(Icons.Outlined.LibraryAdd, "Add")
                        }
                    }
                )
            }
        ) { innerPadding ->
            /* Bottom sheet */
            PartialBottomSheet()

            /* Alert dialog */
            when {
                openConfirmationDialog.value -> {
                    ConfirmationDialog(
                        onDismissRequest = { openConfirmationDialog.value = false },
                        onConfirmation = {
                            openConfirmationDialog.value = false
                            lifecycleScope.launch {
                                try {
                                    val response = apiService.registerPackages(
                                        scannedPackages.value.map { it.barcode }
                                    )

                                    if (response != null && response) {
                                        scannedPackages.value = listOf()
                                        loadPackagesFromApi(shop)
                                    }

                                    Log.d("UwU", "Marked packages as delivered: $response")
                                } catch (e: Exception) {
                                    Log.e(
                                        "UwU",
                                        "Failed to mark packages as delivered",
                                        e
                                    )
                                }
                            }
                        },
                    )
                }
                openCreatePackageDialog.value -> {
                    CreatePackageDialog(
                        onDismissRequest = { openCreatePackageDialog.value = false },
                        onConfirmation = { barcode ->
                            openCreatePackageDialog.value = false
                            onScanPackage(barcode)
                        },
                    )
                }
            }

            /* Scaffold content */
            Column(
                modifier = Modifier
                    .padding(innerPadding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                /* Camera preview */
                CameraView()
            }
        }
    }

    @Composable
    fun ConfirmationDialog(
        onDismissRequest: () -> Unit,
        onConfirmation: () -> Unit,
    ) {
        PopupDialog(
            onDismissRequest = onDismissRequest,
            onConfirmation = onConfirmation,
            title = "Mark scanned packages as delivered",
            text = "Do you want to confirm these packages delivered?"
        )
    }

    @Composable
    fun PopupDialog(
        onDismissRequest: () -> Unit,
        onConfirmation: () -> Unit,
        title: String,
        text: String,
        showDismiss: Boolean = true,
    ) {
        if (showDismiss) {
            AlertDialog(
                title = {
                    Text(title)
                },
                text = {
                    Text(text)
                },
                onDismissRequest = {
                    onDismissRequest()
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onConfirmation()
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        } else {
            AlertDialog(
                title = {
                    Text(title)
                },
                text = {
                    Text(text)
                },
                onDismissRequest = {
                    onDismissRequest()
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onConfirmation()
                        }
                    ) {
                        Text("Confirm")
                    }
                },
            )
        }
    }

    @Composable
    fun CreatePackageDialog(
        onDismissRequest: () -> Unit,
        onConfirmation: (Int) -> Unit,
    ) {
        var packageId by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        Dialog(
            onDismissRequest = { onDismissRequest() },
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Outlined.LibraryAdd, "Add")
                    Text(
                        "Create package",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 24.sp
                    )
                    Text(
                        text = "Manually add a package to the system, by entering the package ID.",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 14.sp
                    )
                    TextField(
                        value = packageId,
                        onValueChange = { packageId = it },
                        label = { Text("Enter package ID") },
                        placeholder = { Text("E.g 863127390") },
                        isError = isError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = { onDismissRequest() }
                        ) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                if (packageId.isEmpty()) {
                                    isError = true
                                } else {
                                    onConfirmation(packageId.toIntOrNull()?:-1)
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun PackageDetails(
        pack: PendingPackage,
        onDismissRequest: () -> Unit
    ) {
        Dialog(
            onDismissRequest = { onDismissRequest() },
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Outlined.Info, "Info")
                    Text(
                        pack.shopName,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 24.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Barcode: ${pack.barcode}",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Address: ${pack.shopAddress}",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Zip code: ${pack.shopZipcode}",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Email: ${pack.shopEmail}",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Phone: ${pack.shopPhone}",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = { onDismissRequest() }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PartialBottomSheet() {

        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        )

        val packages by pendingPackages.collectAsState()
        val scanned by scannedPackages.collectAsState()

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showBottomSheet.value) {
                ModalBottomSheet(
                    modifier = Modifier.fillMaxHeight(),
                    sheetState = sheetState,
                    onDismissRequest = { showBottomSheet.value = false }
                ) {
                    PackageList(packages, scanned, Modifier.fillMaxSize().padding(16.dp))
                }
            }
        }
    }

    @Composable
    fun PackageList(packages: List<PendingPackage>, scannedPackages: List<PendingPackage>, modifier: Modifier = Modifier) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text("Pending", fontSize = 24.sp)
                HorizontalDivider()
                if (packages.isEmpty()) {
                    Text("No pending packages")
                }
                LazyColumn() {
                    items(packages) { pack ->
                        PackageListItem(pack)
                        HorizontalDivider()
                    }
                }
            }

            Column {
                Text("Scanned packages", fontSize = 24.sp)
                HorizontalDivider()
                if (scannedPackages.isEmpty()) {
                    Text("No packages scanned")
                }
                LazyColumn() {
                    items(scannedPackages) { pack ->
                        PackageListItem(pack)
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    @Composable
    fun PackageListItem(pack: PendingPackage) {
        var detailsOpened by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = pack.barcode.toString(), fontWeight = FontWeight.SemiBold)
            Text(text = pack.shopName)
            Text(text = pack.shopAddress, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis, maxLines = 1)
            VerticalDivider()
            TextButton(onClick = {
                detailsOpened = true
            }) {
                Text("Details")
            }
        }

        if (detailsOpened) {
            PackageDetails(pack) {
                detailsOpened = false
            }
        }
    }

    @Composable
    fun CameraView() {
        var isPermissionGranted by remember { mutableStateOf(false) }
        var requestPermission by remember { mutableStateOf(false) }
        var isLocationPermissionGranted by remember { mutableStateOf(false) }
        var requestLocationPermission by remember { mutableStateOf(false) }

        val context = LocalContext.current

        LaunchedEffect(Unit) {
            isPermissionGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            isLocationPermissionGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (requestPermission) {
            RequestCameraPermission { isGranted ->
                isPermissionGranted = isGranted
                requestPermission = false
            }
        }

        if(requestLocationPermission) {
            RequestLocationPermission { isGranted ->
                isLocationPermissionGranted = isGranted
                requestLocationPermission = false
            }
        }

        when {
            !isPermissionGranted -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = {
                    requestPermission = true
                }) {
                    Text("Request Camera Permission")
                }
            }
            !isLocationPermissionGranted -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = {
                    requestLocationPermission = true
                }) {
                    Text("Request Location Permission")
                }
            }
            else -> CameraPreview(camService.getCameraProviderFuture(context))
        }
    }

    @Composable
    fun CameraPreview(cameraProviderFuture: ListenableFuture<ProcessCameraProvider>) {
        val lifecycleOwner = LocalLifecycleOwner.current
        var showDialog by remember { mutableStateOf(false) }
        var detectedBarcode by remember { mutableStateOf<Barcode?>(null) }
        var isScanning by remember { mutableStateOf(true) }

        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    setBackgroundColor(Color.GREEN)
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    scaleType = PreviewView.ScaleType.FILL_START
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    post {
                        cameraProviderFuture.addListener(Runnable {
                            val cameraProvider = cameraProviderFuture.get()
                            camService.bindPreview(
                                cameraProvider,
                                lifecycleOwner,
                                this,
                            ) { barcodes ->
                                if (isScanning) {
                                    // Handle detected barcodes
                                    barcodes.forEach { barcode ->
                                        detectedBarcode = barcode
                                        showDialog = true
                                        isScanning = false
                                    }
                                }
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                }
            }
        )

        if (showDialog && detectedBarcode != null) {
            BarcodeDetailsDialog(
                barcode = detectedBarcode!!.rawValue!!,
                onDismissRequest = {
                    showDialog = false
                    isScanning = true
                },
                onAdd = {
                    // Handle add action
                    showDialog = false
                    isScanning = true

                    if (detectedBarcode!!.rawValue != null && detectedBarcode!!.rawValue!!.toIntOrNull() != null) {
                        onScanPackage(detectedBarcode!!.rawValue!!.toInt())
                    }
                },
                onDontAdd = {
                    // Handle don't add action
                    showDialog = false
                    isScanning = true
                }
            )
        }
    }

    @Composable
    fun BarcodeDetailsDialog(
        barcode: String,
        onDismissRequest: () -> Unit,
        onAdd: () -> Unit,
        onDontAdd: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { onDismissRequest() },
            title = { Text("Barcode Details") },
            text = { Text("Detected barcode: $barcode") },
            confirmButton = {
                TextButton(onClick = { onAdd() }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { onDontAdd() }) {
                    Text("Don't add")
                }
            }
        )
    }

    @Composable
    fun RequestCameraPermission(
        onResult: (Boolean) -> Unit,
    ) {
        val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            onResult(isGranted)
        }

        LaunchedEffect(Unit) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    }

    @Composable
    fun RequestLocationPermission(
        onResult: (Boolean) -> Unit,
    ) {
        val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            onResult(isGranted)
        }
        LaunchedEffect(Unit) {
            launcher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @Preview(showBackground = true, name = "Barcode details dialog")
    @Composable
    fun BarcodeDetailsDialogPreview() {
        DAOTheme {
            BarcodeDetailsDialog(
                "123456789",
                onDismissRequest = {},
                onAdd = {},
                onDontAdd = {},
            )
        }
    }

    @Preview(showBackground = true, name = "Package details dialog")
    @Composable
    fun PackageDetailsPreview() {
        DAOTheme {
            PackageDetails(
                PendingPackage(
                    147628,
                    "Ringevej 52",
                    "email@google.com",
                    "Noah",
                    "12345678",
                    "5750"
                )
            ) {

            }
        }
    }

    @Preview(showBackground = true, name = "Package List")
    @Composable
    fun PackageListPreview() {
        val packages = listOf(
            PendingPackage(147628, "Ringevej 52", "email@google.com", "Noah", "12345678", "5750"),
            PendingPackage(234151212, "Odense banegård 3", "some@email.example", "Nazarii", "12345678", "5000"),
            PendingPackage(314432512, "Svanninge bakker", "wowers@amazing.wow", "Lars", "12345678", "7320"),
        )

        val scannedPackages = listOf(
            PendingPackage(13423525, "Næsby", "email@google.com", "Emil", "12345678", "4230"),
            PendingPackage(25432242, "København", "some@email.example", "Kasper", "12345678", "2483"),
            PendingPackage(34214232, "Tis-sted", "wowers@amazing.wow", "Sten", "12345678", "7320"),
        )

        DAOTheme {
            PackageList(packages, scannedPackages, Modifier.fillMaxWidth().padding(16.dp))
        }
    }

    @Preview(showBackground = true, name = "scaffold")
    @Composable
    fun ScaffoldPreview() {
        DAOTheme {
            ScaffoldSetup()
        }
    }

    @Preview(showBackground = true, name = "alert dialog")
    @Composable
    fun AlertDialogPreview() {
        DAOTheme {
            ConfirmationDialog(
                onDismissRequest = {},
                onConfirmation = {},
            )
        }
    }

    @Preview(showBackground = true, name = "create package dialog")
    @Composable
    fun CreatePackageDialogPreview() {
        DAOTheme {
            CreatePackageDialog(
                onDismissRequest = {},
                onConfirmation = {},
            )
        }
    }
}