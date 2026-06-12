package com.example.ocrproject

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_BASE
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

class ScanManager(private val activity: ComponentActivity) {

    // [성공 콜백 저장소] 스캔이 성공하면 외부(MainActivity)로 Uri를 던져줄 함수 통로야.
    private var onScanSuccessCallback: ((android.net.Uri) -> Unit)? = null

    // 규칙 세팅
    private val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false)
        .setPageLimit(1)
        .setResultFormats(RESULT_FORMAT_JPEG)
        .setScannerMode(SCANNER_MODE_BASE)
        .build()

    // 스캐너 엔진 준비
    private val scanner = GmsDocumentScanning.getClient(options)

    // 복귀 통로 개설
    private val scannerLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)

            // 코틀린 스타일로 깔끔하게 안전한 참조(?.)와 프로퍼티(pages)로 접근해!
            scanningResult?.pages?.firstOrNull()?.let { page ->
                val imageUri = page.imageUri

                // 이미지를 성공적으로 확보했으니, 지시했던 메인 화면에 Uri를 토스해준다!
                onScanSuccessCallback?.invoke(imageUri)
            }
        }
    }

    fun startCameraScan(onScanSuccess: (android.net.Uri?) -> Unit) {
        // 나중에 사진 다 찍으면 실행할 일을 계약서로 미리 받아둠
        this.onScanSuccessCallback = onScanSuccess

        // 진짜로 구글 인텐트를 받아와서 스캐너 화면을 빵 띄우는 실제 작업 구역!
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}