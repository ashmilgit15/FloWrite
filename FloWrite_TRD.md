**FloWrite**

Technical Requirements Document

Version 1.0 \| May 2025

*Powered by Groq Whisper-large-v3-turbo*

**1. Architecture Overview**

FloWrite follows a single-activity, service-centric architecture. The
core recording and transcription logic lives in long-running Android
Services, while the UI layer is built entirely with Jetpack Compose
rendered inside a System Overlay window. The app is structured using
Clean Architecture with MVVM in the presentation layer.

**1.1 High-Level Component Diagram**

  ----------------------- -----------------------------------------------
  **Overlay Layer**       FloatingOverlayService (WindowManager) ---
                          renders Compose UI floating button

  **Audio Layer**         AudioCaptureService --- manages AudioRecord,
                          VAD, WAV encoding

  **Transcription Layer** GroqTranscriptionRepository --- HTTP client,
                          retry, response parsing

  **Injection Layer**     TextInjectionAccessibilityService --- injects
                          text via AccessibilityNodeInfo

  **Storage Layer**       Room DB (TranscriptionEntity) +
                          EncryptedSharedPreferences

  **DI**                  Hilt --- all services, repositories, and
                          ViewModels injected
  ----------------------- -----------------------------------------------

**2. Tech Stack**

  ------------------- ------------------------------ ---------------------------
  **Layer**           **Technology**                 **Notes**

  Language            Kotlin 2.x                     Coroutines + Flow
                                                     throughout

  Min SDK             API 26 (Android 8.0)           TYPE_APPLICATION_OVERLAY

  UI                  Jetpack Compose 1.7+           Rendered in System Overlay
                                                     via ComposeView

  DI                  Hilt 2.52+                     Scoped to Services and
                                                     Activities

  Networking          OkHttp 4.x + Retrofit 2.x      Multipart upload for audio

  JSON                Kotlinx Serialization          Groq response parsing

  DB                  Room 2.7+                      Transcription history, WAL
                                                     mode

  Secure Storage      EncryptedSharedPreferences +   API key storage
                      Keystore                       

  Audio               Android AudioRecord API        16 kHz, 16-bit PCM, mono

  Build               Gradle 8.x + KSP               KSP for Room and Hilt

  Testing             JUnit5, MockK, Turbine,        Unit + integration
                      Robolectric                    
  ------------------- ------------------------------ ---------------------------

**3. Core Modules & Classes**

**3.1 FloatingOverlayService**

-   Extends Service, binds to SYSTEM_ALERT_WINDOW

-   Creates a WindowManager.LayoutParams with TYPE_APPLICATION_OVERLAY

-   Hosts an AndroidComposeView with the FloatingButton composable

-   Exposes startRecording() / stopRecording() intents

-   Handles drag-to-reposition via MotionEvent interception

-   Communicates state changes to UI via StateFlow\<OverlayUiState\>

**3.2 AudioCaptureService**

Responsibilities: manage AudioRecord lifecycle, implement Voice Activity
Detection (VAD), encode raw PCM to WAV, and emit audio chunks via Kotlin
Flow.

Key implementation details:

-   AudioRecord initialized: sampleRate=16000,
    channelConfig=CHANNEL_IN_MONO, audioFormat=ENCODING_PCM_16BIT

-   Buffer size = AudioRecord.getMinBufferSize() × 4 for safety

-   VAD: energy-based threshold (RMS) with 1.5 s trailing silence to
    auto-stop

-   WAV encoding: writes 44-byte header + PCM data into
    ByteArrayOutputStream in-memory

-   Emits Flow\<AudioState\> (Recording, SilenceDetected, Stopped,
    Error)

-   Maximum recording duration: 120 s (Whisper API limit)

**3.3 GroqTranscriptionRepository**

  ----------------------- -----------------------------------------------------
  **Endpoint**            POST
                          https://api.groq.com/openai/v1/audio/transcriptions

  **Model**               whisper-large-v3-turbo

  **Auth**                Bearer {GROQ_API_KEY} (retrieved from Keystore at
                          call time)

  **Content-Type**        multipart/form-data

  **Request Fields**      file (WAV bytes), model, language (optional),
                          response_format=json

  **Response**            { text: String }

  **Retry Policy**        Exponential backoff --- 3 retries, 1s/2s/4s delays,
                          on 429 and 5xx only

  **Timeout**             Connect: 10 s, Read: 30 s, Write: 30 s
  ----------------------- -----------------------------------------------------

**3.4 TextInjectionAccessibilityService**

-   Extends AccessibilityService with EVENT_TYPE_VIEW_FOCUSED and
    WINDOW_STATE_CHANGED

-   Tracks the currently focused AccessibilityNodeInfo with
    TYPE_CLASS_EDIT_TEXT

