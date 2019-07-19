package eu.dirk.haases.spring.session;

import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * A {@link SessionRepository} backed by a {@link Map} and that uses a
 * {@link DefaultSession}. The injected {@link Map} can be backed by a distributed
 * NoSQL store like Hazelcast, for instance. Note that the supplied map itself is
 * responsible for purging the expired sessionsMap.
 * <p>
 * <p>
 * The implementation does NOT support firing {@link SessionDeletedEvent} or
 * {@link SessionExpiredEvent}.
 * </p>
 */
public final class MapSessionRepository implements SessionRepository<Session>, PurgeableSessionRepository {

    private final Map<String, Session> sessionsMap;


    public MapSessionRepository() {
        this(() -> new ConcurrentHashMap<>());
    }

    public MapSessionRepository(final Supplier<Map<String, Session>> sessionsMapSupplier) {
        if (sessionsMapSupplier == null) {
            throw new IllegalArgumentException("Map of sessions cannot be null");
        }
        this.sessionsMap = sessionsMapSupplier.get();
    }

    @Override
    public Session createSession() {
        return new DefaultSession();
    }

    @Override
    public void deleteById(String id) {
        this.sessionsMap.remove(id);
    }

    @Override
    public Session findById(final String id) {
        return this.sessionsMap.get(id);
    }

    @Override
    public void save(final Session session) {
        this.sessionsMap.put(session.getId(), new DefaultSession(session));
    }

    @Override
    public void purge() {
        final Map<String, Session> expiredMap = new HashMap<>();
        final BiConsumer<String, Session> action = (k, v) -> {
            if (v.isExpired()) {
                expiredMap.put(k, v);
            }
        };
        sessionsMap.forEach(action);
        for (String key : expiredMap.keySet()) {
            sessionsMap.remove(key);
        }
    }
}