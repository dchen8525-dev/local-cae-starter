package com.local.caejobservice.adapter.infrastructure;

public final class DummySolverMain {

  private DummySolverMain() {}

  public static void main(String[] args) throws Exception {
    int duration = 10;
    boolean fail = false;

    for (int index = 0; index < args.length; index++) {
      switch (args[index]) {
        case "--duration" -> duration = Integer.parseInt(args[++index]);
        case "--fail" -> fail = parseBoolean(args[++index]);
        default -> throw new IllegalArgumentException("Unknown argument: " + args[index]);
      }
    }

    int total = Math.max(duration, 1);
    for (int index = 1; index <= total; index++) {
      System.out.printf("[%d/%d] running...%n", index, total);
      Thread.sleep(1000);
    }

    if (fail) {
      System.out.println("Dummy solver finished with a simulated failure.");
      System.exit(1);
    }

    System.out.println("Dummy solver finished successfully.");
  }

  private static boolean parseBoolean(String value) {
    return switch (value.trim().toLowerCase()) {
      case "true", "1", "yes" -> true;
      case "false", "0", "no" -> false;
      default -> throw new IllegalArgumentException("Expected true or false.");
    };
  }
}
