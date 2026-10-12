# YaarKhata – Google Play checklist

## Done in code (verify with a Gradle sync + build)
- applicationId/namespace: `com.yaarkhata.app` (permanent once uploaded!)
- compileSdk/targetSdk 36 (AGP 8.5.0 + Gradle 8.7 with android.suppressUnsupportedCompileSdk=36)
- Edge-to-edge + predictive back enabled; R8 shrink on; release signing hooked to keystore.properties
- No permissions, no network -> simple Data safety form

## You must do
1. Play Console account ($25, ID verification). Personal accounts: closed test with 12+ testers for 14 continuous days before production access.
2. Create keystore (once, back it up!):
   `keytool -genkey -v -keystore yaarkhata.jks -keyalg RSA -keysize 2048 -validity 10000 -alias yaarkhata`
   Create `keystore.properties` in project root:
   ```
   storeFile=yaarkhata.jks
   storePassword=...
   keyAlias=yaarkhata
   keyPassword=...
   ```
3. Build > Generate Signed Bundle (AAB) > release. Upload to Closed testing. Enrol in Play App Signing.
4. Host PRIVACY_POLICY.md at a public URL (GitHub Pages / raw GitHub page) and paste the URL; add your email.
5. Data safety form: collects no data, shares no data, no encryption-in-transit needed (no network), users can request deletion via uninstall.
6. Content rating questionnaire (expect Everyone). Target audience: 18+. Ads: No. Category: Finance. Declare Finance-app section: not a loan/banking app.
7. Store assets: 512x512 icon, 1024x500 feature graphic, 2-8 phone screenshots.
8. Test on a real device after the package rename: uninstall the old build first.

## Listing text
Title: YaarKhata - Money & Debt Tracker
Short (80): Track bank & cash balances, expenses and what friends owe you. Fully offline.
Full: Keep all your money in one place. Add multiple bank and cash accounts, log expenses, income and adjustments, and see monthly and category reports. Track who owes you and what you owe, record partial payments, and share a summary on WhatsApp. Backup and restore with JSON. No ads, no sign-up, no internet - your data stays on your phone.

## Files in this project
- generate_keystore.bat  - double-click on Windows to create yaarkhata.jks
- keystore.properties.example - copy to keystore.properties and fill in
- docs/index.html - privacy policy page for GitHub Pages (Settings > Pages > Branch main, folder /docs)
- store_assets/ - 512 icon, feature graphic, listing text, Data safety answers
