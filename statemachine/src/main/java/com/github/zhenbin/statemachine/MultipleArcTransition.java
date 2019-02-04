package com.github.zhenbin.statemachine;

public interface MultipleArcTransition<OPERAND, EVENT, STATE> {
    STATE transition(OPERAND operand, EVENT event);
}
