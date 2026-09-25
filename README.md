# NN_AlertaMesh

**Curso:** Computación en Red III
**Universidad:** Universidad Católica de Santa María — Escuela Profesional de Ingeniería de Sistemas

## Descripción
AlertaMesh es una aplicación Android de mensajería de emergencia que funciona **sin Internet ni red celular**. Tras un sismo de gran magnitud, cuando las antenas se saturan o colapsan, los teléfonos cercanos se conectan directamente entre sí por **Bluetooth y Wi-Fi Direct** y forman una red en malla (mesh): cada teléfono envía, recibe y **reenvía** los mensajes de los demás, ampliando el alcance más allá de un solo enlace.

Características:
- Pantalla de ingreso con nombre y DNI (se guardan solo en el teléfono).
- Chat de grupo: los mensajes escritos por el usuario llegan a los demás teléfonos de la red.
- Botón **SOS**: los mensajes de auxilio salen siempre antes que los normales (cola de prioridad) y se muestran en rojo.
- Reenvío multisalto con control de duplicados y TTL.
- Cifrado AES-256-GCM de todo el contenido (nombre, DNI y texto) con una clave derivada del código de red; el DNI nunca viaja en claro.
- Modo de bajo consumo que espacia la frecuencia de envío.
- Si no hay vecinos cerca, los mensajes esperan en cola y salen cuando aparece uno.

Proyecto del curso **Computación en Red III**. Versión 1.1 (prueba de concepto, TRL 3–4).

## Integrantes
- Alexandra Arce
- Farid Cardenas
- Jhrolan Puma
- Matías Valdivia

Universidad Católica de Santa María — Escuela Profesional de Ingeniería de Sistemas.

## Tecnologías utilizadas
- Kotlin 1.9 y Android SDK (minSdk 26, targetSdk 34)
- Google Nearby Connections API (`play-services-nearby`), que usa Bluetooth LE para descubrir y Bluetooth / Wi-Fi Direct para transferir
- Criptografía estándar de Java (AES-256-GCM, PBKDF2-HMAC-SHA256)
- Gradle 8 con Android Gradle Plugin 8.5
- JUnit 4 (pruebas unitarias)

## Requisitos
- Windows, macOS o Linux, con JDK 17+, Git y **Android Studio** (Koala o superior) con Android SDK 34
- **Dos o más teléfonos Android físicos** (8.0 o superior) con **Google Play Services**. El emulador no sirve para probar la comunicación real, porque no tiene radios Bluetooth/Wi-Fi.
- En cada teléfono: Bluetooth y Wi-Fi activados, y Ubicación activada
- Cable USB y depuración USB activada en los teléfonos
- No requiere Internet para funcionar, ni base de datos, ni variables de entorno (no aplica `.env.example`). Solo necesita Internet la primera vez, para descargar dependencias con Gradle.

## Instalación y ejecución

### Opción A: Instalar el APK ya compilado (rápido, para probar en varios teléfonos)
1. Descargar el archivo `.apk` desde la sección **Releases** de este repositorio (o el enlace/archivo compartido por el equipo).
2. Copiar el APK a cada teléfono Android (por USB, Drive, WhatsApp, etc.).
3. En cada teléfono, ir a **Ajustes → Seguridad** y habilitar **"Instalar apps de orígenes desconocidos"** para la app que se use para abrir el archivo (Archivos, Chrome, etc.).
4. Abrir el archivo `.apk` en el teléfono y pulsar **Instalar**.
5. Al abrir la app por primera vez, aceptar los permisos de Bluetooth / dispositivos cercanos / ubicación.
6. En la pantalla de ingreso, escribir nombre, DNI (8 dígitos) y el **mismo código de red** en todos los teléfonos.

### Opción B: Compilar desde el código fuente (Android Studio)
```bash
git clone https://github.com/<usuario>/NN_AlertaMesh.git
```
1. Abrir la carpeta en Android Studio (File → Open) y esperar el Gradle Sync.
2. Conectar un teléfono por USB (con depuración USB activada) y pulsar **Run ▶**. Repetir en el segundo teléfono (cambiar el dispositivo en la barra superior y Run de nuevo).
3. En cada teléfono, aceptar los permisos de Bluetooth / dispositivos cercanos / ubicación.
4. En la pantalla de ingreso, escribir nombre, DNI (8 dígitos) y el **mismo código de red** en todos los teléfonos.

## Prueba de funcionamiento
1. Con ambos teléfonos ya dentro del chat, esperar unos segundos hasta que el estado diga **"● 1 vecino(s) conectado(s)"**.
2. Escribir un mensaje en el teléfono A y pulsar **Enviar**: debe aparecer en el teléfono B con el nombre y DNI del remitente.
3. Pulsar **SOS** en B: el mensaje llega a A resaltado en rojo.
4. Con un tercer teléfono, ubicado de modo que solo alcance a uno de los otros, comprobar el reenvío multisalto (el mensaje muestra el número de saltos).
5. Con un código de red distinto en un teléfono, ese teléfono no puede leer los mensajes de la otra red.

## Pruebas automáticas
```bash
./gradlew test          # Linux/macOS
gradlew.bat test        # Windows
```
Cubren prioridad SOS, multisalto, duplicados, TTL, mensajes malformados, modo de bajo consumo, cola sin vecinos, acuerdo de claves y cifrado/descifrado.

## Estructura del proyecto
```
app/src/main/java/pe/edu/ucsm/alertamesh/
├── model/       MeshMessage, ChatPacket (formatos de mensaje)
├── crypto/      CryptoManager (AES-256-GCM, PBKDF2, ECDH)
├── routing/     MeshRouter (multisalto, duplicados, TTL, cola SOS)
├── power/       PowerMode (normal / bajo consumo)
├── transport/   Transport, NearbyTransport (real), SimulatedNetwork (pruebas)
├── MeshNode.kt  Une transporte, enrutador y cifrado
└── MainActivity.kt  Pantalla de ingreso y chat
app/src/test/    Pruebas unitarias
```

## Limitaciones conocidas
- La app debe estar abierta (en primer plano) para participar en la malla.
- El modo de bajo consumo reduce la frecuencia de envío; el escaneo de radio lo administra Nearby Connections.
- El chat es de grupo (broadcast). Los mensajes directos y la ubicación GPS quedan como trabajo futuro.

## Seguridad
El repositorio no contiene contraseñas, tokens, certificados ni claves privadas. El DNI y el nombre se guardan solo en el teléfono y viajan únicamente dentro de mensajes cifrados. Lo único que se anuncia por radio es un identificador aleatorio.

## Entrega del curso
- Repositorio de GitHub con el código fuente actualizado, este `README.md`, y el APK compilado adjunto en **Releases**.
- Colaborador invitado en el repositorio de GitHub: **`<CORREO_INSTITUCIONAL_DOCENTE>`** *(pendiente: falta el correo institucional para completar la invitación)*.
