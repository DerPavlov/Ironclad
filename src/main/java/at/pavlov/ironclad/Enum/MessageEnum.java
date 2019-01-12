package at.pavlov.ironclad.Enum;

public enum MessageEnum
{
	//Error Messages
	ErrorCraftBuiltLimit ("Error.CraftBuiltLimit", true),
	ErrorNotTheOwner ("Error.NotTheOwner", true),
	ErrorDismantlingNotOwner ("Error.DismantlingNotOwner", true),
    ErrorMissingSign ("Error.MissingSign", true),
	ErrorNoMoney ("Error.NoMoney", true),
	ErrorNotPaid ("Error.NotPaid", true),
	ErrorAlreadyPaid ("Error.AlreadyPaid", true),
	ErrorPlayerNotFound ("Error.PlayerNotFound", true),

    //Commands
    CmdSelectCraft ("Commands.SelectCraft"),
	CmdSelectBlock ("Commands.SelectBlock"),
    CmdSelectCanceled ("Commands.SelectCanceled"),
    CmdCraftNotFound ("Commands.CraftNotFound"),
	CmdBuyCraft ("Commands.BuyCraft"),
	CmdPaidCraft ("Commands.PaidCraft"),
	CmdClaimCraftStarted("Commands.ClaimCraftsStarted"),
	CmdClaimCraftsFinished("Commands.ClaimCraftsFinished"),

	//Piloting
	PilotingModeEnabled("Piloting.EnablePilotingMode"),
	PilotingModeDisabled("Piloting.DisablePilotingMode"),
	PilotingModeTooFarAway("Piloting.TooFarForPilotingMode"),

	//craft
	CraftCreated ("Craft.Created"),
	CraftDismantled("Craft.Dismantled"),
	CraftDestroyed("Craft.Destroyed"),
    CraftsReseted("Craft.Reseted"),
	CraftInfo ("Craft.Info"),
    CraftRenameSuccess ("Craft.RenameSuccess"),
    CraftRenameFail ("Craft.RenameFail"),

    //imitatedEffects
    ImitatedEffectsEnabled ("ImitatedEffects.Enabled"),
    ImitatedEffectsDisabled ("ImitatedEffects.Disabled"),
	
	//Permission
	PermissionErrorDismantle ("Permission.ErrorDismantle", true),
	PermissionErrorBuild ("Permission.ErrorBuild", true),
	PermissionErrorPilot ("Permission.ErrorPilot", true),
    PermissionErrorRename ("Permission.ErrorRename", true),

	//Help
	HelpText ("Help.Text"),
	HelpBuild ("Help.Build"),
    HelpPilot("Help.Pilot");

	
	private final String str;
	private final boolean isError;
	
	MessageEnum(String str, boolean e)
	{
		this.str = str;
        this.isError = e;
	}
	MessageEnum(String str)
	{
		this.str = str;
        isError = false;
	}

	public String getString()
	{
		return str;
	}
	public boolean isValid()
	{
		return !isError;
	}
	public boolean isError()
	{
		return isError;
	}
}
