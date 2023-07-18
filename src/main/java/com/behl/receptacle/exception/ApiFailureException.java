package com.behl.receptacle.exception;

public class ApiFailureException extends RuntimeException {

    private static final long serialVersionUID = -1356844246760166578L;

    public ApiFailureException(Throwable cause) {
      super(cause);
    }

}
