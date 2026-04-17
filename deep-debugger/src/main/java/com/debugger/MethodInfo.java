package com.debugger;

import java.util.List;

/** Represents a public method found in mod source code. */
public record MethodInfo(
    String className,
    String methodName,
    String returnType,
    List<String> paramTypes,
    boolean isStatic,
    boolean isVoid
) {
    public String signature() {
        return className + "." + methodName + "(" + String.join(", ", paramTypes) + ")";
    }

    public String testMethodName() {
        return "test_" + className.replace(".", "_") + "_" + methodName;
    }
}
