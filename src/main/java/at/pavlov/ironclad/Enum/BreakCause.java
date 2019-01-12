package at.pavlov.ironclad.Enum;


public enum BreakCause {

    //Error Messages
    PlayerBreak ("player break"),
    Explosion ("explosion"),
    Overloading ("overloading"),
    Dismantling ("dismantling"),
    Other ("other");


    private final String str;

    BreakCause(String str)
    {
        this.str = str;
    }

    public String getString()
    {
        return str;
    }

}
