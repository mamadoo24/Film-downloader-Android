# Film Downloader Android

Version Android native inspirée de la version Python V11.

## Fonctionnement
- WebView intégré pour Loadix.
- Connexion et CAPTCHA/anti-bot laissés entièrement à l'utilisateur.
- Recherche du film.
- Filtrage des releases 1Fichier selon taille min/max.
- Parmi les releases compatibles, sélection de la plus récente lorsque la date est détectable.
- Ouverture du lien 1Fichier révélé.

Cette application n'essaie pas de résoudre ni contourner un CAPTCHA.

## Générer l'APK dans Android Studio
1. Installer Android Studio.
2. Décompresser ce projet.
3. Android Studio > Open > choisir le dossier `FilmDownloaderAndroid`.
4. Attendre la fin de la synchronisation Gradle. Accepter l'installation du SDK Android 35 si Android Studio la propose.
5. Menu Build > Build App Bundle(s) / APK(s) > Build APK(s).
6. Une notification `APK(s) generated successfully` apparaît. Cliquer `locate`.
7. L'APK debug se trouve normalement dans :
   `app/build/outputs/apk/debug/app-debug.apk`
8. Copier `app-debug.apk` sur le téléphone et l'ouvrir. Android peut demander d'autoriser l'installation d'applications provenant de cette source.

## APK signé pour distribution
Dans Android Studio :
Build > Generate Signed App Bundle or APK > APK > Create new...
Créez un keystore, conservez-le précieusement, sélectionnez `release`, puis terminez l'assistant.
