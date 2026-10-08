package com.example.LAGO.config;

import com.example.LAGO.constants.Interval;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToIntervalConverter implements Converter<String, Interval> {

    @Override
    public Interval convert(String source) {
        return Interval.fromCode(source);
    }
}
