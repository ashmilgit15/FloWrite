**FloWrite**

Implementation Plan

Version 1.0 \| May 2025

*Powered by Groq Whisper-large-v3-turbo*

**1. Project Overview**

  ----------------------- -----------------------------------------------
  **Project Name**        FloWrite --- Android Speech-to-Text

  **Team Size**           1--2 developers (solo-friendly plan)

  **Total Duration**      8 Weeks

  **Tech Stack**          Kotlin, Jetpack Compose, Groq Whisper API,
                          Hilt, Room, Retrofit

  **Release Target**      Google Play Open Beta at Week 8
  ----------------------- -----------------------------------------------

**2. Milestone Summary**

  ----------- ----------- ----------------------------------- ------------------
  **Phase**   **Weeks**   **Deliverable**                     **Key Goal**

  1           1--2        Project scaffold + overlay proof of Floating button
                          concept                             visible
                                                              system-wide

  2           3--4        Audio capture + Groq integration    Record →
                                                              transcribe → see
                                                              text in overlay

  3           5           Accessibility injection + history   Full dictation
                                                              loop end-to-end

  4           6           Settings, API key, polish           Production-ready
                                                              settings flow

  5           7           Testing, hardening, performance     Crash-free, \< 800
                                                              ms latency
                                                              confirmed

  6           8           Store listing, beta release         APK on Play Open
                                                              Beta
  ----------- ----------- ----------------------------------- ------------------

**3. Detailed Week-by-Week Plan**

**Phase 1 --- Scaffold & Overlay (Weeks 1--2)**

**Week 1: Project Setup**

1.  Create Android project: Kotlin DSL Gradle, min SDK 26, target SDK 35

2.  Configure Hilt, Room, Retrofit, OkHttp, Compose dependencies in
    build.gradle

3.  Set up version catalog (libs.versions.toml)

4.  Implement permission request flow: RECORD_AUDIO,
    SYSTEM_ALERT_WINDOW, Accessibility

5.  Add PermissionActivity with step-by-step onboarding screens
    (Compose)

6.  Write unit test scaffold with JUnit 5 + MockK

**Week 2: Floating Overlay**

7.  Implement FloatingOverlayService with WindowManager +
    TYPE_APPLICATION_OVERLAY

8.  Build FloatingButton composable: idle, recording, processing states
    with animations

9.  Implement drag-to-reposition with MotionEvent handling

10. Persist overlay position via DataStore across restarts

11. Add RECEIVE_BOOT_COMPLETED receiver to auto-start service

12. Manual test: overlay visible in 5+ different apps

**Phase 2 --- Audio & Transcription (Weeks 3--4)**

**Week 3: Audio Capture**

13. Implement AudioCaptureService with AudioRecord at 16 kHz, mono,
    16-bit PCM

14. Build energy-based VAD: 20 ms chunk RMS; auto-stop after 1.5 s
    silence

15. WAV encoder: write 44-byte header + PCM payload into
    ByteArrayOutputStream

16. Emit Flow\<AudioCaptureState\> (Idle, Recording, Processing, Error)

17. Unit tests: VAD with synthetic PCM buffers (silence, speech, noise)

**Week 4: Groq API Integration**

18. Create GroqApiService (Retrofit): multipart POST
    /openai/v1/audio/transcriptions

19. Implement GroqTranscriptionRepository with coroutine suspend
    functions

20. Add exponential backoff retry interceptor (OkHttp) for 429 / 5xx

21. Parse TranscriptionResponse and map to domain model
    TranscriptionResult

22. Integration test with MockWebServer: success, 429, 401, timeout
    scenarios

23. Connect AudioCapture → Groq → display result text in overlay bubble

**Phase 3 --- Injection & History (Week 5)**

**Week 5: End-to-End Dictation Loop**

24. Implement TextInjectionAccessibilityService: track focused EditText,
    inject via ACTION_SET_TEXT, fallback to clipboard

25. Add result card to overlay: shows transcribed text + \[Inject\]
    \[Copy\] \[Dismiss\] buttons

26. Room database: TranscriptionEntity schema, DAO, migrations

