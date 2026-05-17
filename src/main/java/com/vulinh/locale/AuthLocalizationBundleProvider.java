package com.vulinh.locale;

import java.util.List;

/**
 * Registers this service's resource bundles with {@link LocalizationSupport}. Picked up via {@code
 * META-INF/services/com.vulinh.locale.LocalizationBundleProvider}.
 */
public class AuthLocalizationBundleProvider implements LocalizationBundleProvider {

  @Override
  public List<String> bundleNames() {
    return List.of("i18n/messages");
  }
}
