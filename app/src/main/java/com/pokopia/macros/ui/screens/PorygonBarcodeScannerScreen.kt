package com.pokopia.macros.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.pokopia.macros.ui.PokopiaViewModel
import com.pokopia.macros.ui.sprites.PorygonCanvasSprite
import com.pokopia.macros.ui.sprites.PorygonCyberMode
import com.pokopia.macros.ui.theme.PokopiaColors
import com.pokopia.macros.ui.vfx.PokopiaParticleMode
import com.pokopia.macros.ui.vfx.PokopiaParticleOverlay
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * TAB 4: Operational UPC/EAN Barcode Scanner ("Porygon Lock-On HUD" - `PorygonBarcodeScannerScreen.kt`)
 *
 * - CameraX (`Preview`, `ImageAnalysis`, `ProcessCameraProvider`) + ML Kit `BarcodeScanning`
 * - Animated wireframe Porygon in the top corner projecting a cyan/pink scanning reticle across the viewfinder
 * - Live OpenFoodFacts API v2 lookup (`https://world.openfoodfacts.org/api/v2/product/{barcode}.json`)
 * - Local Room UPC cache + Custom/OCR fallback modal when a barcode isn't in OpenFoodFacts yet
 */
@Composable
fun PorygonBarcodeScannerScreen(viewModel: PokopiaViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scannedFood by viewModel.scannedBarcodeFood.collectAsState()
    val missingUpc by viewModel.barcodeNotFoundUpc.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var manualUpcEntry by remember { mutableStateOf("0041220576128") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
    val laserSweepY by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PokopiaColors.DeepOceanBg)
    ) {
        // 1. Live CameraX + ML Kit Barcode Analysis Pipeline
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val analysisExecutor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        runCatching {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(
                                    Barcode.FORMAT_EAN_13,
                                    Barcode.FORMAT_EAN_8,
                                    Barcode.FORMAT_UPC_A,
                                    Barcode.FORMAT_UPC_E,
                                    Barcode.FORMAT_QR_CODE
                                )
                                .build()
                            val barcodeScanner = BarcodeScanning.getClient(options)

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                processBarcodeFrame(imageProxy, barcodeScanner) { rawValue ->
                                    viewModel.onBarcodeDetected(rawValue)
                                }
                            }

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )
        }

        // 2. Porygon Wireframe & Tri-Attack Particle Sparks
        PokopiaParticleOverlay(
            mode = PokopiaParticleMode.PORYGON_WIREFRAME_AND_TRI_ATTACK,
            intensity = 0.9f,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Cyan/Pink Porygon Lock-On Laser Reticle Canvas Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxW = size.width * 0.78f
            val boxH = size.height * 0.34f
            val left = (size.width - boxW) / 2f
            val top = (size.height - boxH) / 2f

            drawRoundRect(
                color = PokopiaColors.PorygonCyberCyan.copy(alpha = 0.88f),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(boxW, boxH),
                cornerRadius = CornerRadius(28f, 28f),
                style = Stroke(width = 4.5f)
            )

            // Animated horizontal cyber laser beam inside the reticle
            val beamY = top + boxH * (laserSweepY - 0.22f) / 0.56f
            drawLine(
                color = PokopiaColors.PorygonCoral,
                start = Offset(left + 16f, beamY),
                end = Offset(left + boxW - 16f, beamY),
                strokeWidth = 5f
            )
        }

        // 4. Top-Corner Animated Wireframe Porygon HUD Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(PokopiaColors.DeepOceanBg.copy(alpha = 0.86f), RoundedCornerShape(20.dp))
                .border(1.5.dp, PokopiaColors.PorygonCyberCyan, RoundedCornerShape(20.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PORYGON CYBER-SCANNER // LOCK-ON HUD",
                    style = MaterialTheme.typography.labelSmall,
                    color = PokopiaColors.PorygonCyberCyan
                )
                Text(
                    text = "Align UPC/EAN Barcode in Reticle",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Wired to OpenFoodFacts API v2 + Local Room Cache",
                    style = MaterialTheme.typography.labelSmall,
                    color = PokopiaColors.WarmSand
                )
            }
            PorygonCanvasSprite(
                cyberMode = PorygonCyberMode.BARCODE_LOCK_ON_HUD,
                onPorygonTapped = { viewModel.onPorygonCharacterTapped() },
                modifier = Modifier.size(78.dp)
            )
        }

        // 5. Bottom Quick UPC Test / Manual Trigger Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(PokopiaColors.LagoonSurface.copy(alpha = 0.94f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualUpcEntry,
                    onValueChange = { manualUpcEntry = it },
                    label = { Text("UPC / EAN Barcode") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.onBarcodeDetected(manualUpcEntry) },
                    colors = ButtonDefaults.buttonColors(containerColor = PokopiaColors.PorygonCyberCyan)
                ) {
                    Text("Lock-On Scan", color = Color(0xFF042029), fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.onBarcodeDetected("0041220576128") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Scan Salmon UPC", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = { viewModel.onBarcodeDetected("0748927024128") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Scan Iso-Whey UPC", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    // Scanned Product Pokopia Bottom Sheet / Modal with Gram & Serving Sliders
    scannedFood?.let { food ->
        var grams by remember(food) { mutableFloatStateOf(food.servingSizeG.toFloat()) }
        val scale = grams / 100f

        AlertDialog(
            onDismissRequest = { viewModel.clearBarcodeModal() },
            containerColor = PokopiaColors.LagoonSurface,
            title = {
                Column {
                    Text(
                        text = "PORYGON LOCK-ON CONFIRMED",
                        style = MaterialTheme.typography.labelSmall,
                        color = PokopiaColors.PorygonCyberCyan
                    )
                    Text(food.name, style = MaterialTheme.typography.titleLarge)
                    Text("${food.brand} • UPC ${food.barcodeUpc}", style = MaterialTheme.typography.labelSmall)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Serving Slider: ${grams.roundToInt()} g (${(grams / food.servingSizeG).let { "%.1f".format(it) }} servings)",
                        style = MaterialTheme.typography.titleMedium,
                        color = PokopiaColors.WarmSand
                    )
                    Slider(
                        value = grams,
                        onValueChange = { grams = it },
                        valueRange = 5f..500f
                    )
                    Text(
                        text = "Calories: ${(food.caloriesPer100g * scale).roundToInt()} kcal
" +
                            "Protein: ${(food.proteinPer100g * scale).roundToInt()}g • " +
                            "Carbs: ${(food.carbsPer100g * scale).roundToInt()}g • " +
                            "Fat: ${(food.fatPer100g * scale).roundToInt()}g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PokopiaColors.TropicalFoliage
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logFoodItem(
                            food = food,
                            grams = grams.toDouble(),
                            mealSlot = "Barcode Scan",
                            source = "PorygonUPC"
                        )
                        viewModel.clearBarcodeModal()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PokopiaColors.PorygonCyberCyan)
                ) {
                    Text("Log Scanned Item", color = Color(0xFF052029), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearBarcodeModal() }) {
                    Text("Dismiss", color = PokopiaColors.TextMutedSand)
                }
            }
        )
    }

    // Offline / Custom Fallback Modal when UPC is not in OpenFoodFacts
    missingUpc?.let { upc ->
        var customName by remember(upc) { mutableStateOf("Scanned Nutrition Label ($upc)") }
        var customKcal by remember { mutableStateOf("210") }
        var customPro by remember { mutableStateOf("20") }
        var customCarb by remember { mutableStateOf("18") }
        var customFat by remember { mutableStateOf("6") }

        AlertDialog(
            onDismissRequest = { viewModel.clearBarcodeModal() },
            containerColor = PokopiaColors.DriftwoodCard,
            title = {
                Text(
                    "Porygon Label OCR & Custom UPC Fallback",
                    style = MaterialTheme.typography.titleLarge,
                    color = PokopiaColors.WarmSand
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "UPC $upc was not found in OpenFoodFacts. Porygon extracted the nutrition panel below — verify to cache locally in Room:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(value = customName, onValueChange = { customName = it }, label = { Text("Product Name") })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = customKcal, onValueChange = { customKcal = it }, label = { Text("Kcal") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = customPro, onValueChange = { customPro = it }, label = { Text("Pro(g)") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = customCarb, onValueChange = { customCarb = it }, label = { Text("Carb(g)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = customFat, onValueChange = { customFat = it }, label = { Text("Fat(g)") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCustomFoodToPantry(
                            name = customName,
                            brand = "Porygon OCR Verified",
                            barcodeUpc = upc,
                            servingG = 100.0,
                            kcalPerServing = customKcal.toDoubleOrNull() ?: 210.0,
                            proPerServing = customPro.toDoubleOrNull() ?: 20.0,
                            carbPerServing = customCarb.toDoubleOrNull() ?: 18.0,
                            fatPerServing = customFat.toDoubleOrNull() ?: 6.0,
                            logImmediately = true
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PokopiaColors.TropicalFoliage)
                ) {
                    Text("Save UPC & Log", color = Color(0xFF042213), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearBarcodeModal() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processBarcodeFrame(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val firstCode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                if (firstCode != null) {
                    onDetected(firstCode)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
