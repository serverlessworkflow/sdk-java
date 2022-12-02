package io.serverlessworkflow.reactive_api_rest;

public class ControllerErrorException extends Exception {
  public ControllerErrorException(String errorMessage) {
    super(errorMessage);
  }
}
