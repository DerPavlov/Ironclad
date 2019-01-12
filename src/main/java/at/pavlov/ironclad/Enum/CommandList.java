package at.pavlov.ironclad.Enum;


public enum CommandList {
    BUILD("build", "/ironclad build", "ironclad.player.command", false),
    FIRE("fire", "/ironclad fire", "ironclad.player.command", false),
    ADJUST("adjust", "/ironclad adjust", "ironclad.player.command", false),
    COMMANDS("commands", "/ironclad commands", "ironclad.player.command", false),
    IMITATE("imitate", "/ironclad imitate", null, false),
    RENAME("rename", "/ironclad rename [OLD] [NEW]", "ironclad.player.rename", false),
    OBSERVER("observer", "/ironclad observer", "ironclad.player.observer", false),
    INFO("info", "/ironclad info", "ironclad.player.info", false),
    LIST("list", "/ironclad list", "ironclad.player.list", false),
    BUY("buy", "/ironclad buy", "ironclad.player.buy", false),
    DISMANTLE("dismantle", "/ironclad dismantle", "ironclad.player.dismantle", false),
    TARGET("target", "/ironclad target", "ironclad.player.target", false),
    WHITELIST_ADD("whitelist add", "/ironclad whitelist add [NAME]", "ironclad.player.whitelist", false),
    WHITELIST_REMOVE("whitelist add", "/ironclad whitelist remove [NAME]", "ironclad.player.whitelist", false),
    RESET("reset", "/ironclad reset", "ironclad.player.reset", false),
    CLAIM("claim", "/ironclad claim", "ironclad.player.claim", false),
    //admin commands
    LIST_ADMIN("list", "/ironclad list [NAME]", "ironclad.admin.list", true),
    CREATE("create", "/ironclad create [DESIGN]", "ironclad.admin.create", true),
    DISMANTLE_ADMIN("dismantle", "/ironclad dismantle", "ironclad.admin.dismantle", true),
    RESET_ADMIN("reset", "/ironclad reset [NAME]", "ironclad.admin.reset", true),
    RELOAD("reload", "/ironclad reload", "ironclad.admin.reload", true),
    SAVE("save", "/ironclad save", "ironclad.admin.save", true),
    LOAD("load", "/ironclad load", "ironclad.admin.load", true),
    GIVE("give", "/ironclad give", "ironclad.admin.give", true),
    PERMISSIONS("permissions", "/ironclad permissions [NAME]", "ironclad.admin.permissions", true),
    BLOCKDATA("blockdata", "/ironclad blockdata", "ironclad.admin.blockdata", true);

    private String command;
    private String usage;
    private String permission;
    private Boolean adminCmd;

    CommandList(String command, String usage, String permission, boolean adminCmd){
        this.command = command;
        this.usage = usage;
        this.permission = permission;
        this.adminCmd = adminCmd;
    }

    public String getCommand() {
        return command;
    }

    public String getPermission() {
        return permission;
    }

    public String getUsage() {
        return usage;
    }

    public boolean isAdminCmd(){
        return this.adminCmd;
    }
}
