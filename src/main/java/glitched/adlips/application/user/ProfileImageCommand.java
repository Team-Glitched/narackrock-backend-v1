package glitched.adlips.application.user;

public record ProfileImageCommand(String originalFilename, String mimeType, byte[] content) {
    public ProfileImageCommand {
        content = content == null ? null : content.clone();
    }

    @Override
    public byte[] content() {
        return content == null ? null : content.clone();
    }
}
