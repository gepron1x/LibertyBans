package space.arim.bans.env.bungee;

import java.util.ArrayList;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.exception.MissingCenterException;
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.env.Enforcer;

// TODO Populate this class
public class BungeeEnforcer implements Enforcer {

	private BungeeEnv environment;
	
	public BungeeEnforcer(BungeeEnv environment) {
		this.environment = environment;
	}
	
	private void missingCenter(String message) {
		environment.logger().warning("MissingCenterException! Are you restarting ArimBans?");
		(new MissingCenterException(message)).printStackTrace();
	}
	
	private void cacheFailed(String subject) {
		missingCenter(subject + "'s information was not updated");
	}
	
	private void enforceFailed(String subject, PunishmentType type) {
		missingCenter(subject + " was not checked for " + type.toString());
	}

	@Override
	public void refreshConfig() {
		
	}

	@Override
	public void close() throws Exception {
		
	}

	@Override
	public void enforce(Punishment punishment) {
		
	}
	
	void enforceBans(LoginEvent evt) {
		if (environment.center() == null) {
			enforceFailed(evt.getConnection().getName(), PunishmentType.BAN);
			return;
		}
		if (evt.isCancelled()) {
			return;
		} else if (environment.center().manager().isBanned(environment.center().subjects().parseSubject(evt.getConnection().getUniqueId()))) {
			try {
				evt.setCancelled(true);
				evt.setCancelReason(TextComponent.fromLegacyText(environment.center().formatter().format(environment.center().manager().getPunishment(environment.center().subjects().parseSubject(evt.getConnection().getUniqueId()), PunishmentType.BAN))));
			} catch (MissingPunishmentException ex) {
				environment.center().logError(ex);
			}
		} else {
			ArrayList<String> ips = environment.center().cache().getIps(evt.getConnection().getUniqueId());
			ips.add(evt.getConnection().getAddress().getAddress().getHostAddress());
			for (String addr : ips) {
				if (environment.center().manager().isBanned(environment.center().subjects().parseSubject(addr))) {
					try {
						evt.setCancelled(true);
						evt.setCancelReason(TextComponent.fromLegacyText(environment.center().formatter().format(environment.center().manager().getPunishment(environment.center().subjects().parseSubject(addr), PunishmentType.BAN))));
					} catch (MissingPunishmentException ex) {
						environment.center().logError(ex);
					}
				}
			}
		}
	}
	
	void enforceMutes(ChatEvent evt) {
		ProxiedPlayer player;
		if (evt.getSender() instanceof ProxiedPlayer) {
			player = (ProxiedPlayer) evt.getSender();
		} else {
			return;
		}
		if (environment.center() == null) {
			enforceFailed(player.getName(), PunishmentType.MUTE);
			return;
		}
		if (evt.isCancelled()) {
			return;
		} else if (environment.center().manager().isMuted(environment.center().subjects().parseSubject(player.getUniqueId()))) {
			evt.setCancelled(true);
			try {
				environment.json(player, environment.center().formatter().format(environment.center().manager().getPunishment(environment.center().subjects().parseSubject(player.getUniqueId()), PunishmentType.MUTE)));
				environment.sendMessage(environment.center().subjects().parseSubject(player.getUniqueId()), environment.center().formatter().format(environment.center().manager().getPunishment(environment.center().subjects().parseSubject(player.getUniqueId()), PunishmentType.MUTE)));
			} catch (MissingPunishmentException ex) {
				environment.center().logError(ex);
			}
		} else {
			for (String addr : environment.center().cache().getIps(player.getUniqueId())) {
				if (environment.center().manager().isBanned(environment.center().subjects().parseSubject(addr))) {
					evt.setCancelled(true);
					try {
						environment.sendMessage(environment.center().subjects().parseSubject(addr), environment.center().formatter().format(environment.center().manager().getPunishment(environment.center().subjects().parseSubject(addr), PunishmentType.MUTE)));
					} catch (MissingPunishmentException ex) {
						environment.center().logError(ex);
					}
				}
			}
		}
	}
	
	void updateCache(LoginEvent evt) {
		if (environment.center() == null) {
			 cacheFailed(evt.getConnection().getName());
		}
		environment.center().cache().update(evt.getConnection().getUniqueId(), evt.getConnection().getName(), evt.getConnection().getAddress().getAddress().getHostAddress());
	}

	@Override
	public boolean callPunishEvent(Punishment punishment) {
		return false;
	}

	@Override
	public boolean callUnpunishEvent(Punishment punishment, boolean automatic) {
		if (automatic) {
			return true;
		}
		return false;
	}

}
