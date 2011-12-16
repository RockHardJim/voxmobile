/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.exception;

import java.io.IOException;

public class HandlerException extends IOException {
	
	private static final long serialVersionUID = -1286995008319987013L;

	public HandlerException(String message) {
        super(message);
    }

    public HandlerException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    @Override
    public String toString() {
        if (getCause() != null) {
            return getLocalizedMessage() + ": " + getCause();
        } else {
            return getLocalizedMessage();
        }
    }

}
