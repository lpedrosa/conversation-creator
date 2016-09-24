package com.github.lpedrosa.util;

import akka.actor.*;
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

    protected static MonitoredRef newDeadMonitor(Props targetProps) {
        final JavaTestKit watcher = mockActor();
        final Props monitoringSupervisorProps = StoppingSupervisor.props(targetProps, watcher.getRef());
        final ActorRef monitoringSupervisor = system.actorOf(monitoringSupervisorProps);
        watcher.getTestActor();
        return new MonitoredRef(watcher, monitoringSupervisor);
    }

    public static class MonitoredRef {

        private final JavaTestKit watcher;
        private final ActorRef supervisedRef;

        public MonitoredRef(JavaTestKit watcher, ActorRef supervisedRef) {
            this.watcher = watcher;
            this.supervisedRef = supervisedRef;
        }

        public void tell(Object message, ActorRef sender) {
            this.supervisedRef.tell(message, sender);
        }

        public void expectActorToBeDead() {
            try {
                this.watcher.expectMsgEquals(StoppingSupervisor.Api.ChildTerminated);
            } catch (AssertionError e) {
                throw new AssertionError("assertion failed: the monitored actor is still alive");
            }
        }

        public void expectActorToBeAlive() {
            try {
                this.watcher.expectNoMsg();
            } catch (AssertionError e) {
                throw new AssertionError("assertion failed: the monitored actor is dead");
            }
        }
    }

    private static class StoppingSupervisor extends UntypedActor {

        private final Props childProps;
        private final ActorRef interestedRef;

        private ActorRef managedChild;

        public static Props props(Props childProps, ActorRef interestedRef) {
            return Props.create(StoppingSupervisor.class,
                    () -> new StoppingSupervisor(childProps, interestedRef));
        }

        private StoppingSupervisor(Props childProps, ActorRef interestedRef) {
            this.childProps = childProps;
            this.interestedRef = interestedRef;
        }

        @Override
        public void preStart() throws Exception {
            this.managedChild = getContext().actorOf(this.childProps);
            getContext().watch(this.managedChild);
        }

        @Override
        public void onReceive(Object message) throws Throwable {
            if (message instanceof Terminated) {
                handleTerminated((Terminated) message);
            } else {
                this.managedChild.tell(message, getSender());
            }
        }

        private void handleTerminated(Terminated message) {
            if (message.getActor().equals(this.managedChild)) {
                this.interestedRef.tell(Api.ChildTerminated, self());
            }
        }

        @Override
        public SupervisorStrategy supervisorStrategy() {
            return SupervisorStrategy.stoppingStrategy();
        }

        public enum Api {
            ChildTerminated
        }
    }
}
