package at.pavlov.ironclad.utils;

import at.pavlov.ironclad.Enum.BreakCause;
import at.pavlov.ironclad.cannon.Craft;


public class RemoveTaskWrapper {
	private Craft craft;
    private boolean breakCannon;
    private boolean canExplode;
    private BreakCause cause;
    private boolean removeEntry;
    private boolean ignoreInvalid;

	public RemoveTaskWrapper(Craft craft, boolean breakCannon, boolean canExplode, BreakCause cause, boolean removeEntry, boolean ignoreInvalid)
    {
        this.craft = craft;
        this.breakCannon = breakCannon;
        this.canExplode = canExplode;
        this.cause = cause;
        this.removeEntry = removeEntry;
        this.ignoreInvalid = ignoreInvalid;
	}

    public Craft getCraft() {
        return craft;
    }

    public void setCraft(Craft craft) {
        this.craft = craft;
    }

    public boolean breakCannon() {
        return breakCannon;
    }

    public void setBreakCannon(boolean breakCannon) {
        this.breakCannon = breakCannon;
    }

    public boolean canExplode() {
        return canExplode;
    }

    public void setCanExplode(boolean canExplode) {
        this.canExplode = canExplode;
    }

    public BreakCause getCause() {
        return cause;
    }

    public void setCause(BreakCause cause) {
        this.cause = cause;
    }

    public boolean removeEntry() {
        return removeEntry;
    }

    public void setRemoveEntry(boolean removeEntry) {
        this.removeEntry = removeEntry;
    }

    public boolean ignoreInvalid() {
        return ignoreInvalid;
    }

    public void setIgnoreInvalid(boolean ignoreInvalid) {
        this.ignoreInvalid = ignoreInvalid;
    }
}
