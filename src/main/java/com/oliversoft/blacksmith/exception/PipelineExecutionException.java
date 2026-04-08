package com.oliversoft.blacksmith.exception;

public class PipelineExecutionException extends RuntimeException{
    
    public PipelineExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PipelineExecutionException(String message) {
        super(message);
    }
}
