package io.serverlessworkflow.impl.expressions;

public class ExpressionValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ExpressionValidationException(String message) {
    super(message);
  }

  public ExpressionValidationException(String message, Throwable ex) {
    super(message, ex);
  }
}
