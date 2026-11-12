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
