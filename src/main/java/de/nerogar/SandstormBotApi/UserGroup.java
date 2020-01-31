package de.nerogar.sandstormBotApi;

public enum UserGroup {
	OWNER(3),
	ADMIN(2),
	GUEST(1),
	;

	public final int permissionLevel;

	UserGroup(int permissionLevel) {this.permissionLevel = permissionLevel;}
}
