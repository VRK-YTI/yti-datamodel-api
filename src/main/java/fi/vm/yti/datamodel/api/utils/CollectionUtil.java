package fi.vm.yti.datamodel.api.utils;

import java.util.*;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

public final class CollectionUtil {

    public static <K, V> Map<K, Set<V>> unmodifiable(Map<K, Set<V>> map) {

        for (K key : new ArrayList<>(map.keySet())) {
            map.put(key, unmodifiableSet(map.get(key)));
        }

        return unmodifiableMap(map);
    }

    public static <K, V> Set<V> getOrInitializeSet(Map<K, Set<V>> map, K key) {

        Set<V> set = map.get(key);

        if (set != null) {
            return set;
        } else {
            HashSet<V> newSet = new HashSet<>();
            map.put(key, newSet);
            return newSet;
        }
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean containsAny(Collection<?> source, Collection<?> candidates) {
        if (isEmpty(source) || isEmpty(candidates)) {
            return false;
        }
        for (Object candidate : candidates) {
            if (source.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private CollectionUtil() {
    }
}
