package com.github.zhenbin.statemachine;

import java.util.Set;

public class StateMachineFactory<OPERAND, STATE extends Enum<STATE>, EVENTTYPE extends Enum<EVENTTYPE>, EVENT> {

    private final STATE initialState;

    public StateMachineFactory(STATE initialState) {
        this.initialState = initialState;
    }

    public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> addTransition(STATE preState, STATE postState, EVENTTYPE eventType) {
        return null;
    }

    public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> addTransition(STATE preState, STATE postState, EVENTTYPE eventType, SingleArcTransition<OPERAND, EVENT> hook) {
        return null;
    }

    public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> addTransition(STATE preState, STATE postState, Set<EVENTTYPE> eventTypes) {
        return null;
    }

    public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> addTransition(STATE preState, STATE postState, Set<EVENTTYPE> eventTypes, SingleArcTransition<OPERAND, EVENT> hook) {
        return null;
    }

    public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> addTransition(STATE preState, Set<STATE> postStates, EVENTTYPE eventType, MultipleArcTransition<OPERAND, EVENT, STATE> hook) {
        return null;
    }

    public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> installTopology() {
        return null;
    }

    public StateMachine<STATE, EVENTTYPE, EVENT> make(OPERAND operand) {
        return new InternalStateMachine(operand, initialState);
    }

    private class InternalStateMachine implements StateMachine<STATE, EVENTTYPE, EVENT> {

        private final OPERAND operand;
        private STATE currentState;

        public InternalStateMachine(OPERAND operand, STATE initialState) {
            this.operand = operand;
            currentState = initialState;
        }

        @Override
        public synchronized STATE getCurrentState() {
            return currentState;
        }

        @Override
        public synchronized STATE doTransition(EVENTTYPE eventType, EVENT event) throws InvalidStateTransitionException {
            return null;
        }
    }
}
