package com.example.coffee.api;

import com.example.coffee.api.response.ApiError;
import com.example.coffee.common.error.ErrorCode;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Spring 5 dispatches unmapped resources through /error rather than NoResourceFoundException. */
@RestController
public class ApiErrorController implements ErrorController {
  @RequestMapping("/error")
  public ResponseEntity<ApiError> handleServletError(HttpServletRequest request) {
    Object attribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    int status = attribute instanceof Integer ? (Integer) attribute : 500;
    ErrorCode errorCode = status == 404 ? ErrorCode.NOT_FOUND : ErrorCode.INTERNAL_ERROR;
    return ResponseEntity.status(status).body(new ApiError(errorCode));
  }
}
