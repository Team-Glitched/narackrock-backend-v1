package glitched.adlips.domain.project;

import lombok.Getter;

@Getter
public class ContributionVersionConflictException extends RuntimeException {
    private final int baseMajorVersion;
    private final int baseMinorVersion;
    private final int currentMajorVersion;
    private final int currentMinorVersion;

    public ContributionVersionConflictException(
            int baseMajorVersion, int baseMinorVersion,
            int currentMajorVersion, int currentMinorVersion) {
        super("contribution base project version does not match the current project version");
        this.baseMajorVersion = baseMajorVersion;
        this.baseMinorVersion = baseMinorVersion;
        this.currentMajorVersion = currentMajorVersion;
        this.currentMinorVersion = currentMinorVersion;
    }
}
