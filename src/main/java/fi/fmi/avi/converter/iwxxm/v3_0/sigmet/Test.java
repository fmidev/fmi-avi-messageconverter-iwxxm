package fi.fmi.avi.converter.iwxxm.v3_0.sigmet;

import java.util.Optional;

class Test {
    public static void main(String argv[]) {
    Optional<String> ss = Optional.of("Hello");
    String greeting = ss.orElseGet(() -> "Hi");
    System.err.println(greeting);
    }
}