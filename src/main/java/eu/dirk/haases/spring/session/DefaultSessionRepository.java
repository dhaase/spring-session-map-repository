package eu.dirk.haases.spring.session;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.lang.ref.WeakReference;

/**
 * A {@link SessionRepository} backed by a {@link java.util.Map} and that uses a
 * {@link DefaultSession}. The injected {@link java.util.Map} can be backed by a distributed
 * NoSQL store like Hazelcast, for instance. Note that the supplied map itself is
 * responsible for purging the expired sessions.
 * <p>
 */
public final class DefaultSessionRepository extends AbstractSessionRepository implements CurrentSessionRepository<Session> {

    private final ThreadLocal<WeakReference<Session>> threadLocalSession = ThreadLocal.withInitial(() -> createWeakSession());

    private final Runnable purgeRepository;

    private int accessCounter = 0;

    public DefaultSessionRepository() {
        this(new MapSessionRepository());
    }

    public DefaultSessionRepository(final SessionRepository<Session> delegateSessionRepository) {
        this(delegateSessionRepository, null);
    }

    public DefaultSessionRepository(final SessionRepository<Session> delegateSessionRepository, final ApplicationEventPublisher applicationEventPublisher) {
        super(delegateSessionRepository, applicationEventPublisher);
        this.purgeRepository = purgeRepositoryFunction(delegateSessionRepository);
    }

    @Override
    public Session currentSession() {
        final WeakReference<Session> weakRefSession1 = threadLocalSession.get();
        final Session session = weakRefSession1.get();
        if ((session == null) && session.isExpired()) {
            final WeakReference<Session> weakRefSession2 = createWeakSession();
            threadLocalSession.set(weakRefSession2);
            return weakRefSession2.get();
        } else {
            purgeRepository.run();
            return session;
        }
    }

    private WeakReference<Session> createWeakSession() {
        final Session session = createSession();
        save(session);
        return new WeakReference<>(session);
    }

    private Runnable purgeRepositoryFunction(final SessionRepository<Session> delegateSessionRepository) {
        if (delegateSessionRepository instanceof PurgeableSessionRepository) {
            return () -> {
                if ((++accessCounter % 1000) == 0) {
                    ((PurgeableSessionRepository) delegateSessionRepository).purge();
                }
            };
        } else {
            return () -> {
            };
        }
    }
}

