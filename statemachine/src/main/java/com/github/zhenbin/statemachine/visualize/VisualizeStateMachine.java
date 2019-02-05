package com.github.zhenbin.statemachine.visualize;

import com.github.zhenbin.statemachine.StateMachineFactory;

import java.lang.reflect.Field;

public class VisualizeStateMachine {

    public static Graph getGraphFromClasses(String className) throws Exception {
        Class clz = Class.forName(className);
        Field factoryField = clz.getDeclaredField("stateMachineFactory");
        factoryField.setAccessible(true);
        StateMachineFactory factory = (StateMachineFactory) factoryField.get(null);

        String gname = clz.getSimpleName();
        if (gname.endsWith("Impl")) {
            gname = gname.substring(0, gname.length() - 4);
        }
        return factory.generateStateGraph(gname);
    }

    public static void main(String[] args) throws Exception {
        String className = "com.github.zhenbin.statemachine.demo.ExecutorNodeImpl";
        Graph g = getGraphFromClasses(className);
        /* paste the graph in http://dreampuf.github.io/GraphvizOnline */
        System.out.println(g.generateGraphViz());
    }
}
