package de.uniks.beastopia.teaml.sockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.beastopia.teaml.Main;
import de.uniks.beastopia.teaml.rest.Event;
import de.uniks.beastopia.teaml.service.TokenStorage;
import de.uniks.beastopia.teaml.utils.PatternHandlerProvider;
import io.reactivex.rxjava3.core.Observable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.function.Consumer;

@Singleton
@SuppressWarnings("unused")
public class EventListener {
    private final TokenStorage tokenStorage;
    private final ObjectMapper mapper;
    private ClientEndpoint endpoint;
    @Inject
    PatternHandlerProvider patternHandlerProvider;

    @Inject
    public EventListener(TokenStorage tokenStorage, ObjectMapper objectMapper) {
        this.tokenStorage = tokenStorage;
        this.mapper = objectMapper;
    }

    private void ensureOpen() {
        if (endpoint != null && endpoint.isOpen()) {
            return;
        }

        final URI endpointURI;
        try {
            endpointURI = new URI(Main.WS_URL + "/events?authToken=" + tokenStorage.getAccessToken());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        endpoint = new ClientEndpoint(endpointURI);
        endpoint.open();
    }

    public <T> Observable<Event<T>> listen(String pattern, Class<T> type) {
        return Observable.create(emitter -> {
            ensureOpen();
            send(Map.of("event", "subscribe", "data", pattern));
            final Consumer<String> handler = patternHandlerProvider.createPatternHandler(mapper, pattern, type, emitter);
            endpoint.addMessageHandler(handler);
            emitter.setCancellable(() -> removeEventHandler(pattern, handler));
        });
    }

    private void removeEventHandler(String pattern, Consumer<String> handler) {
        if (endpoint == null) {
            return;
        }

        send(Map.of("event", "unsubscribe", "data", pattern));
        endpoint.removeMessageHandler(handler);
        if (!endpoint.hasMessageHandlers()) {
            close();
        }
    }

    private void send(Map<String, String> message) {
        ensureOpen();
        try {
            endpoint.sendMessage(mapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void close() {
        if (endpoint == null) {
            return;
        }

        endpoint.close();
        endpoint = null;
    }
}
