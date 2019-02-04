package com.github.zhenbin.statemachine;

public interface SingleArcTransition<OPERAND, EVENT> {
    void transition(OPERAND operand, EVENT event);
}
