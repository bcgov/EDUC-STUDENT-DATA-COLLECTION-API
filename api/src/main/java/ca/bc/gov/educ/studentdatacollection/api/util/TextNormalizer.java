package ca.bc.gov.educ.studentdatacollection.api.util;


import java.lang.reflect.Field;
import java.text.Normalizer;
import java.util.*;

/**
 * Utility to recursively normalize all String fields in an object and its children.
 * This ensures Indigenous language combining diacritics (like ̓) are preserved.
 *
 * Usage:
 *   MyReportData data = createReportData();
 *   TextNormalizer.normalizeObject(data);
 *   String json = objectMapper.writeValueAsString(data);
 */
public class TextNormalizer {

    private static final Set<Class<?>> WRAPPER_TYPES = new HashSet<>(Arrays.asList(
            Boolean.class, Character.class, Byte.class, Short.class,
            Integer.class, Long.class, Float.class, Double.class, Void.class
    ));

    /**
     * Normalize all String fields in an object and its children recursively.
     * Handles: POJOs, Collections, Maps, Arrays
     *
     * @param obj The object to normalize (modified in-place)
     * @return The same object (for chaining)
     */
    public static <T> T normalizeObject(T obj) {
        if (obj == null) {
            return null;
        }

        try {
            normalizeRecursive(obj, new HashSet<>());
        } catch (Exception e) {
            throw new RuntimeException("Failed to normalize object", e);
        }

        return obj;
    }

    /**
     * Internal recursive normalization with cycle detection
     */
    private static void normalizeRecursive(Object obj, Set<Object> visited) throws Exception {
        if (obj == null || visited.contains(obj)) {
            return; // Prevent infinite loops
        }

        Class<?> clazz = obj.getClass();

        // Skip primitives and wrappers
        if (clazz.isPrimitive() || WRAPPER_TYPES.contains(clazz)) {
            return;
        }

        // Skip Java internal classes (UUID, LocalDate, etc.)
        String className = clazz.getName();
        if (className.startsWith("java.") || className.startsWith("javax.") ||
                className.startsWith("jdk.") || className.startsWith("sun.")) {
            return;
        }

        // Handle String
        if (obj instanceof String) {
            // Can't modify String in-place, caller must handle
            return;
        }

        visited.add(obj);

        // Handle Collections
        if (obj instanceof Collection) {
            normalizeCollection((Collection<?>) obj, visited);
            return;
        }

        // Handle Maps
        if (obj instanceof Map) {
            normalizeMap((Map<?, ?>) obj, visited);
            return;
        }

        // Handle Arrays
        if (clazz.isArray()) {
            normalizeArray(obj, visited);
            return;
        }

        // Handle POJOs - normalize all fields
        normalizeFields(obj, visited);
    }

    /**
     * Normalize all fields in a POJO
     */
    private static void normalizeFields(Object obj, Set<Object> visited) throws Exception {
        Class<?> clazz = obj.getClass();

        // Walk up the inheritance hierarchy
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);

                Object value = field.get(obj);
                if (value == null) {
                    continue;
                }

                // If it's a String, normalize and set back
                if (value instanceof String) {
                    String normalized = normalize((String) value);
                    field.set(obj, normalized);
                }
                // Otherwise recurse into the object
                else {
                    normalizeRecursive(value, visited);
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Normalize strings in a Collection
     */
    private static void normalizeCollection(Collection<?> collection, Set<Object> visited) throws Exception {
        // For Lists, we can replace elements
        if (collection instanceof List) {
            List<Object> list = (List<Object>) collection;
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof String) {
                    list.set(i, normalize((String) item));
                } else if (item != null) {
                    normalizeRecursive(item, visited);
                }
            }
        } else {
            // For other collections, just recurse (can't modify elements)
            for (Object item : collection) {
                if (item != null && !(item instanceof String)) {
                    normalizeRecursive(item, visited);
                }
            }
        }
    }

    /**
     * Normalize strings in a Map
     */
    private static void normalizeMap(Map<?, ?> map, Set<Object> visited) throws Exception {
        Map<Object, Object> mutableMap = (Map<Object, Object>) map;

        // Normalize both keys and values
        List<Object> keysToUpdate = new ArrayList<>();
        Map<Object, Object> updates = new HashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            // Normalize key if it's a string
            Object normalizedKey = key;
            if (key instanceof String) {
                normalizedKey = normalize((String) key);
                if (!normalizedKey.equals(key)) {
                    keysToUpdate.add(key);
                    updates.put(normalizedKey, value);
                }
            }

            // Normalize value
            if (value instanceof String) {
                mutableMap.put(key, normalize((String) value));
            } else if (value != null) {
                normalizeRecursive(value, visited);
            }
        }

        // Update keys that changed (remove old, add new)
        for (Object oldKey : keysToUpdate) {
            mutableMap.remove(oldKey);
        }
        mutableMap.putAll(updates);
    }

    /**
     * Normalize strings in an Array
     */
    private static void normalizeArray(Object array, Set<Object> visited) throws Exception {
        int length = java.lang.reflect.Array.getLength(array);

        for (int i = 0; i < length; i++) {
            Object item = java.lang.reflect.Array.get(array, i);

            if (item instanceof String) {
                String normalized = normalize((String) item);
                java.lang.reflect.Array.set(array, i, normalized);
            } else if (item != null) {
                normalizeRecursive(item, visited);
            }
        }
    }

    /**
     * Normalize a single string to NFC form
     * NFC = Canonical Composition (keeps combining marks attached to base characters)
     */
    private static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }
}