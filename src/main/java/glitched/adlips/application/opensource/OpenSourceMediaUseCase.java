package glitched.adlips.application.opensource;

import glitched.adlips.application.media.port.out.MediaContentStoragePort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import java.time.Instant;
import static glitched.adlips.application.opensource.OpenSourceException.Code.*;

public class OpenSourceMediaUseCase {
    public record Playback(Long openSourceId, String audioUrl, String mimeType, Long durationMs, Instant expiresAt) {}
    public record Share(Long openSourceId, String shareUrl) {}
    public record Content(long length, java.util.function.Supplier<java.io.InputStream> stream, String mimeType) {}
    private final OpenSourceMediaPort sources;
    private final MediaFileRepositoryPort media;
    private final MediaContentStoragePort storage;
    private final OpenSourceMediaLinkPort links;
    private final TransactionRunner transactions;

    public OpenSourceMediaUseCase(OpenSourceMediaPort sources, MediaFileRepositoryPort media,
            MediaContentStoragePort storage, OpenSourceMediaLinkPort links, TransactionRunner transactions) {
        this.sources = sources; this.media = media; this.storage = storage;
        this.links = links; this.transactions = transactions;
    }
    public Playback playback(Long userId, Long id) {
        validId(userId); validId(id);
        return transactions.readOnly(() -> {
            var file = audio(id);
            try {
                if (!storage.exists(file.getStorageKey())) throw unavailable();
                var link = links.playback(id, file.getId());
                return new Playback(id, link.url(), mime(file), null, link.expiresAt());
            } catch (RuntimeException exception) { throw unavailable(); }
        });
    }
    public Share share(Long userId, Long id) {
        validId(userId); validId(id);
        return transactions.readOnly(() -> {
            source(id);
            return new Share(id, links.share(id));
        });
    }
    public Content content(Long id, Long mediaId, long expires, String signature) {
        validId(id); validId(mediaId);
        if (!links.verify(id, mediaId, expires, signature))
            throw new OpenSourceException(OPEN_SOURCE_ACCESS_DENIED, "재생 링크가 유효하지 않거나 만료되었습니다.");
        return transactions.readOnly(() -> {
            var file = audio(id);
            if (!file.getId().equals(mediaId)) throw new OpenSourceException(OPEN_SOURCE_NOT_FOUND, "음원이 변경되었습니다.");
            try {
                long length = storage.size(file.getStorageKey());
                return new Content(length, () -> storage.open(file.getStorageKey()), mime(file));
            }
            catch (RuntimeException exception) { throw unavailable(); }
        });
    }
    private OpenSourceMediaPort.Source source(Long id) {
        return sources.findAvailable(id).orElseThrow(() ->
                new OpenSourceException(OPEN_SOURCE_NOT_FOUND, "존재하지 않거나 접근할 수 없는 오픈소스입니다."));
    }
    private MediaFile audio(Long id) {
        var source = source(id);
        var file = media.findById(source.mediaFileId()).orElseThrow(this::notReady);
        if (!file.getOwnerId().equals(source.ownerId()) || file.getFileType() != MediaFileType.AUDIO
                || file.getStatus() != MediaFileStatus.READY) throw notReady();
        return file;
    }
    private String mime(MediaFile file) {
        return file.getMimeType() == null || !file.getMimeType().matches("audio/[a-zA-Z0-9.+-]+")
                ? "application/octet-stream" : file.getMimeType();
    }
    private OpenSourceException notReady() { return new OpenSourceException(OPEN_SOURCE_AUDIO_NOT_READY, "음원이 준비되지 않았습니다."); }
    private OpenSourceException unavailable() { return new OpenSourceException(OPEN_SOURCE_PLAYBACK_UNAVAILABLE, "음원을 제공할 수 없습니다."); }
    private void validId(Long id) {
        if (id == null || id <= 0) throw new OpenSourceException(INVALID_REQUEST, "ID가 올바르지 않습니다.");
    }
}
