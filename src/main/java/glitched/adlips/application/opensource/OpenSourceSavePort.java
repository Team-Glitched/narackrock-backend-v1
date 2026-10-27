package glitched.adlips.application.opensource;

public interface OpenSourceSavePort {
    boolean lockAvailableSource(Long openSourceId);
    boolean isSaved(Long userId, Long openSourceId);
    void insertSave(Long userId, Long openSourceId);
    void deleteSave(Long userId, Long openSourceId);
}
