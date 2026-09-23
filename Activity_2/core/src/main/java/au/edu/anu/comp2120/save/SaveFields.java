package au.edu.anu.comp2120.save;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Strict reader tracks consumed fields so typos and unsupported data cannot be ignored.
 * @author Hao Liu (u7998420)
 */
final class SaveFields {
    /** Detached source fields. */
    private final Map<String, String> fields;
    /** Keys consumed by snapshot decoders. */
    private final Set<String> used = new HashSet<>();
    /** Creates a strict reader.
     * @param fields complete decoded file
     */
    SaveFields(Map<String, String> fields) { this.fields = Map.copyOf(fields); }
    /** Reads one required value.
     * @param key field name
     * @return present value
     * @throws IllegalArgumentException if absent
     */
    String text(String key) {
        used.add(key);
        String value = fields.get(key);
        if (value == null) { throw new IllegalArgumentException("Missing " + key); }
        return value;
    }
    /** Reads a strict boolean.
     * @param key field name
     * @return saved boolean
     * @throws IllegalArgumentException if not exactly true or false
     */
    boolean flag(String key) {
        String value = text(key);
        if (!value.equals("true") && !value.equals("false")) { throw new IllegalArgumentException("Invalid " + key); }
        return Boolean.parseBoolean(value);
    }
    /** Reads a bounded nonnegative integer.
     * @param key field name
     * @param max maximum inclusive value
     * @return bounded value
     * @throws IllegalArgumentException if malformed or out of range
     */
    int count(String key, int max) {
        int value = Integer.parseInt(text(key));
        if (value < 0 || value > max) { throw new IllegalArgumentException("Invalid " + key); }
        return value;
    }
    /** Reads a finite nonnegative number.
     * @param key field name
     * @return saved number
     * @throws IllegalArgumentException if invalid
     */
    double number(String key) {
        double value = Double.parseDouble(text(key));
        if (!Double.isFinite(value) || value < 0) { throw new IllegalArgumentException("Invalid " + key); }
        return value;
    }
    /** Marks fields already validated by a component decoder.
     * @param keys component's expected keys
     */
    void accept(Set<String> keys) { used.addAll(keys); }
    /** Rejects unused keys after all components have been decoded.
     * @throws IllegalArgumentException if any unsupported field exists
     */
    void finish() {
        if (!used.equals(fields.keySet())) { throw new IllegalArgumentException("Unexpected save fields"); }
    }
}
