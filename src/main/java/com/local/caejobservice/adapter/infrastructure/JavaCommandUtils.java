package com.local.caejobservice.adapter.infrastructure;

import java.nio.file.Path;

public final class JavaCommandUtils {

  private JavaCommandUtils() {}

  public static String javaExecutable() {
    return ProcessHandle.current()
        .info()
        .command()
        .orElseGet(
            () ->
                Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java")
                    .toString());
  }

  public static String currentClasspath() {
    return System.getProperty("java.class.path");
  }

  public static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase().contains("win");
  }
}
