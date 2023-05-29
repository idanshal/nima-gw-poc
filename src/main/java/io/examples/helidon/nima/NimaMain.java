package io.examples.helidon.nima;

import io.helidon.config.Config;

public class NimaMain {
    public static void main(String[] args) {
        Bootstrap.run(Config.create());
    }
}
