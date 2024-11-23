package demo;

/**
 * @Author: tongqianwen
 * @Date: 2024/11/23
 */
public class ExpiryValue {
    final public String value;
    final public long expiry;

    public ExpiryValue(String value, long expiry) {
        this.value = value;
        this.expiry = expiry;
    }

    public ExpiryValue(String value) {
        this.value = value;
        this.expiry = Long.MAX_VALUE;
    }
}
