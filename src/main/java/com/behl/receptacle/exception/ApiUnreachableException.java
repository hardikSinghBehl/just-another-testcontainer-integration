package com.behl.receptacle.exception;

public class ApiUnreachableException extends RuntimeException {

    private static final long serialVersionUID = -1356844246760166578L;

    public ApiUnreachableException(Throwable cause) {
        super(cause);
    }

}
