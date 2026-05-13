package com.flowrite.settings;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  private final Provider<ApiKeyManager> apiKeyManagerProvider;

  public SettingsViewModel_Factory(Provider<DataStore<Preferences>> dataStoreProvider,
      Provider<ApiKeyManager> apiKeyManagerProvider) {
    this.dataStoreProvider = dataStoreProvider;
    this.apiKeyManagerProvider = apiKeyManagerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(dataStoreProvider.get(), apiKeyManagerProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<DataStore<Preferences>> dataStoreProvider,
      Provider<ApiKeyManager> apiKeyManagerProvider) {
    return new SettingsViewModel_Factory(dataStoreProvider, apiKeyManagerProvider);
  }

  public static SettingsViewModel newInstance(DataStore<Preferences> dataStore,
      ApiKeyManager apiKeyManager) {
    return new SettingsViewModel(dataStore, apiKeyManager);
  }
}
