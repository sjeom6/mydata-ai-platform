package com.sjeom.mydata.platform.tool.domain;

public interface AiDataTool<I, O> {

    String name();

    Class<I> inputType();

    Class<O> outputType();

    ToolExecutionResult<O> execute(I input, ToolExecutionContext context);
}
