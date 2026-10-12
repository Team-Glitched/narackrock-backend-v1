package glitched.adlips.adapter.in.web.dto;

/** banDurationDays는 BAN_USER일 때만 사용하며, 없으면 영구 정지다. */
public record ReportResolveRequest(String actionType, String reason, Integer banDurationDays) {
}
