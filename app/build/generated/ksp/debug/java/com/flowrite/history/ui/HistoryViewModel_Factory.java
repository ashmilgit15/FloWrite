package com.flowrite.history.ui;

import com.flowrite.history.data.TranscriptionDao;
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
public final class HistoryViewModel_Factory implements Factory<HistoryViewModel> {
  private final Provider<TranscriptionDao> transcriptionDaoProvider;

  public HistoryViewModel_Factory(Provider<TranscriptionDao> transcriptionDaoProvider) {
    this.transcriptionDaoProvider = transcriptionDaoProvider;
  }

  @Override
  public HistoryViewModel get() {
    return newInstance(transcriptionDaoProvider.get());
  }

  public static HistoryViewModel_Factory create(
      Provider<TranscriptionDao> transcriptionDaoProvider) {
    return new HistoryViewModel_Factory(transcriptionDaoProvider);
  }

  public static HistoryViewModel newInstance(TranscriptionDao transcriptionDao) {
    return new HistoryViewModel(transcriptionDao);
  }
}
