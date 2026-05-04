# Zest

Zest helps you scan packaged food labels and understand how processed they look. It focuses on the ingredient panel, gives a NOVA-style classification, highlights important ingredients, and keeps your scan history on your device.

## Why The Name Zest?

`Zest` is meant to feel fresh, clear, and food-focused. The app is built to help you cut through label noise, understand what is really inside a product, and make better everyday choices without friction.

In practice, Zest is about:

- reading labels faster
- spotting ingredient-level signals clearly
- choosing healthier options with more confidence

## What Zest Does

- Scan food ingredient labels with the camera.
- Import a label photo from your gallery.
- Scan a barcode and look up product data when USDA access is configured.
- Show a NOVA-style result for the full label.
- Break ingredients into compact color-coded bubbles.
- Show allergen signals in a separate section.
- Save scan history locally on your phone.
- Show local estimated token and cost usage in scan history.
- Use the same Zest splash, launcher icon, typography, and sound setting across the app.
- Keep API keys encrypted on device.

## Features

- Fast label analysis.
- Compact, easy-to-read results.
- Separate ingredient and allergen sections.
- Local scan history.
- Estimated local usage summary for tokens and cost.
- Optional barcode lookup.
- Optional model metadata in Settings.
- Optional app sound effects.
- No sign-in required.

## How To Set It Up

1. Install and open the app on an Android phone or tablet.
2. Go to `Settings`.
3. Add your AI model key if you want image and ingredient analysis.
4. Optionally add the USDA key if you want barcode lookup support.
5. Wait for the key status indicator to turn green before scanning.

## How To Use Zest

1. Open the app.
2. Choose one of these options:
   - Scan a label with the camera.
   - Upload a label photo from your gallery.
   - Scan a barcode.
3. Review the analysis result.
4. Tap the ingredient bubbles to read the ingredient-level NOVA signals.
5. Check the allergen block for separate allergen signals.
6. Open `History` to revisit old scans.

## What The Result Means

- `NOVA 1` usually means minimally processed.
- `NOVA 2-3` means the product has some processing.
- `NOVA 4` means the label has stronger ultra-processing signals.

Ingredient bubbles are color-coded individually from the API output. Allergen signals are shown separately so they do not get mixed into the ingredient score.

## Privacy

Zest is designed to keep your data local by default.

- Scan history stays on your device.
- Saved keys are encrypted on device.
- Saved keys are not shown back in plain text.
- No sign-in is required.

When you analyze a label, the app sends the image or extracted ingredient data to the selected API provider only if you have configured a key for that provider.

## Important Notes

- Zest is not medical advice.
- Zest is not a nutrition label verifier.
- If the image is not a real ingredient panel or ingredient list, the app will reject it.
- Barcode lookup depends on USDA availability and may fail if the product is missing from the database.
- API providers may rate-limit requests. If that happens, the app will tell you.

## B2 Group

Zest is built and maintained by the B2 group.

## Contributors

- Technical Advisor
  - Atul Bhagat - [LinkedIn](https://www.linkedin.com/in/bhagatatul/)
- Contributors
  - Emmy - LinkedIn: pending
  - Emre Can Baykurt - [LinkedIn](https://www.linkedin.com/in/ebaykurt/)
  - Samet Temurcin - [LinkedIn](https://www.linkedin.com/in/samet-temurcin/)
  - Ola Ajayi - [LinkedIn](https://www.linkedin.com/in/olaajayi1234/)

If you contributed code, design, testing, or product feedback, add your name here in future releases.

## License

Zest is distributed under a modified MIT-style non-commercial license.

See the license text in [LICENSE/LICENSE.md](LICENSE/LICENSE.md).

## Support

If the app fails to analyze a label:

- Check that the ingredient panel is visible and readable.
- Try a clearer photo with better lighting.
- Confirm your API key is saved in `Settings`.
- For barcode scans, confirm USDA lookup is configured.

## Project Links

- License: [LICENSE/LICENSE.md](LICENSE/LICENSE.md)
- Technical documentation: [documentation/README.md](documentation/README.md)
- Non-Android architecture guide: [documentation/00-android-app-guide.md](documentation/00-android-app-guide.md)
