package com.aelitis.azureus.ui.swt.browser.listener;

import com.aelitis.azureus.util.LoginInfoManager;

public class StatusListener
	extends AbstractStatusListener
{
	public static final String LISTENER_ID = "status";

	public StatusListener() {
		super(LISTENER_ID);
	}

	public void handleLoginUpdate() {
		LoginInfoManager.getInstance().setUserInfo(getUserName(), getDisplayName(),
				getPK(), getAvatar());
	}

	public void handleLoginStatus() {
		LoginInfoManager.getInstance().setUserInfo(getUserName(), getDisplayName(),
				getPK(), getAvatar());
	}
}
