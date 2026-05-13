package com.flowrite.app.di;

import android.content.Context;
import com.flowrite.settings.ApiKeyManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppModule_ProvideApiKeyManagerFactory implements Factory<ApiKeyManager> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideApiKeyManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ApiKeyManager get() {
    return provideApiKeyManager(contextProvider.get());
  }

  public static AppModule_ProvideApiKeyManagerFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideApiKeyManagerFactory(contextProvider);
  }

  public static ApiKeyManager provideApiKeyManager(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideApiKeyManager(context));
  }
}
