package glitched.adlips.application.media.port.out;

public interface MediaContentStoragePort {

    void put(String storageKey, byte[] content, String contentType);

    boolean exists(String storageKey);

    long size(String storageKey);

    String contentType(String storageKey);

    String publicUrl(String storageKey);
}
