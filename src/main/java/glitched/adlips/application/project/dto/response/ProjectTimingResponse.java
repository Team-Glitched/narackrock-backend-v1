package glitched.adlips.application.project.dto.response;

public record ProjectTimingResponse(int bpm, int timeSignatureNumerator, int timeSignatureDenominator,
                                    long maxTick, ProjectVersionResponse version) {}
