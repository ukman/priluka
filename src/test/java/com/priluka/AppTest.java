package com.priluka;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {

    @Test
    void greetingReturnsExpectedText() {
        assertEquals("Hello, Priluka!", App.greeting());
    }
}
