package com.github.zhenbin.statemachine.demo;

import com.github.zhenbin.statemachine.InvalidStateTransitionException;
import com.github.zhenbin.statemachine.SingleArcTransition;
import com.github.zhenbin.statemachine.StateMachine;
import com.github.zhenbin.statemachine.StateMachineFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeImpl {
    private String name = "abc";

    private static StateMachineFactory<NodeImpl, NodeState, NodeEventType, NodeEvent> stateMachineFactory
            = new StateMachineFactory<NodeImpl, NodeState, NodeEventType, NodeEvent>(NodeState.INIT)
            // transition from INIT
            .addTransition(NodeState.INIT, NodeState.CONNECTED, NodeEventType.EXECUTOR_CONNECT, new Connect())
            // transition from CONNECTED
            .addTransition(NodeState.CONNECTED, NodeState.INIT, NodeEventType.EXECUTOR_DISCONNECT, new NodeTransition())
            // install topology
            .installTopology();

    private StateMachine<NodeState, NodeEventType, NodeEvent> stateMachine = stateMachineFactory.make(this);

    public void handle(NodeEvent nodeEvent) {
        log.info("current state: {}", stateMachine.getCurrentState());
        try {
            stateMachine.doTransition(nodeEvent.getType(), nodeEvent);
            log.info("current state: {}", stateMachine.getCurrentState());
        } catch (InvalidStateTransitionException e) {
            log.error("cannot transact", e);
        }
    }

    private static class NodeTransition implements SingleArcTransition<NodeImpl, NodeEvent> {
        @Override
        public void transition(NodeImpl node, NodeEvent nodeEvent) {
        }
    }

    private static class Connect extends NodeTransition {
        @Override
        public void transition(NodeImpl node, NodeEvent nodeEvent) {
            log.info("connect. {}", node.name);
        }
    }
}
