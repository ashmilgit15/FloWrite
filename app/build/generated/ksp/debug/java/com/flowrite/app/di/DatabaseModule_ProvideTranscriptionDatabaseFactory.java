package com.flowrite.app.di;

import android.content.Context;
import com.flowrite.history.data.TranscriptionDatabase;
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
public final class DatabaseModule_ProvideTranscriptionDatabaseFactory implements Factory<TranscriptionDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideTranscriptionDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TranscriptionDatabase get() {
    return provideTranscriptionDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideTranscriptionDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideTranscriptionDatabaseFactory(contextProvider);
  }

  public static TranscriptionDatabase provideTranscriptionDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideTranscriptionDatabase(context));
  }
}
