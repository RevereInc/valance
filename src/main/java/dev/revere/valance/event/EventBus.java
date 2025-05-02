package dev.revere.valance.event;

import dev.revere.valance.ClientLoader;
import dev.revere.valance.event.annotation.Subscribe;
import dev.revere.valance.util.Logger;
import dev.revere.valance.util.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Remi
 * @project valance
 * @date 4/28/2025
 */
public class EventBus {
    private static final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":EventBus]";

    // Map: Event Class -> List of Listeners (Method + Instance pairs)
    // ConcurrentHashMap for thread-safe reads/writes to the map itself.
    // CopyOnWriteArrayList for thread-safe iteration during posting and safe modification during reg/unreg.
    private final Map<Class<? extends IEvent>, List<ListenerMethod>> registry = new ConcurrentHashMap<>();

    /**
     * Registers all @Subscribe methods on the given listener object.
     *
     * @param listener The object containing event subscriber methods. Must implement Listener.
     */
    public void register(Listener listener) {
        if (listener == null) {
            Logger.warn(LOG_PREFIX, "Attempted to register a null listener.");
            return;
        }

        Method[] methods = ReflectionUtil.getAnnotatedMethods(listener.getClass(), Subscribe.class);
        if (methods.length == 0) {
            Logger.debug(LOG_PREFIX, "No @Subscribe methods found in " + listener.getClass().getName());
            return;
        }

        boolean registeredAny = false;
        for (Method method : methods) {
            if (method.getParameterCount() == 1) {
                Class<?> eventType = method.getParameterTypes()[0];
                if (IEvent.class.isAssignableFrom(eventType)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends IEvent> eventClass = (Class<? extends IEvent>) eventType;

                    List<ListenerMethod> listeners = registry.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>());
                    if (listeners.stream().noneMatch(lm -> lm.instance == listener && lm.method.equals(method))) {
                        listeners.add(new ListenerMethod(listener, method));
                        registeredAny = true;
                    }
                } else {
                    Logger.warn(LOG_PREFIX, "Method " + method + " in " + listener.getClass().getName() + " has @Subscribe but parameter is not an IEvent.");
                }
            } else {
                Logger.warn(LOG_PREFIX, "Method " + method + " in " + listener.getClass().getName() + " has @Subscribe but incorrect parameter count (must be 1).");
            }
        }
        if (registeredAny) {
            Logger.info(LOG_PREFIX, "Registered listener: " + listener.getClass().getName());
        }
    }

    /**
     * Unregisters all @Subscribe methods associated with the given listener object.
     *
     * @param listener The listener object to unregister.
     */
    public void unregister(Listener listener) {
        if (listener == null) {
            Logger.warn(LOG_PREFIX, "Attempted to unregister a null listener.");
            return;
        }
        final boolean[] removedAny = {false};

        registry.values().forEach(list -> {
            boolean removed = list.removeIf(lm -> lm.instance == listener);
            if (removed) removedAny[0] = true;
        });

        // registry.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        if (removedAny[0]) {
            Logger.info(LOG_PREFIX, "Unregistered listener: " + listener.getClass().getName());
        }
    }

    /**
     * Posts an event to all registered listeners for its specific type and supertypes.
     * If the event implements {@link Cancellable} and is cancelled by a listener,
     * subsequent listeners for that specific event type will still be called,
     * but the cancellation state persists. The caller is responsible for checking
     * the final cancellation state after posting.
     *
     * @param event The event object to dispatch. Must not be null.
     * @param <T>   The type of the event.
     * @return The posted event (potentially modified or cancelled by listeners).
     */
    public <T extends IEvent> T post(T event) {
        if (event == null) {
            Logger.warn(LOG_PREFIX, "Attempted to post a null event.");
            return null;
        }

        postEventToListeners(event, event.getClass());

        return event;
    }

    private <T extends IEvent> void postEventToListeners(T event, Class<? extends IEvent> eventType) {
        List<ListenerMethod> listeners = registry.get(eventType);
        if (listeners != null && !listeners.isEmpty()) {
            boolean isCancellable = event instanceof Cancellable;
            for (ListenerMethod listenerMethod : listeners) {
                try {
                    if (isCancellable && ((Cancellable) event).isCancelled()) {
                        break;
                    }
                    listenerMethod.method.invoke(listenerMethod.instance, event);
                } catch (IllegalAccessException e) {
                    Logger.error(LOG_PREFIX, "Access Error invoking listener " + listenerMethod + " for event " + eventType.getSimpleName());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    Logger.error(LOG_PREFIX, "Exception in listener " + listenerMethod.instance.getClass().getName() + "::" + listenerMethod.method.getName() + " for event " + eventType.getSimpleName());
                    e.getTargetException().printStackTrace();
                } catch (Exception e) {
                    Logger.error(LOG_PREFIX, "Unexpected Error invoking listener " + listenerMethod + " for event " + eventType.getSimpleName());
                    e.printStackTrace();
                }
            }
        }
    }

    private static class ListenerMethod {
        final Listener instance;
        final Method method;

        ListenerMethod(Listener instance, Method method) {
            this.instance = instance;
            this.method = method;
        }

        @Override
        public String toString() {
            return instance.getClass().getName() + "::" + method.getName();
        }
    }
}