#ENGLISH
# DDTips — Native Android

Android rewrite of the original Flutter app using Kotlin and Jetpack Compose.

## Included Features

- Random display of D&D tips and manual navigation;
- Configurable automatic rotation from 1 to 30 seconds;
- Editing, adding, deleting, and locally saving the tips list;
- Importing a text file with tips separated by the `|` character;
- Original background image and font included in the Android resources.

## Opening and Building

Open this folder in Android Studio, wait for Gradle synchronization, and run the `app` module on an Android device (minimum Android 8.0 / API 26).

The package ID retains the one from the Flutter configuration: `com.example.ddtips2`. Replace it with a final identifier before publishing the app.

The Flutter button for selecting a background image had no actual implementation: this version does not expose it to avoid presenting a non-functional feature.

#ITALIANO
# DDTips — Android nativo

Riscrittura Android dell'app Flutter originale con Kotlin e Jetpack Compose.

## Funzioni incluse

- visualizzazione casuale dei tip D&D e navigazione manuale;
- avanzamento automatico configurabile da 1 a 30 secondi;
- modifica, aggiunta, eliminazione e salvataggio locale della lista;
- importazione di un file di testo con i tip separati dal carattere `|`;
- immagine di sfondo e font originali inclusi nelle risorse Android.

## Apertura e build

Aprire questa cartella in Android Studio, attendere la sincronizzazione Gradle e avviare il modulo `app` su un dispositivo Android (minimo Android 8.0 / API 26).

Il package ID mantiene quello della configurazione Flutter: `com.example.ddtips2`. Sostituirlo con un identificativo definitivo prima di pubblicare l'app.

Il pulsante Flutter per scegliere uno sfondo non aveva implementazione effettiva: questa versione non lo espone per evitare di presentare una funzione non funzionante.
