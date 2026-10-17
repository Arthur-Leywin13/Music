# MusicStream

App Android (Kotlin + Jetpack Compose) : recherche YouTube Data API v3 + lecteur YouTube officiel (IFrame Player). Pas de téléchargement, pas d'extraction audio.

## Build avec GitHub
1. Crée un dépôt GitHub et pousse ce dossier.
2. Settings → Secrets and variables → Actions → New repository secret : `YT_API_KEY` (clé YouTube Data API v3).
3. Onglet Actions → "Build APK" → Run workflow (ou simple push).
4. Télécharge l'artefact `musicstream-apk` → `app-debug.apk`, puis installe-le sur le téléphone.

## Clé API
Google Cloud Console → nouveau projet → activer "YouTube Data API v3" → Identifiants → Créer une clé API (restreindre à cette API). La clé peut aussi être saisie dans Réglages de l'app.

## Build local
`gradle assembleDebug` (JDK 17, Android SDK 34) puis `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
