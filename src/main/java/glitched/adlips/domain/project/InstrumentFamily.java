package glitched.adlips.domain.project;

import java.util.Locale;
import java.util.Set;

public enum InstrumentFamily {
        PIANO("피아노", Set.of("piano","electric-piano")),
        GUITAR("기타", Set.of("nylon-guitar","acoustic-guitar")),
        ELECTRIC_GUITAR("일렉기타", Set.of("electric-guitar","overdrive-guitar","distortion-guitar")),
        BASS("베이스", Set.of("acoustic-bass","electric-bass","picked-bass","slap-bass","synth-bass")),
        DRUMS("드럼·타악기", Set.of("drum-kit","percussion")),
        STRINGS("현악기", Set.of("violin","strings")),
        HARP("하프", Set.of("harp")),
        BRASS("트럼펫·브라스", Set.of("trumpet","brass")),
        SAXOPHONE("색소폰", Set.of("saxophone")),
        FLUTE("플루트", Set.of("flute")),
        SYNTH("신디사이저", Set.of("synth-lead","synth-pad")),
        ORGAN("오르간", Set.of("organ")),
        MARIMBA("마림바", Set.of("marimba")),
        VOCAL("보컬", Set.of("vocal","backing-vocal","rap")),
        OTHER("기타 소리", Set.of("other"));
    private final String label;
    private final Set<String> instruments;
    InstrumentFamily(String label,Set<String> instruments) { this.label=label; this.instruments=instruments; }
    public String label() { return label; }
    public static String normalize(String value) {
        if(value==null || value.isBlank()) return null;
        String id=value.trim();
        return switch(id.toUpperCase(Locale.ROOT)) {
            case "PIANO", "피아노" -> "piano";
            case "GUITAR", "기타" -> "acoustic-guitar";
            case "BASS", "베이스" -> "electric-bass";
            case "DRUM", "DRUMS", "드럼" -> "drum-kit";
            default -> id;
        };
    }
    public static InstrumentFamily of(String value) {
        String id=normalize(value);
        if(id==null) return null;
        for(var family:values()) if(family.instruments.contains(id)) return family;
        return null;
    }
    public static boolean audioOnly(String value) {
        String id=normalize(value);
        return id!=null && Set.of("vocal","backing-vocal","rap","percussion","other").contains(id);
    }
}
