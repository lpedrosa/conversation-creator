package com.github.lpedrosa.util;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class AkkaTest {
    private static final String DEFAULT_TEST_SYSTEM_NAME = "testSystem";
    private static ActorSystem system;

    @BeforeClass
    public static void beforeAll() {
        system = ActorSystem.create(DEFAULT_TEST_SYSTEM_NAME);
    }

    @AfterClass
    public static void afterAll() {
        JavaTestKit.shutdownActorSystem(system);
    }

    protected static ActorSystem system() {
        return system;
    }

    protected static JavaTestKit mockActor() {
        return new JavaTestKit(system());
    }
}
