package com.github.zhenbin.statemachine.demo;

import com.github.zhenbin.statemachine.*;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;

@Slf4j
public class ExecutorNodeImpl {
    private String name = "abc";

    private static final StateMachineFactory<ExecutorNodeImpl, NodeState, NodeEventType, NodeEvent> stateMachineFactory
            = new StateMachineFactory<ExecutorNodeImpl, NodeState, NodeEventType, NodeEvent>(NodeState.INIT)
            // doTransition from INIT
            .addTransition(NodeState.INIT, NodeState.CONNECTED, NodeEventType.EXECUTOR_CONNECT, new Connect())
            .addTransition(NodeState.INIT, EnumSet.of(NodeState.INSTALLING, NodeState.LOADING), NodeEventType.AGENT_CONNECT, new OtherConnect())
            // doTransition from CONNECTED
            .addTransition(NodeState.CONNECTED, NodeState.INIT, NodeEventType.EXECUTOR_DISCONNECT, new NodeTransition());

    private StateMachine<NodeState, NodeEventType, NodeEvent> stateMachine = stateMachineFactory.make(this);

    public void handle(NodeEvent nodeEvent) {
        NodeState preState = stateMachine.getCurrentState();
        try {
            stateMachine.doTransition(nodeEvent.getType(), nodeEvent);
            log.info("state: {} -> {}", preState, stateMachine.getCurrentState());
        } catch (InvalidStateTransitionException e) {
            log.error("cannot transact", e);
        }
    }

    private static class NodeTransition implements SingleArcTransition<ExecutorNodeImpl, NodeEvent> {
        @Override
        public void transition(ExecutorNodeImpl node, NodeEvent nodeEvent) {
        }
    }

    private static class Connect extends NodeTransition {
        @Override
        public void transition(ExecutorNodeImpl node, NodeEvent nodeEvent) {
            log.info("connect. {}", node.name);
        }
    }

    private static class OtherConnect implements MultipleArcTransition<ExecutorNodeImpl, NodeEvent, NodeState> {
        @Override
        public NodeState transition(ExecutorNodeImpl node, NodeEvent nodeEvent) {
            return NodeState.LOADING;
        }
    }

    public static void main(String[] args) {
        ExecutorNodeImpl node = new ExecutorNodeImpl();
        node.handle(new NodeEvent(NodeEventType.EXECUTOR_CONNECT));
        node.handle(new NodeEvent(NodeEventType.EXECUTOR_DISCONNECT));
        node.handle(new NodeEvent(NodeEventType.AGENT_CONNECT));
        node.handle(new NodeEvent(NodeEventType.AGENT_CONNECT));
    }
}
