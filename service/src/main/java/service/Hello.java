package service;

import lombok.*;

/**
 * @author yls91
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class Hello {
    private String message;
    private String description;
}
