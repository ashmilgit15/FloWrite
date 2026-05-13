package com.flowrite.transcription.domain;

import com.flowrite.transcription.data.GroqTranscriptionRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class TranscribeUseCase_Factory implements Factory<TranscribeUseCase> {
  private final Provider<GroqTranscriptionRepository> repositoryProvider;

  public TranscribeUseCase_Factory(Provider<GroqTranscriptionRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public TranscribeUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static TranscribeUseCase_Factory create(
      Provider<GroqTranscriptionRepository> repositoryProvider) {
    return new TranscribeUseCase_Factory(repositoryProvider);
  }

  public static TranscribeUseCase newInstance(GroqTranscriptionRepository repository) {
    return new TranscribeUseCase(repository);
  }
}
