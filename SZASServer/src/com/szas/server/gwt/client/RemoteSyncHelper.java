package com.szas.server.gwt.client;

import java.util.ArrayList;

public interface RemoteSyncHelper {
	public void append(String className, RemoteDAO<?> localService);
	Void sync(ArrayList<ToSyncElementsHolder> toSyncElementsHolders);
}
