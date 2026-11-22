package glitched.adlips.application.opensource;

import glitched.adlips.application.port.TransactionRunner;

public class OpenSourceSaveUseCase {
    private final OpenSourceSavePort port;
    private final TransactionRunner transactions;

    public OpenSourceSaveUseCase(OpenSourceSavePort port, TransactionRunner transactions) {
        this.port = port;
        this.transactions = transactions;
    }

    public OpenSourceSaveResult save(Long userId, Long openSourceId) {
        validate(userId, openSourceId);
        return transactions.required(() -> {
            if (!port.lockAvailableSource(openSourceId)) {
                throw new OpenSourceException(OpenSourceException.Code.OPEN_SOURCE_NOT_FOUND,
                        "존재하지 않거나 접근할 수 없는 샘플입니다.");
            }
            if (!port.isSaved(userId, openSourceId)) {
                port.insertSave(userId, openSourceId);
            }
            return new OpenSourceSaveResult(openSourceId, true);
        });
    }

    public OpenSourceSaveResult remove(Long userId, Long openSourceId) {
        validate(userId, openSourceId);
        return transactions.required(() -> {
            port.deleteSave(userId, openSourceId);
            return new OpenSourceSaveResult(openSourceId, false);
        });
    }

    private void validate(Long userId, Long openSourceId) {
        if (userId == null || userId <= 0 || openSourceId == null || openSourceId <= 0) {
            throw new OpenSourceException(OpenSourceException.Code.INVALID_REQUEST, "ID는 양의 정수여야 합니다.");
        }
    }
}
