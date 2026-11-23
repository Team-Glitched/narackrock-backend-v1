package glitched.adlips.application.opensource;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.common.ContentLimits;
import glitched.adlips.domain.opensource.OpenSourcePublication;
import java.util.List;

public class OpenSourceQueryUseCase {
    public record Page<T>(int page, int size, boolean hasNext, List<T> items) {}
    private final OpenSourceQueryPort query;
    private final TransactionRunner transactions;

    public OpenSourceQueryUseCase(OpenSourceQueryPort query, TransactionRunner transactions) {
        this.query = query;
        this.transactions = transactions;
    }
    public Page<OpenSourceQueryPort.Item> list(Long userId, String keyword, int page, int size) {
        int offset = offset(userId, page, size);
        String normalized = keyword == null ? null : keyword.strip();
        if (ContentLimits.exceeds(normalized, ContentLimits.SEARCH_QUERY_MAX_LENGTH)) throw invalid();
        String search = normalized == null || normalized.isEmpty() ? null : normalized;
        return transactions.readOnly(() -> page(page, size, query.list(userId, search, offset, size + 1)));
    }
    public Page<OpenSourceQueryPort.Item> popular(Long userId, int size) {
        offset(userId, 0, size);
        return transactions.readOnly(() -> new Page<>(0, size, false, query.popular(userId, size)));
    }

    public OpenSourceQueryPort.Item detail(Long userId, Long id) {
        validId(userId);
        validId(id);
        return transactions.readOnly(() -> query.detail(userId, id).orElseThrow(() ->
                new OpenSourceException(OpenSourceException.Code.OPEN_SOURCE_NOT_FOUND,
                        "존재하지 않거나 접근할 수 없는 샘플입니다.")));
    }
    public Page<OpenSourceQueryPort.SavedItem> saved(Long userId, int page, int size) {
        int offset = offset(userId, page, size);
        return transactions.readOnly(() -> page(page, size, query.saved(userId, offset, size + 1)));
    }
    public Page<OpenSourcePublication> publications(Long userId, int page, int size) {
        int offset = offset(userId, page, size);
        return transactions.readOnly(() -> page(page, size, query.publications(userId, offset, size + 1)));
    }
    private <T> Page<T> page(int page, int size, List<T> rows) {
        return new Page<>(page, size, rows.size() > size, rows.stream().limit(size).toList());
    }
    private int offset(Long userId, int page, int size) {
        validId(userId);
        if (page < 0 || size < 1 || size > 100 || (long)page * size > Integer.MAX_VALUE) throw invalid();
        return page * size;
    }
    private void validId(Long id) { if (id == null || id <= 0) throw invalid(); }
    private OpenSourceException invalid() {
        return new OpenSourceException(OpenSourceException.Code.INVALID_REQUEST, "조회 조건이 올바르지 않습니다.");
    }
}
