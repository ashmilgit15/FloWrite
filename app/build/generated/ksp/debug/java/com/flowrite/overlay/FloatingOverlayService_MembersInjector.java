package com.flowrite.overlay;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.flowrite.audio.AudioCaptureManager;
import com.flowrite.history.data.TranscriptionDao;
import com.flowrite.transcription.domain.TranscribeUseCase;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class FloatingOverlayService_MembersInjector implements MembersInjector<FloatingOverlayService> {
  private final Provider<AudioCaptureManager> audioCaptureManagerProvider;

  private final Provider<TranscribeUseCase> transcribeUseCaseProvider;

  private final Provider<TranscriptionDao> transcriptionDaoProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public FloatingOverlayService_MembersInjector(
      Provider<AudioCaptureManager> audioCaptureManagerProvider,
      Provider<TranscribeUseCase> transcribeUseCaseProvider,
      Provider<TranscriptionDao> transcriptionDaoProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.audioCaptureManagerProvider = audioCaptureManagerProvider;
    this.transcribeUseCaseProvider = transcribeUseCaseProvider;
    this.transcriptionDaoProvider = transcriptionDaoProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  public static MembersInjector<FloatingOverlayService> create(
      Provider<AudioCaptureManager> audioCaptureManagerProvider,
      Provider<TranscribeUseCase> transcribeUseCaseProvider,
      Provider<TranscriptionDao> transcriptionDaoProvider,
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new FloatingOverlayService_MembersInjector(audioCaptureManagerProvider, transcribeUseCaseProvider, transcriptionDaoProvider, dataStoreProvider);
  }

  @Override
  public void injectMembers(FloatingOverlayService instance) {
    injectAudioCaptureManager(instance, audioCaptureManagerProvider.get());
    injectTranscribeUseCase(instance, transcribeUseCaseProvider.get());
    injectTranscriptionDao(instance, transcriptionDaoProvider.get());
    injectDataStore(instance, dataStoreProvider.get());
  }

  @InjectedFieldSignature("com.flowrite.overlay.FloatingOverlayService.audioCaptureManager")
  public static void injectAudioCaptureManager(FloatingOverlayService instance,
      AudioCaptureManager audioCaptureManager) {
    instance.audioCaptureManager = audioCaptureManager;
  }

  @InjectedFieldSignature("com.flowrite.overlay.FloatingOverlayService.transcribeUseCase")
  public static void injectTranscribeUseCase(FloatingOverlayService instance,
      TranscribeUseCase transcribeUseCase) {
    instance.transcribeUseCase = transcribeUseCase;
  }

  @InjectedFieldSignature("com.flowrite.overlay.FloatingOverlayService.transcriptionDao")
  public static void injectTranscriptionDao(FloatingOverlayService instance,
      TranscriptionDao transcriptionDao) {
    instance.transcriptionDao = transcriptionDao;
  }

  @InjectedFieldSignature("com.flowrite.overlay.FloatingOverlayService.dataStore")
  public static void injectDataStore(FloatingOverlayService instance,
      DataStore<Preferences> dataStore) {
    instance.dataStore = dataStore;
  }
}
