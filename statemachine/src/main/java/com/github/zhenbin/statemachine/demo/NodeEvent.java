package com.github.zhenbin.statemachine.demo;

public class NodeEvent extends AbstractEvent<NodeEventType> {
    public NodeEvent(NodeEventType nodeEventType) {
        super(nodeEventType);
    }
}
