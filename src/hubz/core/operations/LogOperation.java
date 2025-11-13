//package hubz.core.operations;
//
//import hubz.core.exception.RepositoryNotFoundException;
//import hubz.model.OperationResult;
//
//public class LogOperation implements Operation{
//    @Override
//    public OperationResult execute(String arg){
//        try{
//
//        }catch (RepositoryNotFoundException e) {
//            return new OperationResult(false, e.getMessage());
//        } catch (Exception e) {
//            return new OperationResult(false, "Unexpected error: " + e.getMessage());
//        }
//    }
//}
