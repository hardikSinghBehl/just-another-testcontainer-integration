package com.behl.receptacle.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class EmailDispatchRequest {
  
    private final String recipient;
    private final String subject;
    private final String body;
    
}