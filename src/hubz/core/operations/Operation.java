package hubz.core.operations;

import hubz.core.model.OperationResult;

public interface Operation {
    OperationResult execute(String arg);
}
