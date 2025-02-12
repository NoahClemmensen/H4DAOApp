package com.h4.dao.services

import android.content.Context
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage

class CameraService() {
    fun getCameraProviderFuture(context: Context): ListenableFuture<ProcessCameraProvider> {
        return ProcessCameraProvider.getInstance(context)
    }

    @OptIn(ExperimentalGetImage::class)
    fun bindPreview(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onBarcodeDetected: (List<Barcode>) -> Unit
    ) {
        val preview: Preview = Preview.Builder().build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val imageAnalyzer = ImageAnalysis.Builder()
            .build()
            .also { imageAnalysis ->
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(previewView.context)) { imageProxy ->
                    processImageProxy(imageProxy, onBarcodeDetected)
                }
            }

        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImageProxy(imageProxy: ImageProxy, onBarcodeDetected: (List<Barcode>) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_ALL_FORMATS
                )
                .build()
            val scanner = BarcodeScanning.getClient(options)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    onBarcodeDetected(barcodes)
                }
                .addOnFailureListener {
                    // Handle failure
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}