/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.exception;

import java.io.IOException;

public class AuthException extends IOException {
	
	private static final long serialVersionUID = 6903193931574029573L;

	public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
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
