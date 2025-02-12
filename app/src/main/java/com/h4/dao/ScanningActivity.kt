package com.h4.dao

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.google.mlkit.vision.barcode.common.Barcode
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.h4.dao.services.ApiService
import com.h4.dao.services.CameraService
import com.h4.dao.ui.theme.DAOTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ScanningActivity : ComponentActivity() {
    private var camService: CameraService = CameraService()
    private var apiService: ApiService = ApiService("http://172.27.238.8:3000/")

    private var showBottomSheet = mutableStateOf(false)
    private var openCreatePackageDialog = mutableStateOf(false)
    private var openConfirmationDialog = mutableStateOf(false)
    private var pendingPackages: MutableStateFlow<List<PendingPackage>> = MutableStateFlow(
        listOf()
    )
    private var scannedPackages: MutableStateFlow<List<PendingPackage>> = MutableStateFlow(
        listOf()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadPackagesFromApi()

        enableEdgeToEdge()
        setContent {
            DAOTheme {
                ScaffoldSetup()
            }
        }
    }

    private fun loadPackagesFromApi() {
        lifecycleScope.launch {
            try {
                val packages = apiService.getPendingPackages()
                if (packages != null) {
                    pendingPackages.value = packages
                } else {
                    pendingPackages.value = listOf()
                }

                Log.d("ScanningActivity", "Fetched pending packages: $packages")
            } catch (e: Exception) {
                Log.e("ScanningActivity", "Failed to fetch pending packages", e)
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
                                        loadPackagesFromApi()
                                    }

                                    Log.d("ScanningActivity", "Marked packages as delivered: $response")
                                } catch (e: Exception) {
                                    Log.e(
                                        "ScanningActivity",
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
        AlertDialog(
            icon = {
                Icon(Icons.Outlined.Check, contentDescription = "Checkmark")
            },
            title = {
                Text("Mark scanned packages as delivered")
            },
            text = {
                Text("Do you want to confirm these packages delivered?")
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
                        pack.name,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 24.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Package ID: ${pack.barcode}",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Address: ${pack.address}",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Zip code: ${pack.zipcode}",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Email: ${pack.email}",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Phone: ${pack.phone}",
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
            Text(text = pack.name)
            Text(text = pack.address, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis, maxLines = 1)
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

        val context = LocalContext.current

        LaunchedEffect(Unit) {
            isPermissionGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (requestPermission) {
            RequestCameraPermission { isGranted ->
                isPermissionGranted = isGranted
                requestPermission = false
            }
        }

        when (isPermissionGranted) {
            true -> CameraPreview(camService.getCameraProviderFuture(context))
            false -> Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceAround) {
                Button(onClick = {
                    requestPermission = true
                }) {
                    Text("Request permission")
                }
            }
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
                barcode = detectedBarcode!!,
                onDismissRequest = {
                    showDialog = false
                    isScanning = true
                },
                onAdd = {
                    // Handle add action
                    showDialog = false
                    isScanning = true
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
        barcode: Barcode,
        onDismissRequest: () -> Unit,
        onAdd: () -> Unit,
        onDontAdd: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { onDismissRequest() },
            title = { Text("Barcode Details") },
            text = { Text("Detected barcode: ${barcode.rawValue}") },
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