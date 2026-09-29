package glitched.adlips.adapter.in.web.dto;

/** 게시글 목록 카드에 쓰는 본문 요약. 앞 100자(코드포인트 기준)를 넘으면 말줄임표를 붙인다. */
final class ContentPreview {

    private static final int LENGTH = 100;

    private ContentPreview() {
    }

    static String of(String content) {
        if (content == null) {
            return "";
        }
        if (content.codePointCount(0, content.length()) <= LENGTH) {
            return content;
        }
        return content.substring(0, content.offsetByCodePoints(0, LENGTH)) + "…";
    }
}
