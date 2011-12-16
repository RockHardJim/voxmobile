/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.types;

public class DidState {
	
	public String mStateId;
	public int mCount;
	public String mDescription;
	
	public DidState(String stateId, int count, String description) {
		super();
		this.mStateId = stateId;
		this.mCount = count;
		this.mDescription = description;
	}
	
}