27. HistoryScreen in Compose: LazyColumn, search bar, delete swipe
    gesture

28. Wire overlay Inject button → AccessibilityService → focused field

29. End-to-end manual test: speak → see text → inject into WhatsApp,
    Gmail, Chrome URL bar

**Phase 4 --- Settings & Polish (Week 6)**

**Week 6: Settings & UX Polish**

30. Settings screen: API key input (masked), language selector, VAD
    sensitivity slider, theme picker

31. Encrypt API key with EncryptedSharedPreferences backed by Android
    Keystore

32. Onboarding flow: 3-step wizard (permissions → API key → done)

33. Add post-processing: auto-capitalise first word, optional trailing
    punctuation

34. Animate overlay state transitions (idle → recording → processing →
    result)

35. Dark / light / dynamic colour theming (Material 3)

**Phase 5 --- Testing & Hardening (Week 7)**

**Week 7: QA & Performance**

36. Measure end-to-end latency: tap-to-transcription-visible (target \<
    800 ms p50)

37. Profile memory with Android Profiler: identify leaks (AudioRecord
    not released, etc.)

38. Foreground service notification: required for API 34+
    FOREGROUND_SERVICE_MICROPHONE

39. Test overlay persistence across: screen rotation, multi-window, PiP,
    lock screen

40. Test text injection in: WhatsApp, Gmail, Chrome, Notes, Slack,
    Twitter/X

41. Language testing: English, Spanish, French, Hindi, Arabic

42. Edge cases: no internet, API key missing, mic busy (another app),
    Accessibility disabled

43. Raise unit test coverage to \> 70% on domain + data layers

**Phase 6 --- Release (Week 8)**

**Week 8: Play Store Release**

44. Set up Play Console: app listing, screenshots, feature graphic

45. Write store description highlighting Groq speed and privacy

46. Generate signed AAB with release keystore

47. Configure ProGuard / R8 rules (keep Groq API models, Room entities)

48. Privacy policy page (required for RECORD_AUDIO apps)

49. Submit to Play Open Beta; share APK with testers

50. Set up Firebase Crashlytics (optional) for beta crash reporting

**4. Risk Register**

  --------------------- ---------------- ------------ -------------------------------
  **Risk**              **Likelihood**   **Impact**   **Mitigation**

  Accessibility Service High             High         Test on 3+ OEMs; document
  blocked by OEM                                      manual workaround; provide
  (Samsung, Xiaomi)                                   clipboard fallback

  Groq API rate limits  Medium           Medium       Implement backoff; show
  in beta                                             user-friendly quota message;
                                                      link to Groq console

  Play Store rejection  Medium           High         Prepare detailed use-case
  for                                                 justification; document feature
  SYSTEM_ALERT_WINDOW                                 as accessibility tool

  Overlay memory leak   Medium           Medium       WeakReference for context;
  on long sessions                                    lifecycle-aware coroutine
                                                      scopes; leak canary in debug

  Android 15 foreground Low              High         Follow
  service restrictions                                FOREGROUND_SERVICE_MICROPHONE
                                                      type; test on API 35 emulator
  --------------------- ---------------- ------------ -------------------------------

**5. Definition of Done**

-   All Phase 1--6 tasks completed and code reviewed

-   Unit test coverage ≥ 70% on domain + data layers

-   End-to-end latency ≤ 800 ms (p50) on mid-range device (Pixel 6a or
    equivalent)

-   Zero known P0 crashes in 24 hours of internal testing

-   App listed on Play Store Open Beta with privacy policy URL

-   All 3 required permissions (RECORD_AUDIO, Overlay, Accessibility)
    have guided onboarding

-   API key stored securely; verified by Android Keystore audit

**6. Future Roadmap (Post v1.0)**

  ----------------------- -----------------------------------------------
  **v1.1**                Streaming transcription (word-by-word via Groq
                          streaming API)

  **v1.2**                On-device Whisper inference option (no API key
                          required)

  **v1.3**                Wake-word activation ("Hey FloWrite")

  **v2.0**                Translation mode, custom vocabulary injection,
                          team shared history
  ----------------------- -----------------------------------------------
