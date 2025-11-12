package hubz.core.operations;

import hubz.model.OperationResult;

//Common interface for all operation
public interface Operation {
    OperationResult execute(String arg);
}