-   Text injection: calls performAction(ACTION_SET_TEXT) with new
    content bundle

-   Fallback: if ACTION_SET_TEXT not supported, uses clipboard paste via
    ACTION_PASTE

-   Exposes injectText(text: String): Result\<Unit\> to other components
    via a bound service interface

**3.5 Data Layer --- Room Database**

Entities and DAOs:

-   TranscriptionEntity(id, text, languageCode, durationMs, createdAt,
    wordCount)

-   TranscriptionDao: insert, deleteById, deleteAll, getAll
    (Flow\<List\>), searchByText

-   Database migrations handled via auto-migration where possible,
    manual otherwise

**4. Permissions & Manifest**

  ------------------------------- ----------------- ------------------------------------
  **Permission**                  **Type**          **Purpose**

  RECORD_AUDIO                    Dangerous ---     Microphone access for recording
                                  runtime           

  SYSTEM_ALERT_WINDOW             Special App       Floating overlay
                                  Access            (ACTION_MANAGE_OVERLAY_PERMISSION)

  ACCESSIBILITY_SERVICE           Special App       Text injection into focused fields
                                  Access            

  FOREGROUND_SERVICE              Normal            Keep services alive while recording

  FOREGROUND_SERVICE_MICROPHONE   Normal (API 34+)  Foreground service type declaration

  INTERNET                        Normal            Groq API calls

  RECEIVE_BOOT_COMPLETED          Normal            Auto-start overlay after device boot
  ------------------------------- ----------------- ------------------------------------

**5. Audio Pipeline --- Data Flow**

  ----------------------- ------------------------------------------------
  **Step 1**              User taps floating button →
                          FloatingOverlayService starts
                          AudioCaptureService via Intent

  **Step 2**              AudioRecord begins buffering PCM at 16 kHz; RMS
                          VAD runs each 20 ms chunk

  **Step 3**              On silence or manual stop: PCM frames assembled
                          into WAV (in memory)

  **Step 4**              WAV ByteArray passed to
                          GroqTranscriptionRepository.transcribe()

  **Step 5**              Retrofit sends multipart POST to Groq API;
                          awaits response (coroutine suspend)

  **Step 6**              Groq returns JSON { text }; repository emits
                          TranscriptionResult.Success

  **Step 7**              ViewModel receives result; updates
                          StateFlow\<OverlayUiState.ShowResult\>

  **Step 8**              Overlay shows transcribed text with Inject /
                          Copy / Dismiss actions

  **Step 9**              On Inject:
                          TextInjectionAccessibilityService.injectText()
                          is called

  **Step 10**             Room DAO inserts TranscriptionEntity for history
  ----------------------- ------------------------------------------------

**6. Security Architecture**

**6.1 API Key Storage**

-   User enters Groq API key in Settings screen

-   Key encrypted using AES-256-GCM via Android Keystore-backed
    EncryptedSharedPreferences

-   Key retrieved at transcription call time; never held in memory
    between calls

-   Key never logged, never transmitted except as Bearer header to
    api.groq.com

**6.2 Audio Data Handling**

-   PCM/WAV audio exists only in heap memory during the transcription
    flow

-   No audio files written to external storage or cache directory

-   WAV ByteArray eligible for GC immediately after HTTP request is
    dispatched

**7. Error Handling Strategy**

  ------------------- ---------------------------- ---------------------------
  **Error Scenario**  **Detection**                **User-Facing Handling**

  No internet         UnknownHostException         Toast: "No connection ---
                                                   recording saved"

  Groq 429 rate limit HTTP 429                     Retry with backoff; overlay
                                                   shows spinner

  Invalid API key     HTTP 401                     Navigate to Settings with
                                                   error message

  Audio record        AudioRecord.ERROR            Toast + log; recording
  failure                                          stopped

  Overlay permission  Settings.canDrawOverlays() = Deep-link to Settings \>
  denied              false                        Special app access

  Accessibility not   getSystemService check       Deep-link to Accessibility
  enabled                                          settings
  ------------------- ---------------------------- ---------------------------

**8. Testing Strategy**

**8.1 Unit Tests**

-   AudioCaptureService VAD logic --- mocked AudioRecord with synthetic
    PCM buffers

-   GroqTranscriptionRepository --- MockWebServer (OkHttp) for HTTP
    contract tests

-   TranscriptionViewModel --- Turbine for Flow assertions

-   Room DAO --- in-memory Room database

**8.2 Integration Tests**

-   End-to-end: record 5 s audio → transcribe → verify text appears in
    overlay (Robolectric)

-   Accessibility injection test on a test EditText

**8.3 Manual / QA**

-   Overlay persistence across app switches (Chrome, WhatsApp, Gmail)

-   Dictation in 5 languages (EN, ES, FR, DE, HI)

-   Performance profiling: CPU, memory, battery (Android Profiler)
