# Film Downloader Android — compilation GitHub depuis un téléphone

Ce dossier est prêt pour GitHub Actions.

## Depuis Android
1. Créez un nouveau dépôt GitHub.
2. Envoyez **le contenu de ce dossier** à la racine du dépôt. Il faut notamment voir `app`, `.github`, `build.gradle.kts`, `settings.gradle.kts`.
3. Ouvrez l'onglet **Actions** du dépôt.
4. Ouvrez **Construire APK Android**.
5. Touchez **Run workflow**, puis confirmez.
6. Attendez que le travail `build-apk` devienne vert.
7. Ouvrez l'exécution terminée.
8. Dans **Artifacts**, téléchargez **FilmDownloader-Android-APK**.
9. Décompressez l'archive téléchargée sur Android : elle contient `app-debug.apk`.
10. Ouvrez `app-debug.apk` pour l'installer.

Le workflow compile un APK de test (debug), adapté à une installation personnelle.
Il ne résout ni ne contourne les CAPTCHA : les vérifications restent manuelles.
