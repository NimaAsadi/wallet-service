package ir.ebb.external.rayan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RayanErrorResponseDTO(
        String title,
        String description,
        String descriptionEn,
        String errorType,
        Integer errorCode,
        Boolean systemError,
        @JsonProperty("UUID") String uuid,
        Object payloads
) {}
