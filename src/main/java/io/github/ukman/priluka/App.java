package io.github.ukman.priluka;

/**
 * Minimal command-line/demo entry point for the library artifact.
 */
public final class App {
    private App() {
    }

    public static String greeting() {
        return "Hello, Priluka!";
    }

    public static void main(String[] args) {
        System.out.println(greeting());
    }
}
