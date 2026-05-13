package com.flowrite.transcription.data;

import com.flowrite.settings.ApiKeyManager;
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
public final class GroqTranscriptionRepository_Factory implements Factory<GroqTranscriptionRepository> {
  private final Provider<GroqApiService> apiServiceProvider;

  private final Provider<ApiKeyManager> apiKeyManagerProvider;

  public GroqTranscriptionRepository_Factory(Provider<GroqApiService> apiServiceProvider,
      Provider<ApiKeyManager> apiKeyManagerProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.apiKeyManagerProvider = apiKeyManagerProvider;
  }

  @Override
  public GroqTranscriptionRepository get() {
    return newInstance(apiServiceProvider.get(), apiKeyManagerProvider.get());
  }

  public static GroqTranscriptionRepository_Factory create(
      Provider<GroqApiService> apiServiceProvider, Provider<ApiKeyManager> apiKeyManagerProvider) {
    return new GroqTranscriptionRepository_Factory(apiServiceProvider, apiKeyManagerProvider);
  }

  public static GroqTranscriptionRepository newInstance(GroqApiService apiService,
      ApiKeyManager apiKeyManager) {
    return new GroqTranscriptionRepository(apiService, apiKeyManager);
  }
}
