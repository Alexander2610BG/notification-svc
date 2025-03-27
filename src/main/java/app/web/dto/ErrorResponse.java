package app.web.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponse {

    private int status;
    private String message;
    private LocalDateTime time;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.time = LocalDateTime.now();
    }
}
