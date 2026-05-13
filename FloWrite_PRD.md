**FloWrite**

Product Requirements Document

Version 1.0 \| May 2025

*Powered by Groq Whisper-large-v3-turbo*

**1. Product Overview**

**1.1 Vision**

FloWrite is a system-wide speech-to-text Android application that lets
users dictate text into any app --- messages, emails, notes, search bars
--- using a floating overlay. Powered by Groq's ultra-fast
Whisper-large-v3-turbo model, FloWrite delivers near-instant, accurate
transcription, making it the fastest dictation tool on Android.

**1.2 Problem Statement**

Typing on mobile is slow, error-prone, and fatiguing. Existing Android
voice input (Gboard mic, Google Assistant) is tightly coupled to
Google's ecosystem, has latency issues, and provides no user control
over the AI model being used. Power users, professionals, and users with
accessibility needs have no high-quality, privacy-aware, model-agnostic
dictation solution on Android.

**1.3 Target Users**

  ----------------------- -----------------------------------------------
  **Primary**             Power users, knowledge workers, writers who
                          draft long-form content on mobile

  **Secondary**           Accessibility users needing hands-free text
                          input

  **Tertiary**            Developers and early adopters who want
                          customizable STT on Android
  ----------------------- -----------------------------------------------

**1.4 Competitive Landscape**

  ----------------- ----------------- ------------------ -----------------
  **Product**       **Platform**      **Model**          **Weakness**

  Wispr Flow        iOS/macOS         Proprietary        No Android
                                                         support

  Gboard Voice      Android           Google STT         Requires Google
                                                         ecosystem

  Whisper App       Android           Whisper            No system-wide
                                                         overlay

  FloWrite          Android           Whisper-v3-turbo   New entrant
                                      (Groq)             
  ----------------- ----------------- ------------------ -----------------

**2. Core Features**

**F1 --- Floating Overlay Button**

-   Always-on floating microphone button rendered as a system overlay
    (TYPE_APPLICATION_OVERLAY)

-   Draggable, resizable bubble that persists across all apps

-   Tap to start recording; tap again or auto-stop on silence to finish

-   Visual pulse animation while recording

**F2 --- Speech Transcription via Groq Whisper**

-   Audio captured via Android AudioRecord at 16 kHz mono 16-bit PCM

-   Audio encoded as WAV and sent to Groq's
    /openai/v1/audio/transcriptions endpoint

-   Model: whisper-large-v3-turbo

-   Median latency target: \< 800 ms for up to 30-second clips

**F3 --- Smart Text Injection**

-   Transcribed text injected into the focused input field using Android
    Accessibility Service

-   Supports appending to existing text or replacing selected text

-   Auto-capitalisation and punctuation post-processing

**F4 --- Language & Auto-Detect**

-   Supports all Whisper-supported languages

-   Auto-detect mode by default; manual language selection in settings

**F5 --- Transcription History**

-   Local Room database stores last 100 transcriptions

-   Users can copy, re-inject, or delete individual entries

-   Full-text search within history

**F6 --- Settings & Customisation**

-   Groq API key entry (encrypted via Android Keystore)

-   Microphone sensitivity / VAD threshold

-   Overlay position memory and size slider

-   Auto-punctuation toggle

-   Dark / light / system theme

**3. User Stories**

  -------- ----------------- ----------------------------- -----------------
  **ID**   **As a\...**      **I want to\...**             **So that\...**

  US-01    User              Tap a floating mic button in  I can dictate
                             any app                       without leaving
                                                           the current
                                                           screen

  US-02    User              See my transcription appear   I can review
                             immediately after speaking    before it is
                                                           inserted

  US-03    User              Have my text auto-injected    I don't need to
                             into a text field             copy-paste
                                                           manually

  US-04    User              Browse my dictation history   I can retrieve
                                                           things I said
                                                           earlier

  US-05    Power User        Set my own Groq API key       I control my
                                                           usage and cost

  US-06    User              Record in any language and    I can use
                             get the right output          FloWrite in my
                                                           native language

  US-07    Accessibility     Activate recording hands-free I can dictate
           User              via a button                  without precise
                                                           tapping
  -------- ----------------- ----------------------------- -----------------

**4. Non-Functional Requirements**

**4.1 Performance**

  ----------------------- -----------------------------------------------
  **Transcription         \< 800 ms (p50) for clips up to 30 s
  Latency**               

  **App Start Time**      \< 2 s cold start

  **Memory Footprint**    \< 80 MB active RAM

  **Battery Impact**      \< 2% additional drain per hour of background
                          idle
  ----------------------- -----------------------------------------------

**4.2 Security & Privacy**

-   API key stored in Android Keystore, never in plain SharedPreferences

-   Audio data never persisted to disk; sent directly to Groq in memory

-   No analytics or telemetry without explicit consent

-   All network traffic over HTTPS/TLS 1.3

**4.3 Compatibility**

  ----------------------- -----------------------------------------------
  **Minimum Android       Android 8.0 (API 26) ---
  Version**               TYPE_APPLICATION_OVERLAY requires API 26+

  **Target SDK**          Android 15 (API 35)

  **Architecture**        arm64-v8a, x86_64

  **Form Factors**        Phone (primary), Tablet (secondary)
  ----------------------- -----------------------------------------------

**5. Success Metrics**

  ----------------------- -----------------------------------------------
  **North Star**          Weekly Active Dictation Sessions per user \> 20

  **Activation Rate**     \> 60% of installs grant Overlay +
                          Accessibility permissions

  **Transcription         \> 95% WER on English clean speech
  Accuracy**              

  **Latency (p50)**       \< 800 ms end-to-end

  **Retention (Day 7)**   \> 40%

  **Crash Rate**          \< 0.1% sessions
  ----------------------- -----------------------------------------------

**6. Out of Scope (v1.0)**

-   On-device inference (future: local Whisper model)

-   iOS / iPadOS version

-   Real-time streaming transcription (word-by-word)

-   Custom wake word activation

-   Translation (separate feature post v1)

-   Enterprise MDM / SSO integration
