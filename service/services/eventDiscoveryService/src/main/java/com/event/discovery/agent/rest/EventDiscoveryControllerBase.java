package com.event.discovery.agent.rest;

import com.event.discovery.agent.rest.model.v1.ErrorDTO;
import com.event.discovery.agent.framework.exception.AuthorizationException;
import com.event.discovery.agent.framework.exception.EntityNotFoundException;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import com.event.discovery.agent.framework.exception.ValidationException;
import com.event.discovery.agent.framework.model.DTO;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
import org.slf4j.Marker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class EventDiscoveryControllerBase {

    public int xyz;

    @ExceptionHandler({ExecutionException.class})
    @ResponseBody
    public ResponseEntity<ErrorDTO> handleExecutionException(Exception ex) {
        if (ex.getCause() instanceof ValidationException) {
            return handleValidationException((ValidationException) ex.getCause());
        } else if (ex.getCause() instanceof EntityNotFoundException) {
            return handleEntityNotFoundException((EntityNotFoundException) ex.getCause());
        } else if (ex.getCause() instanceof AuthorizationException) {
            return handleAuthorizationException((AuthorizationException) ex.getCause());
        } else if (ex.getCause() instanceof ValidationException) {
            return handleValidationException((ValidationException) ex.getCause());
        } else if (ex.getCause() instanceof EventDiscoveryAgentException) {
            return handleSupportException((EventDiscoveryAgentException) ex.getCause());
        }
        return handleThrowable(ex.getCause());
    }

    @ExceptionHandler({EventDiscoveryAgentException.class})
    @ResponseBody
    public ResponseEntity<ErrorDTO> handleSupportException(EventDiscoveryAgentException ex) {
        log.error("SupportException caught by exception handler", ex);
        return new ResponseEntity<>(supportException(ex), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({ValidationException.class})
    @ResponseBody
    public ResponseEntity<ErrorDTO> handleValidationException(ValidationException ex) {
        ErrorDTO err = validationException(ex);
        return new ResponseEntity<>(err, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler({EntityNotFoundException.class})
    @ResponseBody
    public ResponseEntity<ErrorDTO> handleEntityNotFoundException(EntityNotFoundException ex) {
        return new ResponseEntity<>(entityNotFoundException(ex), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({AuthorizationException.class})
    @ResponseBody
    public ResponseEntity<ErrorDTO> handleAuthorizationException(AuthorizationException ex) {
        return new ResponseEntity<>(authorizationException(ex), HttpStatus.FORBIDDEN);
    }


    @ExceptionHandler({Throwable.class})
    @ResponseBody
    public ResponseEntity<ErrorDTO> handleThrowable(Throwable ex) {
        log.error("Generic Throwable caught by exception handler", ex);
        return new ResponseEntity<>(throwable(ex), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorDTO handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, Collection<String>> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            Collection<String> perFieldErrors = errors.getOrDefault(fieldName, new ArrayList<>());
            perFieldErrors.add(errorMessage);
            errors.put(fieldName, perFieldErrors);
        });
        Object targetBean = ex.getBindingResult().getTarget();
        String dtoType = targetBean instanceof DTO ? ((DTO) targetBean).getType() : "unknown";
        return validationException(new ValidationException(dtoType, errors));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return new ResponseEntity<>(methodArgumentTypeMismatchException(ex), HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(javax.validation.ValidationException.class)
    public ResponseEntity<ErrorDTO> handleJavaxValidationException(javax.validation.ValidationException e) {
        if (e.getCause() instanceof EntityNotFoundException) {
            return handleEntityNotFoundException((EntityNotFoundException) e.getCause());
        } else if (e.getCause() instanceof ValidationException) {
            return handleValidationException((ValidationException) e.getCause());
        } else {
            return handleThrowable(e);
        }
    }

    protected ErrorDTO supportException(EventDiscoveryAgentException ex) {
        String msg = ex.getLocalizedMessage() != null && !ex.getLocalizedMessage().isEmpty() ? ex.getLocalizedMessage() : "A server error occurred";
        return ErrorDTO.builder().message(msg).subCode(ex.getSupportCode().getSupportCode()).build();
    }

    protected ErrorDTO throwable(Throwable ex) {
        String errorId = UUID.randomUUID().toString();
        log.error(buildErrorIdMarker(errorId), "An unexpected exception occurred", ex);
        return new ErrorDTO("An unexpected server error occurred.", errorId);
    }

    protected ErrorDTO entityNotFoundException(EntityNotFoundException ex) {
        String errorId = UUID.randomUUID().toString();
        String message = (ex.getMessage() == null) ? "Not found." : ex.getMessage();
        log.warn(buildErrorIdMarker(errorId), "Unable to find {} {}", ex.getEntityType(), ex.getEntityLabel(), ex);
        return new ErrorDTO(message, errorId);
    }

    protected ErrorDTO authorizationException(AuthorizationException ex) {
        String errorId = UUID.randomUUID().toString();
        log.warn(buildErrorIdMarker(errorId), "A forbidden resource was accessed");
        return new ErrorDTO(ex.getUserMessage(), errorId);
    }

    protected ErrorDTO methodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String errorId = UUID.randomUUID().toString();
        Class<?> type = ex.getRequiredType();
        String message;

        // We want to get the list of
        assert type != null;
        if (type.isAssignableFrom(List.class)) {
            ParameterizedType parameterizedType = (ParameterizedType) ex.getParameter().getGenericParameterType();
            type = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            if (type.isEnum()) {
                message = String.format("Invalid parameter: %s. Must be a list consisting of the following types: %s", ex.getName(),
                        Arrays.toString(type.getEnumConstants()));
            } else {
                message = String.format("Invalid parameter: %s. Must be a list consisting of type: %s", ex.getName(), type.getTypeName());
            }
        } else if (type.isEnum()) {
            message = String.format("Invalid parameter: %s. Must be of the following types: %s", ex.getName(),
                    StringUtils.arrayToCommaDelimitedString(type.getEnumConstants()));
        } else {
            message = String.format("Invalid parameter: %s. Must be of type: %s", ex.getName(), type.getTypeName());

        }

        log.warn(buildErrorIdMarker(errorId), message, ex);
        return new ErrorDTO(message, errorId);
    }

    @ExceptionHandler({UnsupportedOperationException.class})
    @ResponseBody
    public ResponseEntity<ErrorDTO> handleUnsupportedOperationException(UnsupportedOperationException ex) {
        return new ResponseEntity<>(unsupportedOperation(ex), HttpStatus.METHOD_NOT_ALLOWED);
    }

    protected int getTimeout(Map<String, String> properties, String timeoutProperty, int defaultTimeout) {
        int result = defaultTimeout;
        String timeout = properties.get(timeoutProperty);
        if (timeout != null) {
            try {
                result = Integer.parseInt(timeout);
            } catch (NumberFormatException ex) {
                log.error("The timeout property " + timeoutProperty + " is not a valid integer. Using default timeout of "
                        + defaultTimeout + " instead", ex);
            }
            if (result < 0) {
                result = defaultTimeout;
                log.error("The timeout property {} is not positive. Using default timeout of {} instead", timeoutProperty, defaultTimeout);
            }
        }
        return result;
    }

    protected ErrorDTO unsupportedOperation(UnsupportedOperationException ex) {
        String errorId = UUID.randomUUID().toString();
        String msg = ex.getMessage() != null && !ex.getMessage().isEmpty() ? ex.getMessage() : "Unsupported method for the entity.";
        log.warn(buildErrorIdMarker(errorId), msg, ex);
        return new ErrorDTO(msg, errorId);

    }

    protected ErrorDTO validationException(ValidationException ex) {
        final StringBuilder sb = new StringBuilder();
        Map<String, Object> meta = new HashMap<>();
        Collection<Map<String, Object>> fields = new ArrayList<>();
        meta.put(ex.getEntityType(), fields);
        sb.append(" Reasons: ");
        ex.getErrors().forEach((field, fieldErrors) -> {
            Map<String, Object> fieldErrorsMap = new HashMap<>();
            // if there is a hierarchy of nested objects in DTO, field might look like: nestedObjLevel1.nestedObjLevel2.fieldA
            if (field.contains(".")) {
                String[] fieldParts = field.split("\\.");
                Map<String, Object> nestedMap = fieldErrorsMap;
                for (int i = 0; i < fieldParts.length - 1; i++) {
                    Map<String, Object> value = new HashMap<>();
                    nestedMap.put(fieldParts[i], value);
                    nestedMap = value;
                }
                nestedMap.put(fieldParts[fieldParts.length - 1], fieldErrors);
            } else {
                fieldErrorsMap.put(field, fieldErrors);
            }
            fields.add(fieldErrorsMap);
            sb.append(' ');
            sb.append(field);
            sb.append(": ");
            sb.append(StringUtils.collectionToCommaDelimitedString(fieldErrors));
        });
        String errorId = UUID.randomUUID().toString();
        ErrorDTO err = new ErrorDTO(ex.getMessage(), errorId);
        err.setMeta(meta);
        log.warn(buildErrorIdMarker(err.getErrorId()), "An error occurred validating a {} entity. {}", ex.getEntityType(), sb.toString(), ex);
        return err;
    }

    protected static Marker buildErrorIdMarker(String errorId) {
        return Markers.append("errorId", errorId);
    }

}
