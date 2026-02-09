package fi.fmi.avi.converter.iwxxm;

import java.util.*;

/**
 * Common functionality for IWXXM property container classes used in scanning the IWXXM messages
 */
public abstract class AbstractPropertyContainer {

    private final Map<PropertyName, Object> properties = new HashMap<>();

    protected AbstractPropertyContainer() {
    }

    public boolean contains(final PropertyName key) {
        return this.properties.containsKey(key);
    }

    public boolean containsAny(final PropertyName... keys) {
        for (final PropertyName key : keys) {
            if (contains(key)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <S> Optional<S> get(final PropertyName name, final Class<S> clz) {
        final Object o = this.properties.get(name);
        if (o != null) {
            if (name.getAcceptedType().isAssignableFrom(o.getClass())) {
                return (Optional<S>) Optional.of(o);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public <S> List<S> getList(final PropertyName name, final Class<S> itemClz) {
        final Object o = this.properties.get(name);
        if (o != null) {
            if (o instanceof List) {
                final List<S> list = (List<S>) o;
                return list.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(list);
            } else {
                throw new IllegalArgumentException("Value for " + name + " is not a List");
            }
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Sets a value for the named property.
     * Note: silently ignores setting a null value to any property.
     *
     * @param key   the key
     * @param value to set
     * @return the previous value if set
     * @throws IllegalArgumentException if the value type is not acceptable for the given key
     */
    public Object set(final PropertyName key, final Object value) throws IllegalArgumentException {
        if (value == null) {
            return null;
        }
        if (key.getAcceptedType().isAssignableFrom(value.getClass())) {
            return this.properties.put(key, value);
        } else {
            throw new IllegalArgumentException(
                    "Cannot assign value of type " + value.getClass().getCanonicalName() + " to property " + key + ", must be assignable to "
                            + key.getAcceptedType().getCanonicalName());
        }
    }

    /**
     * Removes the value for this key is one has previously been set.
     *
     * @param key the property to unset
     */
    public void unset(final PropertyName key) {
        this.properties.remove(key);
    }

    /**
     * Adds a new value for the list of values for the given key.
     *
     * @param <E>   type of the list item
     * @param key   the key
     * @param value the new list item
     * @throws IllegalArgumentException if the property with this key already has non-list value, or if the value is of wrong type
     */
    @SuppressWarnings("unchecked")
    public <E> void addToList(final PropertyName key, final E value) throws IllegalArgumentException {
        if (key.getAcceptedType().isAssignableFrom(value.getClass())) {
            final Object o = this.properties.computeIfAbsent(key, k -> new ArrayList<>());
            if (o instanceof List) {
                final List<E> list = (List<E>) o;
                list.add(value);
            } else {
                throw new IllegalArgumentException("Property " + key + " has non-list value set, cannot add as list value");
            }
        } else {
            throw new IllegalArgumentException(
                    "Cannot assign value of type " + value.getClass().getCanonicalName() + " to property " + key + ", must be assignable to "
                            + key.getAcceptedType().getCanonicalName());
        }
    }

    /**
     * Adds all given values to the list of values for the given key.
     *
     * @param key    the key
     * @param values the new list items
     * @throws IllegalArgumentException if the property with this key already has non-list value, or if the value is of wrong type
     */
    public void addAllToList(final PropertyName key, final Collection<?> values) throws IllegalArgumentException {
        if (values != null) {
            for (final Object value : values) {
                addToList(key, value);
            }
        }
    }

    public interface PropertyName {
        Class<?> getAcceptedType();
    }
}


