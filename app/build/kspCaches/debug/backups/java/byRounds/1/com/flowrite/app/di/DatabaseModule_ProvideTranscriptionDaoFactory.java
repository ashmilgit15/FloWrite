package com.flowrite.app.di;

import com.flowrite.history.data.TranscriptionDao;
import com.flowrite.history.data.TranscriptionDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideTranscriptionDaoFactory implements Factory<TranscriptionDao> {
  private final Provider<TranscriptionDatabase> databaseProvider;

  public DatabaseModule_ProvideTranscriptionDaoFactory(
      Provider<TranscriptionDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public TranscriptionDao get() {
    return provideTranscriptionDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideTranscriptionDaoFactory create(
      Provider<TranscriptionDatabase> databaseProvider) {
    return new DatabaseModule_ProvideTranscriptionDaoFactory(databaseProvider);
  }

  public static TranscriptionDao provideTranscriptionDao(TranscriptionDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideTranscriptionDao(database));
  }
}
