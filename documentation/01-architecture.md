# Architecture

Zest is a native Android app for label analysis. It captures a food label image, extracts ingredient text, sends that text to staged API workflows for NOVA classification and allergen detection, and stores the final result locally for history and review.

## Design Goals

- Keep classification and allergen logic API-driven.
- Keep secret storage encrypted and out of source control.
- Keep the UI deterministic and driven by explicit contracts.
- Keep history local, deletable, and exportable by future work.
- Keep the pipeline modular so on-device OCR can feed the same API contracts later.

## Runtime Layers

```mermaid
flowchart LR
    UI[Compose UI] --> Orchestration[FoodAnalysisPipeline]
    Orchestration --> Input[Camera / Gallery / Barcode / OCR]
    Orchestration --> LLM[network/llm]
    Orchestration --> USDA[network/usda]
    LLM --> Prompts[prompts/*.md]
    UI --> Storage[Room + Encrypted Secrets]
```

## Main User Flows

### Label Image

```mermaid
sequenceDiagram
    participant User
    participant Scanner as ScannerScreen
    participant Pipeline as FoodAnalysisPipeline
    participant LLM as FoodLabelLlmWorkflow
    participant Room

    User->>Scanner: Capture or import image
    Scanner->>Pipeline: analyzeFromImage(path)
    Pipeline->>LLM: extractIngredients(image)
    LLM-->>Pipeline: IngredientExtraction
    Pipeline->>LLM: classifyIngredients(extraction)
    Pipeline->>LLM: detectAllergens(extraction)
    Pipeline-->>Scanner: AnalysisReport
    Scanner->>Room: Persist scan result
```

### Barcode

```mermaid
sequenceDiagram
    participant User
    participant Scanner as ScannerScreen
    participant Pipeline as FoodAnalysisPipeline
    participant USDA as UsdaRepository
    participant LLM as FoodLabelLlmWorkflow

    User->>Scanner: Scan barcode
    Scanner->>Pipeline: analyzeFromBarcode(code)
    Pipeline->>USDA: lookupByBarcode(code)
    USDA-->>Pipeline: USDA product record
    Pipeline->>LLM: classifyIngredients(from USDA text)
    Pipeline->>LLM: detectAllergens(from USDA text)
    Pipeline-->>Scanner: AnalysisReport
```

## Component Boundaries

- `ui/` owns Compose state, screen transitions, and display logic.
- `analysis/` owns orchestration, stage timing, and failure policy.
- `network/llm/` owns provider requests, retry repair, parsing, and prompt assets.
- `network/usda/` owns FoodData Central lookup, retry handling, and exact-hit ranking.
- `storage/room/` owns scan persistence.
- `storage/secrets/` owns encrypted API key storage.

## Key Contracts

### Analysis Report

```text
AnalysisReport
├── sourceType
├── productName
├── ingredientsTextUsed
├── warnings
└── scanResult: ScanResultUi
```

### Result UI Model

```text
ScanResultUi
├── productName
├── novaGroup
├── summary
├── problemIngredients
├── allIngredients
├── allergens
├── ingredientAssessments
├── rawIngredientText
├── labelImagePath
└── usageEstimate
```

## Production Rules

```mermaid
classDiagram
    class FoodAnalysisPipeline {
        +analyzeFromImage()
        +analyzeFromBarcode()
        +analyzeFromBarcodeImage()
    }
    class FoodLabelLlmWorkflow {
        <<interface>>
        +extractIngredients()
        +classifyIngredients()
        +detectAllergens()
    }
    class UsdaRepository {
        +lookupByBarcode()
    }
    class SecretKeyManager {
        +saveApiKey()
        +getApiKey()
    }
    class ScanResultUi
    FoodAnalysisPipeline --> FoodLabelLlmWorkflow
    FoodAnalysisPipeline --> UsdaRepository
    FoodAnalysisPipeline --> ScanResultUi
    SecretKeyManager --> FoodAnalysisPipeline
```

- Do not use rules-based NOVA classification in runtime code.
- Do not treat allergens as a signal inside ingredient coloring.
- Do not infer ingredients from product name or package art.
- Do not persist plaintext keys or secret values in Compose state.

## Failure Policy

- Invalid image at extraction returns `code = -1` and stops.
- API rate limit errors surface as 429-specific UI messages.
- USDA lookup miss falls back to image analysis only when an image exists.
- If the LLM workflow is unavailable, the analysis fails rather than inventing a result.
