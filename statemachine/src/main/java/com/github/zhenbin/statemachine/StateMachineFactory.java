package com.github.zhenbin.statemachine;

import com.github.zhenbin.statemachine.visualize.Graph;

import java.util.*;

public class StateMachineFactory<OPERAND, STATE extends Enum<STATE>, EVENTTYPE extends Enum<EVENTTYPE>, EVENT> {

    private final STATE initialState;

    private final Map<STATE, Map<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>>> stateMachineTable;

    public StateMachineFactory(STATE initialState) {
        this.initialState = initialState;

        Map<STATE, Map<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>>> prototype = new HashMap<>();
        prototype.put(initialState, null);
        stateMachineTable = new EnumMap<>(prototype);
    }

    public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> addTransition(STATE preState, STATE postState, EVENTTYPE eventType) {
        return addTransition(preState, postState, eventType, null);
    }

    public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> addTransition(STATE preState, STATE postState, EVENTTYPE eventType, SingleArcTransition<OPERAND, EVENT> hook) {
        Transition<OPERAND, STATE, EVENTTYPE, EVENT> transition = new SingleTransition(postState, hook);
        Map<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>> transitionMap = stateMachineTable.computeIfAbsent(preState, k -> new HashMap<>());
        transitionMap.put(eventType, transition);
        return this;
    }

    public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> addTransition(STATE preState, STATE postState, Set<EVENTTYPE> eventTypes) {
        return addTransition(preState, postState, eventTypes, null);
    }

    public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> addTransition(STATE preState, STATE postState, Set<EVENTTYPE> eventTypes, SingleArcTransition<OPERAND, EVENT> hook) {
        for (EVENTTYPE eventType : eventTypes) {
            addTransition(preState, postState, eventType, hook);
        }
        return this;
    }

    public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> addTransition(STATE preState, Set<STATE> postStates, EVENTTYPE eventType, MultipleArcTransition<OPERAND, EVENT, STATE> hook) {
        Transition<OPERAND, STATE, EVENTTYPE, EVENT> transition = new MultipleTransition(postStates, hook);
        Map<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>> transitionMap = stateMachineTable.computeIfAbsent(preState, k -> new HashMap<>());
        transitionMap.put(eventType, transition);
        return this;
    }

    public StateMachine<STATE, EVENTTYPE, EVENT> make(OPERAND operand) {
        return new InternalStateMachine(operand, initialState);
    }

    private interface Transition<OPERAND, STATE extends Enum<STATE>, EVENTTYPE extends Enum<EVENTTYPE>, EVENT> {
        STATE doTransition(OPERAND operand, STATE preState, EVENTTYPE eventType, EVENT event) throws InvalidStateTransitionException;
    }

    private class MultipleTransition implements Transition<OPERAND, STATE, EVENTTYPE, EVENT> {

        private Set<STATE> validPostStates;

        private MultipleArcTransition<OPERAND, EVENT, STATE> hook;

        MultipleTransition(Set<STATE> postStates, MultipleArcTransition<OPERAND, EVENT, STATE> hook) {
            this.validPostStates = postStates;
            this.hook = hook;
        }

        @Override
        public STATE doTransition(OPERAND operand, STATE preState, EVENTTYPE eventType, EVENT event) throws InvalidStateTransitionException {
            STATE postState = hook.transition(operand, event);
            if (!validPostStates.contains(postState)) {
                throw new InvalidStateTransitionException(preState, eventType);
            }
            return postState;
        }
    }

    private class SingleTransition implements Transition<OPERAND, STATE, EVENTTYPE, EVENT> {

        private STATE postState;

        private SingleArcTransition<OPERAND, EVENT> hook;

        SingleTransition(STATE postState, SingleArcTransition<OPERAND, EVENT> hook) {
            this.postState = postState;
            this.hook = hook;
        }

        public STATE doTransition(OPERAND operand, STATE preState, EVENTTYPE eventType, EVENT event) {
            if (hook != null) {
                hook.transition(operand, event);
            }
            return postState;
        }
    }

    private STATE doTransition(OPERAND operand, STATE preState, EVENTTYPE eventType, EVENT event) throws InvalidStateTransitionException {
        Map<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>> transitionMap = stateMachineTable.get(preState);
        if (transitionMap != null) {
            Transition<OPERAND, STATE, EVENTTYPE, EVENT> transition = transitionMap.get(eventType);
            if (transition != null) {
                return transition.doTransition(operand, preState, eventType, event);
            }
        }
        throw new InvalidStateTransitionException(preState, eventType);
    }

    private class InternalStateMachine implements StateMachine<STATE, EVENTTYPE, EVENT> {

        private final OPERAND operand;
        private STATE currentState;

        InternalStateMachine(OPERAND operand, STATE initialState) {
            this.operand = operand;
            currentState = initialState;
        }

        @Override
        public synchronized STATE getCurrentState() {
            return currentState;
        }

        @Override
        public synchronized STATE doTransition(EVENTTYPE eventType, EVENT event) throws InvalidStateTransitionException {
            currentState = StateMachineFactory.this.doTransition(operand, currentState, eventType, event);
            return currentState;
        }
    }

    public Graph generateStateGraph(String name) {
        Graph g = new Graph(name);
        for (STATE startState : stateMachineTable.keySet()) {
            Map<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>> transitions = stateMachineTable.get(startState);
            for (Map.Entry<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>> entry : transitions.entrySet()) {
                Transition<OPERAND, STATE, EVENTTYPE, EVENT> transition = entry.getValue();
                if (transition instanceof StateMachineFactory.SingleTransition) {
                    Graph.Node fromNode = g.getNode(startState.toString());
                    Graph.Node toNode = g.getNode(((SingleTransition) transition).postState.toString());
                    fromNode.addEdge(toNode, entry.getKey().toString());
                } else if (transition instanceof StateMachineFactory.MultipleTransition) {
                    for (Object o : ((MultipleTransition) transition).validPostStates) {
                        Graph.Node fromNode = g.getNode(startState.toString());
                        Graph.Node toNode = g.getNode(o.toString());
                        fromNode.addEdge(toNode, entry.getKey().toString());
                    }
                }
            }
        }
        return g;
    }
}
