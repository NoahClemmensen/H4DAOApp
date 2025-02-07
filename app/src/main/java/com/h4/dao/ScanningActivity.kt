package com.h4.dao

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
    private var apiService: ApiService = ApiService("http://172.27.232.4:3000/")

    private var showBottomSheet = mutableStateOf(false)
    private var openCreatePackageDialog = mutableStateOf(false)
    private var openConfirmationDialog = mutableStateOf(false)
    private var pendingPackages: MutableStateFlow<List<PendingPackage>> = MutableStateFlow(
        listOf()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                val packages = apiService.getPendingPackages()
                if (packages != null) {
                    pendingPackages.value = packages
                } else {
                    pendingPackages.value = listOf()
                }
            } catch (e: Exception) {
                Log.e("ScanningActivity", "Failed to fetch pending packages", e)
                pendingPackages.value = listOf()
            }
        }

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
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                        IconButton(onClick = {
                            showBottomSheet.value = true
                        }) {
                            Icon(
                                Icons.Outlined.Menu,
                                contentDescription = "Localized description",
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
                        onConfirmation = { openConfirmationDialog.value = false },
                    )
                }
                openCreatePackageDialog.value -> {
                    CreatePackageDialog(
                        onDismissRequest = { openCreatePackageDialog.value = false },
                        onConfirmation = { openCreatePackageDialog.value = false },
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
        onConfirmation: (String) -> Unit,
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
                                    onConfirmation(packageId)
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PartialBottomSheet() {

        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        )

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
                    Text(
                        "Swipe up to open sheet. Swipe down to dismiss.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
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
            true -> CameraPreview(camService.GetCameraProviderFuture(context))
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
                            camService.BindPreview(
                                cameraProvider,
                                lifecycleOwner,
                                this,
                            )
                        }, ContextCompat.getMainExecutor(context))
                    }
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