# Camera, OCR, And Barcode

This component owns product input capture. It turns physical product labels or barcodes into local image paths or barcode values for downstream analysis. OCR remains available as a fallback path, but the default label path can send the captured image into the staged LLM workflow when the user has saved an LLM key.

## Files

- `camera/CameraCaptureController.kt`
- `camera/LocalImageImportController.kt`
- `ocr/MlKitOcrPipeline.kt`
- `ocr/OcrPipeline.kt`
- `ocr/OcrResult.kt`
- `barcode/BarcodeLiveScanController.kt`
- `barcode/MlKitBarcodeScanner.kt`
- `barcode/BarcodeScanner.kt`
- `barcode/BarcodeResult.kt`

## Label Capture Flow

```mermaid
flowchart LR
    User[User taps Scan Label] --> CameraX[CameraX ImageCapture]
    CameraX --> File[App-local image file]
    File --> Analysis[FoodAnalysisPipeline]
    Analysis --> LLM[LLM ingredient extraction]
    Analysis --> OCR[ML Kit OCR fallback]
```

## Gallery Import Flow

```mermaid
flowchart LR
    Picker[Android content picker] --> Uri[Content URI]
    Uri --> Copy[Copy stream into app-local imports folder]
    Copy --> Analysis[FoodAnalysisPipeline]
    Analysis --> LLM[LLM ingredient extraction]
    Analysis --> OCR[ML Kit OCR fallback]
```

## Barcode Flow

```mermaid
flowchart LR
    Preview[CameraX Preview] --> Analyzer[ImageAnalysis]
    Analyzer --> MLKit[ML Kit Barcode Scanner]
    MLKit --> Code[UPC/EAN/code value]
    Code --> USDA[USDA lookup]
```

## Implementation Notes

- Camera capture uses `CAPTURE_MODE_MINIMIZE_LATENCY`.
- Barcode live scanning uses `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` to prevent analyzer backlog.
- Barcode delivery is guarded so the same visible barcode does not trigger repeated navigation.
- Imported images are copied into app-local external files before analysis.
- OCR uses ML Kit Text Recognition with Latin options.
- OCR is intentionally behind the `OcrPipeline` interface so future on-device OCR can feed the same classification/allergen stages.

## Failure Behavior

- Missing image file returns a typed OCR/barcode failure.
- Empty OCR text returns a user-friendly failure.
- Barcode miss can fall back to image analysis when an image path is available.
