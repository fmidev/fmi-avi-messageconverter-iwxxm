package fi.fmi.avi.converter.iwxxm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Common functionality for IWXXM property container classes used in scanning the IWXXM messages
 */
public abstract class AbstractPropertyContainer<T> {

    private T parent;

    private final Map<Object, Object> properties = new HashMap<>();

    protected AbstractPropertyContainer(final T parent) {
        this.parent = parent;
    }

    public boolean contains(final Object key) {
        return this.properties.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public <S> Optional<S> get(final Object name, final Class<S> clz) {
        Object o = this.properties.get(name);
        if (o != null) {
            if (clz.isAssignableFrom(o.getClass())) {
                return (Optional<S>) Optional.of(o);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public <S> List<S> getList(final Object name, final Class<S> itemClz) {
        Object o = this.properties.get(name);
        if (o != null) {
            if (o instanceof List) {
                return (List<S>) o;
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
     * @param key the key
     * @param value to set
     *
     * @return the previous value if set
     *
     * @throws IllegalArgumentException if the value type is not acceptable for the given key
     */
    @SuppressWarnings("unchecked")
    public Object set(final Object key, final Object value) throws IllegalArgumentException {
        if (value == null) {
            return null;
        }
        if (getAcceptedType(key).isAssignableFrom(value.getClass())) {
            return this.properties.put(key, value);
        } else {
            throw new IllegalArgumentException(
                    "Cannot assign value of type " + value.getClass().getCanonicalName() + " to property " + key + ", must be assignable to " + getAcceptedType(
                            key).getCanonicalName());
        }
    }

    /**
     * Removes the value for this key is one has previously been set.
     *
     * @param key
     *         the property to unset
     */
    public void unset(final Object key) {
        this.properties.remove(key);
    }

    /**
     * Adds a new value for the list of values for the given key.
     *
     * @param key
     *         the key
     * @param value
     *         the new list itme
     *
     * @throws IllegalArgumentException
     *         if the property with this key already has non-list value, or if the value is of wrong type
     */
    @SuppressWarnings("unchecked")
    public void addToList(final Object key, final Object value) throws IllegalArgumentException {
        if (getAcceptedType(key).isAssignableFrom(value.getClass())) {
            Object o = this.properties.computeIfAbsent(key, k -> new ArrayList<>());
            if (o instanceof List) {
                List retval = (List) o;
                retval.add(value);
            } else {
                throw new IllegalArgumentException("Property " + key + " has non-list value set, cannot add as list value");
            }
        } else {
            throw new IllegalArgumentException(
                    "Cannot assign value of type " + value.getClass().getCanonicalName() + " to property " + key + ", must be assignable to " + getAcceptedType(
                            key).getCanonicalName());
        }
    }

    public T getParent() {
        return parent;
    }

    protected abstract Class<?> getAcceptedType(final Object key);
}


