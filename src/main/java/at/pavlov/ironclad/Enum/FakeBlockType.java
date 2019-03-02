package at.pavlov.ironclad.Enum;

/**
 * Created by Peter on 25.04.2014.
 */
public enum FakeBlockType {
    CRUISING("Cruising"),
    EXPLOSION("Explosion"),
    SMOKE_TRAIL("Smoke trail");

    private final String str;

    FakeBlockType(String type) {
        this.str = type;
    }


    public String getStr() {
        return str;
    }
}
