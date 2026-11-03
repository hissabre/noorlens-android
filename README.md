# Noor Lens (Basirah) — application Android pour Meta Quest

Application de test : Coran hors ligne, reconnaissance **native** (whisper.cpp) sur le casque, formules de prière, mode Démo.
Aucun serveur, aucun son envoyé : tout est dans l'APK.

## 1. Construire l'APK (une fois, ~15 min, sans rien installer sur votre PC)

1. Créez un dépôt GitHub (privé, par exemple `noorlens-android`).
2. Envoyez-y **tout le contenu de ce dossier** (bouton *Add file → Upload files*, glissez les fichiers et dossiers).
   - Le dossier `.github` est caché sur certains systèmes. S'il n'est pas envoyé : *Add file → Create new file*, nommez-le `.github/workflows/build.yml` et collez le contenu du fichier du même nom.
3. Onglet **Actions** → *Construire l'APK Noor Lens* → **Run workflow**.
   - Choisissez le modèle : `tiny` (le plus rapide), `base` (équilibre, recommandé pour commencer), `small` (le plus précis, plus lent).
4. Quand la pastille est verte, ouvrez l'exécution et téléchargez l'artefact **NoorLens-apk** (un ZIP contenant `app-release.apk`).
5. Si la construction échoue (pastille rouge) : copiez les dernières lignes du journal rouge et envoyez-les-moi.

## 2. Installer sur le Quest

1. Activez le **mode développeur** : application Meta Horizon (téléphone) → Appareils → Paramètres du casque → Mode développeur.
2. Sur PC, installez **SideQuest** (ou Meta Quest Developer Hub), branchez le casque en USB et acceptez la demande dans le casque.
3. Glissez `app-release.apk` dans SideQuest (ou : `adb install -r app-release.apk`).
4. Dans le casque : Bibliothèque → filtre **Sources inconnues** → **Noor Lens**.

## 3. Utilisation

- Premier lancement : l'app copie le modèle (≈ 1 min), puis affiche « Moteur natif prêt ». Autorisez le micro.
- **Écouter** démarre la reconnaissance. Le statut affiche le temps d'analyse (« analyse 1.8 s ») : **c'est le chiffre à me communiquer**.
- **▶ Démo** simule une prière complète sans micro (pour juger l'affichage).
- Les réglages (⚙) gardent les mêmes options que la version web : affichage arabe d'abord, traductions, tailles.

## Limites de cette première version

- C'est une fenêtre 2D dans le casque (pas encore un panneau fixé dans votre champ de vision avec passthrough : cela demande une application VR native, étape suivante).
- Les traductions sont celles de Tanzil (usage non commercial) ; les formules de prière ont des traductions rédigées pour l'app.
- Le bouton « AR » de la version web est masqué ici (WebXR n'existe pas dans une application Android).
