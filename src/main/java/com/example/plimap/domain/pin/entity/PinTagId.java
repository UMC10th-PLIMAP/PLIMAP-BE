package com.example.plimap.domain.pin.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PinTagId implements Serializable {

    private Long pin;
    private Long tag;
}
