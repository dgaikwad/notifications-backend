package com.redhat.cloud.notifications.events;

import com.redhat.cloud.notifications.TestLifecycleManager;
import com.redhat.cloud.notifications.models.KafkaMessage;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
public class KafkaMessagesCleanerTest {

    /*
     * The KafkaMessage#created field is automatically set because of the @PrePersist annotation in CreationTimestamped
     * when a KafkaMessage is persisted using a stateful session. @PrePersist does not work with a stateless session. We
     * need to set KafkaMessage#created manually to a past date in the tests below, that's why these tests are run using
     * a stateless session.
     */
    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    void testPostgresStoredProcedure() {
        sessionFactory.withStatelessSession(statelessSession -> deleteAllKafkaMessages()
                .chain(() -> createKafkaMessage(now().minus(Duration.ofHours(13L))))
                .chain(() -> createKafkaMessage(now().minus(Duration.ofDays(2L))))
                .chain(() -> count())
                .invoke(count -> assertEquals(2L, count))
                .chain(() -> statelessSession.createNativeQuery("CALL cleanKafkaMessagesIds()").executeUpdate())
                .chain(() -> count())
                .invoke(count -> assertEquals(1L, count))
        ).await().indefinitely();
    }

    private Uni<Integer> deleteAllKafkaMessages() {
        return sessionFactory.withStatelessSession(statelessSession ->
                statelessSession.createQuery("DELETE FROM KafkaMessage")
                        .executeUpdate()
        );
    }

    private Uni<Void> createKafkaMessage(LocalDateTime created) {
        KafkaMessage kafkaMessage = new KafkaMessage(UUID.randomUUID());
        kafkaMessage.setCreated(created);
        return sessionFactory.withStatelessSession(statelessSession -> statelessSession.insert(kafkaMessage));
    }

    private Uni<Long> count() {
        return sessionFactory.withStatelessSession(statelessSession ->
                statelessSession.createQuery("SELECT COUNT(*) FROM KafkaMessage", Long.class)
                        .getSingleResult()
        );
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
