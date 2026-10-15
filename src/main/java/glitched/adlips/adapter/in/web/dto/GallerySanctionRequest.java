package glitched.adlips.adapter.in.web.dto;

/** type은 SUSPEND 또는 EXPEL. durationDays는 SUSPEND일 때만 사용한다. */
public record GallerySanctionRequest(Long userId, String type, Integer durationDays, String reason) {
}
