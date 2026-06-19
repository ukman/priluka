package io.github.ukman.priluka;

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
